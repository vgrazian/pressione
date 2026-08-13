# IperTeso — Piano di Sviluppo Android (Kotlin)

> **Versione target:** 1.0.0 | **Data piano:** 2026-08-12  
> **Target device:** Samsung Galaxy S24 (Android 14, API 34)  
> **Min SDK:** 26 (Android 8.0) | **Compile SDK:** 34  
> **Stack:** Kotlin 2.3.0 · Jetpack Compose · Supabase Kotlin SDK · Room (offline cache)  
> **Parità funzionale:** 1:1 con l'app web Pressione v1.2.1  
> **DB condiviso:** Stesso schema Supabase, stesse utenze, stesso backend

---

## Stato dell'Ambiente (verificato 2026-08-12)

| Componente | Versione | Stato |
| --- | --- | --- |
| Java | OpenJDK 17.0.18 | ✅ |
| Kotlin | 2.3.0 | ✅ |
| Android SDK | 34/35/36 (build-tools 36.1.0) | ✅ |
| Android Studio | `/Applications/Android Studio.app` | ✅ |
| Gradle | 9.3.0 | ✅ |
| Keystore release | `iperteso/android/keystore/release.keystore` | ✅ |
| Icone Android | `iperteso/android/mipmap-*/` | ✅ |

---

## Architettura

```
iperteso/
├── app/
│   ├── src/main/
│   │   ├── java/com/pressione/iperteso/
│   │   │   ├── IperTesoApplication.kt          # Application class
│   │   │   ├── MainActivity.kt                  # Single-activity entry
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt           # Room DB (offline cache)
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── ReadingDao.kt
│   │   │   │   │   │   ├── UserDao.kt
│   │   │   │   │   │   ├── SettingsDao.kt
│   │   │   │   │   │   └── ReminderDao.kt
│   │   │   │   │   └── entity/
│   │   │   │   │       ├── ReadingEntity.kt
│   │   │   │   │       ├── UserEntity.kt
│   │   │   │   │       └── SettingEntity.kt
│   │   │   │   ├── remote/
│   │   │   │   │   ├── SupabaseClient.kt         # Inizializzazione Supabase
│   │   │   │   │   └── api/
│   │   │   │   │       ├── AuthApi.kt            # Login/register RPC
│   │   │   │   │       ├── ReadingsApi.kt        # CRUD letture
│   │   │   │   │       ├── SettingsApi.kt        # Settings key-value
│   │   │   │   │       └── SharedReportApi.kt    # Report condivisi
│   │   │   │   └── repository/
│   │   │   │       ├── AuthRepository.kt
│   │   │   │       ├── ReadingRepository.kt      # Offline-first logic
│   │   │   │       ├── SettingsRepository.kt
│   │   │   │       └── SyncRepository.kt
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Reading.kt
│   │   │   │   │   ├── User.kt
│   │   │   │   │   ├── Category.kt               # ESC/ESH enum
│   │   │   │   │   └── TimeBand.kt
│   │   │   │   └── usecase/
│   │   │   │       ├── ClassifyReadingUseCase.kt
│   │   │   │       ├── ComputeStatisticsUseCase.kt
│   │   │   │       ├── GeneratePdfUseCase.kt
│   │   │   │       └── ShareReportUseCase.kt
│   │   │   ├── ui/
│   │   │   │   ├── navigation/
│   │   │   │   │   └── NavGraph.kt               # Navigation routes
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Theme.kt                  # Material3 + dark mode
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── ReadingCard.kt
│   │   │   │   │   ├── CategoryBadge.kt
│   │   │   │   │   ├── DateRangePicker.kt
│   │   │   │   │   ├── TimeOfDayIcon.kt
│   │   │   │   │   ├── SkeletonLoader.kt
│   │   │   │   │   └── ConfirmDialog.kt
│   │   │   │   └── screens/
│   │   │   │       ├── LoginScreen.kt
│   │   │   │       ├── HomeScreen.kt             # Dashboard + KPI
│   │   │   │       ├── AddEditReadingScreen.kt
│   │   │   │       ├── ReadingListScreen.kt
│   │   │   │       ├── AnalysisScreen.kt         # Statistiche + Grafici + Report
│   │   │   │       ├── ReportScreen.kt           # PDF preview + share
│   │   │   │       ├── SharedReportScreen.kt     # Report pubblico via token
│   │   │   │       ├── SettingsScreen.kt
│   │   │   │       └── AdminOperatorsScreen.kt
│   │   │   └── util/
│   │   │       ├── NetworkMonitor.kt
│   │   │       ├── PasswordHasher.kt             # SHA-256
│   │   │       └── DateTimeExt.kt
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml                  # it (default) + en
│   │   │   │   └── colors.xml
│   │   │   ├── values-it/
│   │   │   │   └── strings.xml
│   │   │   ├── drawable/                         # Vector assets
│   │   │   └── mipmap-*/                         # Launcher icons (già pronti)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts                              # Root build
├── settings.gradle.kts
└── gradle.properties
```

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

## Piano di Sviluppo — Fasi

### Fase 0: Setup progetto (Giorno 1)

- [ ] Creare progetto Android Studio con template Empty Compose Activity
- [ ] Configurare `build.gradle.kts` con tutte le dipendenze
- [ ] Configurare `gradle.properties` con le variabili da `.env`
- [ ] Copiare icone da `iperteso/android/mipmap-*/` in `res/`
- [ ] Configurare Supabase client (stesso URL, stessa publishable key)
- [ ] Setup Room database con schema identico a Supabase
- [ ] Setup Koin per dependency injection
- [ ] Setup Navigation Compose con route

### Fase 1: Auth + Core (Giorno 2-3)

- [ ] LoginScreen (table-based auth, SHA-256)
- [ ] Session management (DataStore, TTL 8h)
- [ ] Password recovery flow (email → token → reset)
- [ ] RBAC (admin/user) guard sulle route
- [ ] Room DAO: UserEntity, migrazione schema

### Fase 2: CRUD Misurazioni (Giorno 4-5)

- [ ] Room DAO: ReadingEntity
- [ ] ReadingRepository (offline-first: Room → Supabase sync)
- [ ] AddEditReadingScreen (form con validazione)
- [ ] Classificazione ESC/ESH live
- [ ] ReadingListScreen (lista, filtri, ricerca, swipe-to-delete)
- [ ] WorkManager sync worker

### Fase 3: Dashboard + Stats (Giorno 6-8)

- [ ] HomeScreen (saluto, ultima lettura, KPI, letture recenti)
- [ ] AnalysisScreen con TabRow
- [ ] Grafici: line (SYS/DIA), bar (derivate), doughnut (OMS)
- [ ] Annotazioni ESC/ESH (zona target, soglia 140)
- [ ] Morning Surge, Carico Ipertensivo, HRV
- [ ] Dark mode reattiva

### Fase 4: Report PDF + Condivisione (Giorno 9-10)

- [ ] GeneratePdfUseCase (iText7, header, stats, tabella, grafici)
- [ ] Condivisione PDF (Intent.ACTION_SEND)
- [ ] Link temporaneo + PIN
- [ ] SharedReportScreen (dashboard pubblica)
- [ ] Revoca link

### Fase 5: Impostazioni + Polish (Giorno 11-12)

- [ ] SettingsScreen (account, profilo, promemoria, fasce orarie)
- [ ] Notifiche native per promemoria
- [ ] Esportazione CSV
- [ ] i18n (it/en)
- [ ] Eliminazione dati massiva
- [ ] Test su Samsung S24 (fisico/emulatore)

### Fase 6: Release (Giorno 13)

- [ ] Firmare APK/AAB con keystore release
- [ ] Test E2E su dispositivo
- [ ] Pubblicazione (Play Store / APK diretto)

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
