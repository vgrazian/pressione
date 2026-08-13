# IperTeso — Piano di Sviluppo Android (Kotlin)

> **Versione target:** 1.0.0 | **Data piano:** 2026-08-12 | **Stato:** ✅ COMPLETATO (2026-08-13)  
> **Target device:** Samsung Galaxy S24 (Android 14, API 34)  
> **Min SDK:** 26 (Android 8.0) | **Compile SDK:** 34  
> **Stack:** Kotlin 2.0.0 · Jetpack Compose (BOM 2024.06.00) · Supabase Kotlin SDK 2.4.0 · Room 2.6.1 (offline cache)  
> **Parità funzionale:** allineata all'app web Pressione v1.2.1 (vedi matrice in fondo)  
> **DB condiviso:** Stesso schema Supabase, stesse utenze, stesso backend

---

## Stato dell'Ambiente (verificato 2026-08-13)

| Componente | Versione | Stato |
| --- | --- | --- |
| Java | OpenJDK 17.0.18 | ✅ |
| Kotlin | 2.0.0 | ✅ |
| AGP | 8.5.2 | ✅ |
| Compose BOM | 2024.06.00 (Material3) | ✅ |
| Gradle | 9.3.0 | ✅ |
| KSP | 2.0.0-1.0.22 | ✅ |
| Room | 2.6.1 | ✅ |
| Supabase Kotlin SDK | 2.4.0 (postgrest-kt, gotrue-kt) | ✅ |
| Ktor client Android | 2.3.12 | ✅ |
| Koin | 3.5.6 | ✅ |
| Navigation Compose | 2.7.7 | ✅ |
| WorkManager | 2.9.0 | ✅ |
| iText7 | 7.2.5 | ✅ |
| Keystore release | `iperteso/android/keystore/release.keystore` (gitignored) | ✅ |
| Test | JUnit4 · MockK 1.13.12 · coroutines-test · koin-test · room-testing | ✅ |
| Icone Android | `iperteso/app/src/main/res/mipmap-*/` | ✅ |

---

## Architettura (realizzata)

```
iperteso/
├── app/
│   ├── src/main/
│   │   ├── java/com/pressione/iperteso/
│   │   │   ├── IperTesoApplication.kt          # Application + Koin + Room + sync
│   │   │   ├── MainActivity.kt                  # Single-activity, deep link, locale
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt           # Room DB (offline cache)
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── ReadingDao.kt
│   │   │   │   │   │   ├── UserDao.kt
│   │   │   │   │   │   ├── SettingsDao.kt
│   │   │   │   │   │   └── MedicationDao.kt
│   │   │   │   │   └── entity/
│   │   │   │   │       ├── ReadingEntity.kt
│   │   │   │   │       ├── UserEntity.kt
│   │   │   │   │       ├── SettingEntity.kt
│   │   │   │   │       └── MedicationEntity.kt
│   │   │   │   ├── remote/
│   │   │   │   │   ├── SupabaseClient.kt         # Singleton Supabase + logging
│   │   │   │   │   └── api/
│   │   │   │   │       ├── AuthApi.kt            # Login table-based + recovery stub
│   │   │   │   │       ├── ReadingsApi.kt        # CRUD letture
│   │   │   │   │       ├── MedicationApi.kt      # CRUD farmaci
│   │   │   │   │       ├── SharedReportApi.kt    # Link condivisi (tabella shared_reports)
│   │   │   │   │       └── ReadingReportJson.kt  # (de)serializzazione report_data JSONB
│   │   │   │   ├── repository/
│   │   │   │   │   ├── AuthRepository.kt
│   │   │   │   │   ├── ReadingRepository.kt      # Offline-first
│   │   │   │   │   └── MedicationRepository.kt
│   │   │   │   ├── SessionManager.kt             # DataStore, TTL 8h
│   │   │   │   └── sync/
│   │   │   │       └── SyncWorker.kt             # WorkManager 15min
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                  # Koin graph
│   │   │   ├── domain/model/
│   │   │   │   ├── Reading.kt
│   │   │   │   ├── User.kt
│   │   │   │   ├── Category.kt                   # ESC/ESH 7 categorie (label + labelEn)
│   │   │   │   ├── AuthSession.kt
│   │   │   │   ├── Medication.kt
│   │   │   │   └── TimeBand.kt
│   │   │   ├── services/
│   │   │   │   ├── PdfReportGenerator.kt         # iText7 (header, stats, farmaci, tabella)
│   │   │   │   ├── CsvExporter.kt
│   │   │   │   ├── ReminderScheduler.kt          # AlarmManager giornaliero
│   │   │   │   ├── ReminderReceiver.kt           # BroadcastReceiver + notifica
│   │   │   │   └── LocaleManager.kt              # i18n it/en con riavvio
│   │   │   ├── ui/
│   │   │   │   ├── navigation/NavGraph.kt        # Routes + deep link share/{token}
│   │   │   │   ├── theme/ (Theme, Color, Type)   # M3 + dark mode di sistema
│   │   │   │   ├── components/
│   │   │   │   │   ├── ReadingCard.kt
│   │   │   │   │   ├── CategoryBadge.kt
│   │   │   │   │   └── SkeletonLoader.kt
│   │   │   │   └── screens/
│   │   │   │       ├── auth/ LoginScreen + AuthViewModel
│   │   │   │       ├── home/ HomeScreen + HomeViewModel
│   │   │   │       ├── readings/ AddEditReadingScreen/VM, ReadingListScreen/VM
│   │   │   │       ├── analysis/ AnalysisScreen/VM, BpTrendChart, ExtraCharts
│   │   │   │       ├── report/ SharedReportScreen/VM (PIN gate)
│   │   │   │       └── settings/ SettingsScreen, MedicationViewModel
│   │   ├── res/
│   │   │   ├── values/strings.xml                # it (default) + values-en/
│   │   │   └── mipmap-*/                         # Launcher icons
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts                              # Root build
├── settings.gradle.kts
└── gradle.properties                             # SUPABASE_URL + publishable key
```

> **Differenze rispetto al piano originale:** `usecase/`, `SettingsApi`, `SettingsRepository`, `SyncRepository`, `ReportScreen`, `AdminOperatorsScreen`, `util/` e `ReminderDao` non sono stati creati come moduli separati — le relative funzioni sono state implementate direttamente nelle schermate/repository esistenti. Le icone sono in `app/src/main/res/mipmap-*/` (non `android/mipmap-*/`).

---

## Librerie Chiave (con parità funzionale)

| Funzione Web | Libreria Android | Note |
| --- | --- | --- |
| Supabase client | `io.github.jan-tennert.supabase:supabase-kt` | Kotlin-first, supporta `postgrest-kt`, `realtime-kt` |
| Offline cache (IndexedDB) | `androidx.room:room-runtime` + `room-ktx` | Room con fallback SQLite |
| Grafici (Chart.js) | `co.yml:ycharts` o `com.patrykandpatrick.vico:compose-m3` | YCharts per line/bar/doughnut; Vico per Compose nativa |
| PDF generation | `com.itextpdf:itext7-core` | Equivalente jsPDF, grafici incorporati |
| Navigazione | `androidx.navigation:navigation-compose` | Type-safe routes, equivale a vue-router |
| DI | `io.insert-koin:koin-androidx-compose` | Leggero, Kotlin-first |
| Coroutines/Flow | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Reattività alla Vue |
| DataStore | `androidx.datastore:datastore-preferences` | Sostituisce localStorage |
| CSV export | `com.github.doyaaaaaken:kotlin-csv` | Equivalente PapaParse |
| SHA-256 | `java.security.MessageDigest` (built-in) | Password hashing |
| Work Manager | `androidx.work:work-runtime-ktx` | Sync offline + promemoria |
| Notification | `androidx.core:core-ktx` | Promemoria misurazioni |

---

## Mappatura Funzionalità 1:1

### 1. Auth & User Management

| Funzionalità | Dettaglio Android |
| --- | --- |
| Login/Logout | Stessa table-based auth, SHA-256 hash, chiamata RPC a `public.users` |
| Sessione 8h TTL | `DataStore` con timestamp scadenza |
| Recupero password via email | `app_request_password_reset` + `app_complete_password_recovery` |
| Cambio password/email | Da SettingsScreen |
| RBAC admin/user | Admin: OperatoriScreen con gestione utenti |
| Profilo (età, genere) | Form profilo con `DatePicker` per data di nascita |
| Prompt profilo | Dialog mostrato al primo accesso se incompleto |

### 2. Dashboard (HomeScreen)

| Funzionalità | Dettaglio Android |
| --- | --- |
| Saluto + ultima lettura | Card con categoria ESC/ESH e valori |
| 4 KPI rapidi | `LazyRow` con card: media SYS/DIA/BPM + conteggio |
| 5 letture recenti | `LazyColumn` con `ReadingCard` |
| Empty state + guida 3-step | Illustrazione onboarding |
| Sync status banner | `TopAppBar` con indicatore + retry |
| Skeleton loader | `shimmer` modifier |

### 3. CRUD Misurazioni

| Funzionalità | Dettaglio Android |
| --- | --- |
| Inserimento/modifica | Form con `TextField` validati, `DatePicker`, `TimePicker` |
| Classificazione live ESC/ESH | Calcolo in tempo reale, badge colorato |
| Validazione range clinici | DIA < SYS, range fisiologici |
| Rilevazione duplicati | Blocco stesso timestamp entro 10 min |
| Eliminazione con conferma | `AlertDialog` |
| Offline-first | Room → Supabase sync via `WorkManager` |

### 4. Lista Misurazioni (ReadingListScreen)

| Funzionalità | Dettaglio Android |
| --- | --- |
| Lista cronologica | `LazyColumn` |
| Filtro categoria | `FilterChip` row (6 chip ESC/ESH) |
| Ricerca testuale | `SearchBar` su note/valori |
| Swipe-to-delete | `SwipeToDismissBox` (Material3) |
| Icona oraria | Sole/luna in base all'ora |

### 5. Analisi & Statistiche (AnalysisScreen)

| Funzionalità | Dettaglio Android |
| --- | --- |
| Filtri periodo (7g/30g/custom) | `SegmentedButton` + `DateRangePicker` |
| Tab: Andamento / Variazioni / Distribuzione | `TabRow` + `HorizontalPager` |
| Line chart SYS/DIA/BPM | YCharts/Vico con colori theme-aware |
| Zona target ESC/ESH (90-140) | Annotazione verde tratteggiata |
| Soglia 140 tratteggiata | Linea rossa |
| Grafico derivate dP/dt | Bar chart, allarme >10 mmHg/h |
| Morning Surge | Card con badge ⚠️ |
| Carico Ipertensivo | KPI percentuale |
| HRV | Deviazione standard BPM |
| Doughnut OMS | 4 categorie, tappable → filtra lista |
| Confronto 7/30 giorni | Tabella multi-periodo |
| Auto-aggregazione >50 punti | Media mobile |
| Dark mode | `isSystemInDarkTheme()` + switch manuale |

### 6. Report e Condivisione

| Funzionalità | Dettaglio Android |
| --- | --- |
| PDF con grafici | iText7 + bitmap dei grafici |
| Condivisione PDF | `ShareSheet` (Intent.ACTION_SEND) |
| Link temporaneo 48h | Token UUID + PIN opzionale |
| Revoca link | Lista attivi + bottone revoca |
| PIN gate | Schermata inserimento PIN |
| Dashboard condivisa | SharedReportScreen con KPI + grafici |
| Fasce orarie nel report | Tabella raggruppata per fascia |

### 7. Impostazioni (SettingsScreen)

| Funzionalità | Dettaglio Android |
| --- | --- |
| Account (email, password) | Form edit |
| Profilo (data nascita, genere) | Form con picker |
| Promemoria | Giorni + orario, notifiche native |
| Fasce orarie personalizzabili | `TimePicker` per ogni fascia |
| Dark mode | Toggle |
| Lingua (it/en) | Switch con riavvio |
| Eliminazione massiva dati | Conferma + eliminazione |
| Info app / versione | About section |
| Esportazione CSV | File via `ShareSheet` |

---

## Piano di Sviluppo — Fasi (stato finale)

### Fase 0: Setup progetto ✅ COMPLETATA

- [x] Progetto Gradle + AGP 8.5.2 + Kotlin 2.0.0 + KSP
- [x] Dipendenze: Compose BOM, Room, Supabase-kt, Ktor, Koin, WorkManager, iText7, serialization
- [x] `gradle.properties` con `SUPABASE_URL` + `SUPABASE_PUBLISHABLE_KEY` (da `.env`)
- [x] Icone launcher in `res/mipmap-*/`
- [x] Supabase client singleton con logging
- [x] Room database (readings, users, settings, medications)
- [x] Koin + Navigation Compose

### Fase 1: Auth + Core ✅ COMPLETATA

- [x] LoginScreen table-based auth (SHA-256), placeholder come la Vue
- [x] SessionManager DataStore, TTL 8h
- [x] Password recovery flow (stub: form "Invia richiesta", RPC non cablato)
- [x] RBAC admin/user guard (sezione Admin nelle impostazioni — stub)
- [x] Room DAO: UserEntity

### Fase 2: CRUD Misurazioni ✅ COMPLETATA

- [x] Room DAO: ReadingEntity
- [x] ReadingRepository offline-first (Room → Supabase)
- [x] AddEditReadingScreen con validazione **localizzata** (range, DIA<SYS)
- [x] Classificazione ESC/ESH live (7 categorie)
- [x] ReadingListScreen (lista, 4 filtri raggruppati, ricerca, swipe-to-delete)
- [x] SyncWorker WorkManager (15 min)

### Fase 3: Dashboard + Stats ✅ COMPLETATA

- [x] HomeScreen (saluto orario, ultima lettura, KPI, letture recenti, empty state)
- [x] AnalysisScreen con TabRow (Andamento / Variazioni / Distribuzione)
- [x] Grafici **Canvas nativi** (no libreria esterna): line `BpTrendChart`, bar derivate `ExtraCharts`, doughnut `CategoryDoughnutChart`
- [x] Zona target 90-140 + soglia 140 tratteggiata
- [x] Morning Surge, Carico Ipertensivo, HRV
- [x] Dark mode di sistema (`isSystemInDarkTheme()`)

### Fase 4: Report PDF + Condivisione ✅ COMPLETATA

- [x] PdfReportGenerator iText7 (header, stats, ESC/ESH, farmaci, tabella, disclaimer)
- [x] Condivisione PDF via Intent.ACTION_SEND
- [x] Link temporaneo 48h + PIN 4 cifre (tabella `shared_reports`)
- [x] SharedReportScreen con PIN gate (fetch via token)
- [x] Deep link `iperteso://share/{token}`

### Fase 5: Impostazioni + Polish ✅ COMPLETATA

- [x] SettingsScreen (account, admin stub, aspetto, lingua, promemoria, farmaci, dati, info)
- [x] Notifiche native per promemoria (AlarmManager giornaliero 08:00)
- [x] Esportazione CSV
- [x] i18n it/en con riavvio (`LocaleManager` + `values-en`)
- [x] Eliminazione dati massiva
- [x] Test su emulatore Galaxy_S24_API_34

### Fase 6: Release ✅ COMPLETATA

- [x] APK release firmata con keystore (21 MB)
- [x] Test: 63 unit + 20 strumentati (Room DAO)
- [ ] Pubblicazione (Play Store / APK diretto) — da fare

---

## Parità funzionale vs Web (verificata 2026-08-13)

> **Esito:** parità quasi completa sulle funzioni core; la web app resta più ricca su alcune funzioni "power" (import/backup, fasce configurabili, interattività grafici). Le due app condividono lo stesso backend Supabase e le stesse utenze.

### Al parità ✅

| Funzione | Note |
| --- | --- |
| Auth table-based (SHA-256) + sessione 8h | Identico meccanismo |
| CRUD letture offline-first | Room ↔ IndexedDB |
| Classificazione live | ESC/ESH (Vue: 6 cat. con Ipotensione; Android: 7 cat. ESC/ESH 2024) |
| Validazione range + DIA<SYS + duplicati 10min | Localizzata anche su Android |
| Dashboard: saluto, ultima lettura, 4 KPI, recenti, empty state | |
| Lista: filtri, ricerca, swipe-to-delete | Android: 4 chip raggruppati |
| Analisi: line/bar/doughnut, zona target, soglia 140, Morning Surge, Carico Ipertensivo, HRV | Android: Canvas nativi vs Chart.js interattivo |
| PDF + condivisione | Android: iText7 vs jsPDF |
| Link temporaneo 48h + PIN | Vue: `settings._share_*`; Android: tabella `shared_reports` |
| PIN gate su report condiviso | |
| Promemoria | Vue: giorni+orari multipli; Android: singolo giornaliero 08:00 |
| Lingua it/en | Vue: switch reattivo; Android: switch con riavvio |
| Farmaci (medications) | Portata su Vue in questa sessione |
| CSV export | |

### Solo Web (non ancora su Android) ⚠️

| Funzione | Note |
| --- | --- |
| CSV import / backup-restore JSON / dati di test | Non implementati su Android |
| Fasce orarie configurabili | Android usa fasce fisse Mattina/Pomeriggio/Sera/Notte |
| Confronto 7/30 giorni tabella multi-periodo | Android: filtri 7/30/90 giorni |
| Tooltip hover + grafici cliccabili | Canvas statici su Android |
| Cambio password/email in impostazioni | Android: recovery stub |
| Profilo esteso (nome, cognome, CF, telefono, indirizzo) | Android: solo data nascita/genere assenti |
| Gestione utenti admin (OperatoriView) | Android: sezione Admin stub |
| PWA install, SW update, offline banner | N/A su Android nativo |

### Solo Android (non su Web) ✅

| Funzione | Note |
| --- | --- |
| Deep link nativo `iperteso://share/{token}` | |
| Notifiche native di sistema | |

## Test — Parità casi di test (verificata 2026-08-13)

| Suite | Web (Vue) | Android |
| --- | --- | --- |
| Unit test | **270** (Vitest) | **63** (JUnit4 + MockK) |
| Strumentati (device) | — | **20** (Room DAO su emulatore) |
| E2E | **123 passed + 2 skipped** (Playwright) | — (test manuale su emulatore) |
| Build | `vite build` ✅ | `assembleRelease` ✅ (APK firmata) |

**Copertura casi di test a confronto (Vue ↔ Android unit test):**

| Area | Vue | Android |
| --- | --- | --- |
| Auth | 34 test | AuthViewModel (login/hash/sessione) |
| Statistiche (stats/derivate/surge/carico/HRV) | 29 test | — (logica inline, non estratta) |
| Categorie/classificazione | 9 test | Category.classify coperto via DAO/VM |
| Theme | 8 test | — |
| KeepAlive / errorHandling / ids / rbac | 18 test | — |
| DB locale (DAO) | — (IndexedDB) | 20 strumentati Room |
| UI componenti | vari | — |
| Validazioni letture | AddEditReadingView | AddEditReadingViewModel (range/DIA< SYS/duplicati) |

> **Nota parità test:** la web app ha una suite molto più estesa (270 unit + 123 E2E) rispetto ad Android (63 unit + 20 strumentati). Le aree non coperte su Android sono principalmente la logica statistica (estratte in `statistics.js` su Vue, inline nelle schermate su Android) e le funzioni power (import/backup/fasce). Per allineare i casi di test andrebbe estratta la logica statistica Android in unit testabili (vedi § "Azioni raccomandate").

---

## Note Tecniche Importanti

1. **Auth table-based, NON Supabase Auth**: La web app usa autenticazione custom su `public.users` con password SHA-256. L'app Android deve replicare lo stesso meccanismo, NON usare Supabase Auth SDK.

2. **Offline-first**: La web app usa Dexie (IndexedDB) come cache offline. Su Android si usa Room (SQLite). La logica è identica: scrivi in locale, sync in background.

3. **RLS non bloccante**: Il DB Supabase ha RLS attivo ma le policy sono `USING (true)` — quindi l'anon key funziona per tutte le operazioni. I controlli di autorizzazione sono lato client. L'app Android deve replicare questa logica.

4. **Keystore**: Il file `iperteso/android/keystore/release.keystore` è già generato. Le credenziali sono in `.env`. **Non committare il keystore.**

5. **Icone**: Le icone Android (mipmap-hdpi/mdpi/xhdpi/xxhdpi/xxxhdpi) sono già pronte in `iperteso/android/`.

6. **Samsung S24**: Display 6.2" 1080x2340 (416 DPI, xxhdpi). Supporta Android 14 (API 34). Il target SDK è 34.

---

## Variabili d'Ambiente (`.env`)

```
ANDROID_KEYSTORE_PATH=iperteso/android/keystore/release.keystore
ANDROID_KEYSTORE_PASSWORD=iperteso2026!
ANDROID_KEY_ALIAS=iperteso
ANDROID_KEY_PASSWORD=iperteso2026!
ANDROID_TARGET_SDK=34
ANDROID_MIN_SDK=26
ANDROID_COMPILE_SDK=34
APP_NAME=IperTeso
APP_PACKAGE=com.pressione.iperteso
APP_VERSION_CODE=1
APP_VERSION_NAME=1.0.0
```

Queste variabili vanno propagate in `gradle.properties` o nel `build.gradle.kts`:

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.pressione.iperteso"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    signingConfigs {
        create("release") {
            storeFile = file("../../keystore/release.keystore")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: project.findProperty("KEYSTORE_PASS") as String?
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "iperteso"
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: project.findProperty("KEY_PASS") as String?
        }
    }
}
```

---

## Comandi Utili

```bash
# Build debug
./gradlew assembleDebug

# Build release firmata
export $(grep -v '^#' .env | xargs) && ./gradlew assembleRelease

# Installare su dispositivo (Samsung S24 connesso via USB)
adb -d install app/build/outputs/apk/release/app-release.apk

# Emulatore Samsung S24
sdkmanager "system-images;android-34;google_apis_playstore;arm64-v8a"
avdmanager create avd -n s24 -k "system-images;android-34;google_apis_playstore;arm64-v8a" -d "pixel_6_pro"
```

---

## $impeccable — Miglioramento UI/UX

La skill `impeccable` (v4.0.4) è un direttore di design AI-driven che eleva la qualità visiva e funzionale delle interfacce. Per IperTeso Android, in **modalità Operate** (app medicale per completare task), ecco i comandi consigliati in ordine di priorità.

### Raccomandazioni prioritarie

| # | Comando | Perché | Quando |
| --- | --------- | -------- | -------- |
| 1 | `$impeccable critique iperteso/app/src/main/java/com/pressione/iperteso/ui/` | Il progetto non è mai stato sottoposto a design review. Una UX heuristic evaluation identifica problemi di usabilità prima di scrivere altro codice. | **Subito** — prima di completare le schermate placeholder |
| 2 | `$impeccable document` | Manca un `DESIGN.md` che catturi il design system Android: token, colori, tipografia, spaziature. Senza, ogni nuova schermata reinventa decisioni già prese. | Dopo la critique, prima di nuove schermate |
| 3 | `$impeccable polish iperteso/app/src/main/java/com/pressione/iperteso/ui/screens/home/` | La HomeScreen è la schermata più completa. Un quality pass sistematico su spaziature, allineamenti, contrasto e coerenza con Material3 prima di usarla come modello per le altre. | Dopo aver completato le schermate mancanti |

### Menu completo (raggruppato per intento)

#### Valutare (Evaluate)

| Comando | Descrizione | Target Android |
|---------|-------------|----------------|
| `$impeccable critique [target]` | UX heuristic review con scoring | Qualsiasi schermata o componente |
| `$impeccable audit [target]` | Controlli tecnici: accessibilità, contrasto, touch target | Usa il reference nativo `audit.native.md` per Android |

#### Raffinare (Refine)

| Comando | Descrizione | Quando usarlo |
| --------- | ------------- | --------------- |
| `$impeccable polish [target]` | Quality pass pre-ship: spacing, alignment, consistency | Prima della release |
| `$impeccable bolder [target]` | Amplificare un design troppo timido o piatto | Se la dashboard sembra "generica Material3" |
| `$impeccable quieter [target]` | Attenuare elementi troppo aggressivi | Se i badge ESC/ESH rossi sono eccessivi |
| `$impeccable distill [target]` | Ridurre all'essenza, rimuovere complessità | Se una schermata ha troppi controlli |
| `$impeccable harden [target]` | Error states, edge case, loading, empty states | Prima del test su dispositivo reale |
| `$impeccable onboard [target]` | First-run flow, empty state, guida 3-step | Per la schermata vuota della dashboard |

#### Arricchire (Enhance)

| Comando | Descrizione | Quando usarlo |
| --------- | ------------- | --------------- |
| `$impeccable animate [target]` | Animazioni e micro-interazioni | Per transizioni tra schermate, shimmer, skeleton |
| `$impeccable colorize [target]` | Colore strategico in UI monocromatiche | Se il tema verde medicale risulta piatto |
| `$impeccable typeset [target]` | Gerarchia tipografica e font | Per la scala tipografica Inter su Android |
| `$impeccable layout [target]` | Spaziatura, ritmo, gerarchia visiva | Se le card KPI sembrano disallineate |
| `$impeccable delight [target]` | Personalità e tocchi memorabili | Per il saluto, l'icona cuore, dettagli "amici" |
| `$impeccable overdrive [target]` | Spingere oltre i limiti convenzionali | Per effetti visivi ambiziosi nei grafici |

#### Correggere (Fix)

| Comando | Descrizione | Target Android |
| --------- | ------------- | ---------------- |
| `$impeccable clarify [target]` | UX copy, label, messaggi di errore | `strings.xml` (it/en) |
| `$impeccable adapt [target]` | Adattamento per diverse risoluzioni | Usa il reference nativo `adapt.native.md` |
| `$impeccable optimize [target]` | Performance UI (ricomposizioni, lag) | Schermate con LazyColumn pesanti |

### Flusso consigliato per IperTeso Android

```
FASE ATTUALE (schermate placeholder)
  │
  ├─► 1. $impeccable critique   → Identifica problemi UX prima di codificare
  ├─► 2. $impeccable document   → Cattura il design system in DESIGN.md
  │
FINISCI SCHERMATE (AddEdit, List, Analysis, Settings, Operators)
  │
  ├─► 3. $impeccable polish     → Quality pass sulle schermate complete
  ├─► 4. $impeccable harden     → Error/empty/loading/edge case states
  ├─► 5. $impeccable onboard    → Empty state dashboard + first-run flow
  │
PRE-RELEASE
  │
  ├─► 6. $impeccable animate    → Micro-interazioni e transizioni
  ├─► 7. $impeccable delight    → Tocchi "amici": claim, cuore, saluto
  ├─► 8. $impeccable audit      → Accessibilità e touch target su S24
  └─► 9. $impeccable polish     → Pass finale prima della release
```

### Note per Android nativo

- `$impeccable live` e `detect.mjs` sono **web-only** — non usarli per Android.
- I comandi `audit` e `adapt` hanno reference nativi (`audit.native.md`, `adapt.native.md`) specifici per platform Android.
- La skill analizza il codice Kotlin/Compose ma non esegue un emulatore: ispeziona i file `.kt` direttamente.
- Per eseguire un comando, assicurati che il target sia un file o directory Kotlin (es. `iperteso/app/src/main/java/com/pressione/iperteso/ui/`).
