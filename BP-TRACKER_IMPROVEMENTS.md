# Pressione → BP-Tracker: Miglioramenti Portabili

> Guida per implementare le feature di Pressione in BP-Tracker (Android nativo).
> Solo funzionalità realizzabili in Kotlin/Java con librerie Android standard.

---

## 1. 📅 Data di Nascita con Calcolo Dinamico dell'Età

**Stato in Pressione**: Invece di chiedere l'età (che diventa obsoleta), si chiede la data di nascita. L'età viene calcolata dinamicamente con `computeAge()`.

**Implementazione Android**:

```kotlin
fun computeAge(birthDate: LocalDate): Int {
    val today = LocalDate.now()
    return Period.between(birthDate, today).years
}
```

- Usare `DatePickerDialog` nell'UI profilo
- Salvare `birth_date` in SQLite/Room/SharedPreferences
- Mostrare età calcolata in tempo reale: "Età: 45 anni"

**File di riferimento**: `ProfilePrompt.vue`, `SettingsView.vue`, `auth.js`

---

## 2. ⏰ Fasce Orarie Configurabili

**Stato in Pressione**: L'utente può configurare gli orari di Mattina/Pomeriggio/Sera/Notte. Le fasce sono usate in statistiche, report e grafici.

**Implementazione Android**:

```kotlin
data class TimeBand(
    val key: String,      // "MORNING", "AFTERNOON", etc.
    val label: String,    // "Mattina", "Pomeriggio"
    val icon: String,     // "☀️", "🌤️"
    val startHour: Int,   // 6
    val endHour: Int      // 12
)

fun getBandForHour(hour: Int, bands: List<TimeBand>): TimeBand {
    for (band in bands) {
        if (band.startHour <= band.endHour) {
            if (hour >= band.startHour && hour < band.endHour) return band
        } else {
            // Wraps midnight (e.g., NIGHT: 22-6)
            if (hour >= band.startHour || hour < band.endHour) return band
        }
    }
    return bands.last()
}
```

- UI: 4 NumberPicker per ogni fascia in Settings
- Default clinici: Mattina 6-12, Pomeriggio 12-17, Sera 17-22, Notte 22-6
- Salvare in SharedPreferences come JSON

**File di riferimento**: `timeBands.js`, `SettingsView.vue`

---

## 3. 📊 Vista Report Raggruppata per Fascia Oraria

**Stato in Pressione**: Toggle "Lista" / "Per fascia" nella sezione Storico. La vista raggruppata mostra le letture per giorno, suddivise in fasce orarie con label colorate.

**Implementazione Android**:

```kotlin
data class DayGroup(
    val date: String,
    val bands: Map<String, List<Reading>>
)

fun groupReadingsByDayAndBand(
    readings: List<Reading>,
    bands: List<TimeBand>
): List<DayGroup> {
    return readings
        .sortedByDescending { it.timestamp }
        .groupBy { it.timestamp.toLocalDate().toString() }
        .map { (date, dayReadings) ->
            DayGroup(date, dayReadings.groupBy { r ->
                getBandForHour(r.timestamp.hour, bands).key
            })
        }
}
```

- UI: TabLayout o ToggleButton per switch Lista/PerFascia
- RecyclerView con sezioni: header data → label fascia → letture nella fascia
- Color coding SYS/DIA (rosso ≥140, arancione ≥130)

**File di riferimento**: `ReportView.vue` (viewMode + groupedReadings), `timeBands.js` (groupReadingsByDayAndBand)

---

## 4. 📈 Derivata con Segno (dP/dt ↑↓)

**Stato in Pressione**: La tabella "Confronto 7/30 giorni" mostra `Variazione max ↑: +12 mmHg/h` e `Variazione max ↓: -8 mmHg/h` con colori (rosso/arancione).

**Implementazione Android**:

```kotlin
data class DerivativeStats(
    val maxRate: Float,           // massimo assoluto
    val maxPositiveRate: Float,   // massimo positivo (salita)
    val maxNegativeRate: Float,   // massimo negativo (discesa)
    val alarmSegments: List<AlarmSegment>
)

fun computeDerivatives(readings: List<Reading>): DerivativeStats {
    val sorted = readings.sortedBy { it.timestamp }
    val smoothed = movingAverage(sorted.map { it.systolic }, 3)
    
    var maxRate = 0f
    var maxPos = 0f
    var maxNeg = 0f
    val alarms = mutableListOf<AlarmSegment>()
    
    for (i in 1 until sorted.size) {
        val dtHours = (sorted[i].timestamp - sorted[i-1].timestamp) / 3600000f
        if (dtHours <= 0) continue
        val ds = (smoothed[i] - smoothed[i-1]) / dtHours
        
        maxRate = maxOf(maxRate, abs(ds))
        if (ds > 0) maxPos = maxOf(maxPos, ds)
        if (ds < 0) maxNeg = minOf(maxNeg, ds)
        if (abs(ds) > 10) alarms.add(AlarmSegment(sorted[i].timestamp, ds, sorted[i]))
    }
    return DerivativeStats(maxRate, maxPos, maxNeg, alarms)
}
```

- Mostrare nella tabella confronto periodi con `+` per salita, `−` per discesa
- Colori: rosso `#E63946` per positivo, arancione `#EF6C00` per negativo

**File di riferimento**: `statistics.js` (computeDerivatives), `ReportView.vue`

---

## 5. 🎯 Grafici con Zone Target ESC/ESH

**Stato in Pressione**: Chart.js con zona verde tratteggiata 90-140 mmHg, linea tratteggiata rossa a 140, hover tooltip con data+ora+categoria.

**Implementazione Android** (con MPAndroidChart):

```kotlin
// Zona target ESC/ESH nel grafico a linee
val targetZone = LimitLine(140f, "Target <140/90")
targetZone.lineWidth = 1f
targetZone.lineColor = Color.parseColor("#9900614C")
targetZone.enableDashedLine(6f, 3f, 0f)
targetZone.labelPosition = LimitLabelPosition.RIGHT_TOP
targetZone.textSize = 9f

// Rettangolo zona verde 90-140
val rect = RectF(chart.viewPortHandler.contentLeft(), 
    chart.getYPosition(90f),
    chart.viewPortHandler.contentRight(),
    chart.getYPosition(140f))
// Draw rect with green fill in onDraw override

// Tooltip interattivo
chart.marker = object : MarkerView(context, R.layout.chart_marker) {
    override fun refreshContent(entry: Entry, highlight: Highlight) {
        // Mostra data, ora, valore, categoria ESC/ESH
        val reading = readings[entry.x.toInt()]
        tvDate.text = reading.timestamp.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
        tvValue.text = "${reading.systolic}/${reading.diastolic} mmHg"
        tvCategory.text = getCategoryLabel(reading)
        super.refreshContent(entry, highlight)
    }
}
```

**File di riferimento**: `StatisticsView.vue` (renderBPChart), `SharedReportView.vue`

---

## 6. 📊 Bar Chart Derivate (dP/dt)

**Stato in Pressione**: Bar chart con barre colorate: rosso `#D90429` se |dP/dt| > 10, rosso chiaro se positivo, blu se negativo.

**Implementazione Android** (con MPAndroidChart):

```kotlin
val barChart = findViewById<BarChart>(R.id.derivChart)
val entries = derivatives.systolic.mapIndexed { i, ds ->
    BarEntry(i.toFloat(), ds)
}
val dataSet = BarDataSet(entries, "dS/dt").apply {
    colors = derivatives.systolic.map { ds ->
        when {
            abs(ds) > 10 -> Color.parseColor("#D90429")  // rosso allarme
            ds > 0 -> Color.parseColor("#80E63946")       // rosso chiaro
            else -> Color.parseColor("#80457B9D")          // blu chiaro
        }
    }
    setDrawValues(false)
}
```

- Altezza ridotta (~150dp) sotto il grafico principale
- Lista allarmi sotto: "⚠️ 03/08 — Variazione +12 mmHg/ora (145/95)"

**File di riferimento**: `StatisticsView.vue` (renderDerivChart)

---

## 7. 📋 Dashboard Medico Interattiva (Shared Report)

**Stato in Pressione**: Link temporaneo 48h con PIN opzionale. Il medico vede: classificazione, KPI grid, grafico BP interattivo, derivata, distribuzione, fasce orarie, tabella letture.

**Implementazione Android**:

- **Generazione link**: Firebase Dynamic Links o URL condivisibile + PIN
- **Scadenza 48h**: Timestamp su server + cleanup automatico
- **PIN gate**: Schermata PIN a 4 cifre
- **Dashboard**: Fragment con:
  - Classificazione ESC/ESH (badge colorato)
  - 4 KPI cards (Media SYS/DIA, BPM, Carico Ipertensivo, Picco Mattutino)
  - Grafico BP con zona target (MPAndroidChart)
  - Bar chart derivate
  - Doughnut distribuzione
  - Card fasce orarie
  - Filtro date (Tutto/30gg/7gg)
  - Tabella letture completa

**File di riferimento**: `SharedReportView.vue`

---

## 8. 🚨 Indicatori di Rischio Clinico

**Stato in Pressione**: Alert box colorate nel report:

- ⚠️ Picco mattutino elevato (>10 mmHg Δ)
- ⚠️ Carico ipertensivo >30%
- ⚠️ Variazioni rapide (>10 mmHg/ora)
- ✅ Nessun indicatore di rischio (verde)

**Implementazione Android**:

```kotlin
data class RiskIndicators(
    val morningSurgeAlert: Boolean,
    val morningSurgeDelta: Float,
    val hypertensiveLoadPercent: Int,
    val hypertensiveLoadAbnormal: Int,
    val rapidChangeCount: Int
)

fun getRiskAlerts(risks: RiskIndicators): List<Alert> {
    val alerts = mutableListOf<Alert>()
    if (risks.morningSurgeAlert) alerts.add(Alert(
        "⚠️ Picco mattutino elevato: Δ ${risks.morningSurgeDelta} mmHg",
        AlertColor.ORANGE
    ))
    if (risks.hypertensiveLoadPercent > 30) alerts.add(Alert(
        "⚠️ Carico ipertensivo >30%",
        AlertColor.RED
    ))
    if (risks.rapidChangeCount > 0) alerts.add(Alert(
        "⚠️ ${risks.rapidChangeCount} episodi di variazione rapida",
        AlertColor.YELLOW
    ))
    if (alerts.isEmpty()) alerts.add(Alert(
        "✅ Nessun indicatore di rischio critico",
        AlertColor.GREEN
    ))
    return alerts
}
```

**File di riferimento**: `ReportView.vue`, `SharedReportView.vue`, `statistics.js`

---

## 9. 📤 Condivisione PDF con Allegato

**Stato in Pressione**: Invece di link testuali, il PDF viene generato come File e condiviso via Android Sharesheet / Web Share API.

**Implementazione Android**:

```kotlin
fun sharePDF(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, 
        "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Report Pressione Arteriosa")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Condividi report"))
}
```

- Genera PDF con Android PDFDocument API o iText
- Allega via FileProvider
- Apre share sheet nativo (Gmail, WhatsApp, etc.)

**File di riferimento**: `pdfReport.js`, `ReportView.vue`

---

## 10. ✅ Prompt Profilo Non Ripetitivo

**Stato in Pressione**: Il prompt profilo appare solo se `profileCompleted = false` e `skipProfilePrompt = false`. Dopo il salvataggio o skip, i flag vengono persistiti nella sessione.

**Implementazione Android**:

```kotlin
// SharedPreferences
val prefs = getSharedPreferences("profile", MODE_PRIVATE)
val profileCompleted = prefs.getBoolean("completed", false)
val skipPrompt = prefs.getBoolean("skip", false)

if (!profileCompleted && !skipPrompt) {
    showProfileDialog()
}

fun onProfileSaved() {
    prefs.edit().putBoolean("completed", true).apply()
}

fun onProfileSkipped() {
    prefs.edit().putBoolean("skip", true).apply()
}
```

**File di riferimento**: `ProfilePrompt.vue`, `auth.js` (refreshSession, updateUserProfile)

---

## 11. 🔢 Validazione Avanzata Input

**Stato in Pressione**: Validazione lato client prima del submit:

- SYS 1-299, DIA 1-199, HR 1-299
- DIA < SYS
- Rilevazione duplicati (stessa misurazione entro 10 min)
- Classificazione ESC/ESH in tempo reale

**Implementazione Android**:

```kotlin
fun validateReading(sys: Int, dia: Int, hr: Int): ValidationResult {
    if (sys !in 1..299) return ValidationResult.Error("Sistolica non valida (1-299 mmHg)")
    if (dia !in 1..199) return ValidationResult.Error("Diastolica non valida (1-199 mmHg)")
    if (hr !in 1..299) return ValidationResult.Error("Frequenza non valida (1-299 BPM)")
    if (dia >= sys) return ValidationResult.Error("Diastolica ≥ Sistolica")
    return ValidationResult.Ok
}
```

- TextWatcher per classificazione live mentre l'utente digita
- Snackbar/TextView per errori in tempo reale

**File di riferimento**: `AddEditReadingView.vue`

---

## Riepilogo Priorità

| # | Feature | Impatto | Complessità |
| --- | --- | --- | --- |
| 1 | Data di nascita dinamica | 🔴 Basso | 🟢 Bassa |
| 2 | Fasce orarie configurabili | 🟡 Medio | 🟡 Media |
| 5 | Grafici con zone target | 🔴 Alto | 🟡 Media |
| 4 | Derivate con segno (+/-) | 🟡 Medio | 🟢 Bassa |
| 7 | Dashboard medico | 🔴 Alto | 🔴 Alta |
| 8 | Indicatori rischio clinico | 🟡 Medio | 🟢 Bassa |
| 9 | Condivisione PDF | 🔴 Alto | 🟡 Media |
| 10 | Prompt profilo non ripetitivo | 🟢 Basso | 🟢 Bassa |
| 3 | Vista raggruppata fasce | 🟡 Medio | 🟡 Media |
| 6 | Bar chart derivate | 🟡 Medio | 🟢 Bassa |
| 11 | Validazione avanzata | 🟡 Medio | 🟢 Bassa |
