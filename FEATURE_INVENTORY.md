# Pressione / IperTeso — Feature Inventory & Review (Web + Android)

> **Versione Web:** 1.2.1 | **Versione Android:** 1.0.0 | **Data:** 2026-08-13  
> **Web:** <https://vgrazian.github.io/pressione/> — Vue 3 + Vite PWA · Supabase · Dexie/IndexedDB (+ localStorage bridge) · Chart.js · jsPDF  
> **Android:** Kotlin 2.0.0 + Jetpack Compose (M3) · Supabase Kotlin SDK · Room · Koin · WorkManager · iText7 · Canvas charts  
> **Test Web:** 270 unit (Vitest) + 123 E2E (Playwright, 2 skipped)  
> **Test Android:** 63 unit (JUnit4/MockK) + 20 strumentati (Room DAO su emulatore)  
> **DB condiviso:** stesso Supabase, stesse utenze, stesso schema

---

## Indice

1. [Auth & User Management](#1-auth--user-management)
2. [Dashboard](#2-dashboard-home)
3. [CRUD Misurazioni](#3-crud-misurazioni)
4. [Lista Misurazioni](#4-lista-misurazioni)
5. [Analisi](#5-analisi-statistiche--report-unificati)
6. [Report e Condivisione](#6-report-e-condivisione-integrati-in-analisi)
7. [Impostazioni](#7-impostazioni)
8. [UI/UX](#8-uiux)
9. [Infrastruttura](#9-infrastruttura)
10. [Changelog Web (Agosto 2026)](#10-changelog-web-agosto-2026)
11. [Feature Android](#11-feature-android)
12. [Matrice di parità Web ↔ Android](#12-matrice-di-parità-web--android)

---

## 1. Auth & User Management

| Feature | Note |
| --- | --- |
| Login/Logout | Table-based auth, SHA-256 |
| Sessione TTL 8h | localStorage + IndexedDB fallback |
| **Recupero password via email** | **NUOVO** — Link con token UUID (30 min), pagina `/reset-password` dedicata, RPC `app_request_password_reset` + `app_complete_password_recovery` |
| Cambio password | Da Impostazioni (richiede pw attuale) |
| Modifica email | Da Impostazioni → Account |
| RBAC admin/user | Admin: gestione utenti, reset pw |
| Topbar logout | Icona in alto a destra su ogni pagina |
| 9 utenti seed | admin, valerio (admin) + 7 user |
| **Profilo utente** | Data di nascita + genere, età calcolata dinamicamente |
| **Prompt profilo** | Mostrato solo se incompleto, mai più se skippato/salvato |

## 2. Dashboard (Home)

| Feature | Note |
| --- | --- |
| Saluto + ultima lettura | Card uniforme con categoria e valori |
| 4 KPI rapidi | Media SYS/DIA/BPM + conteggio |
| 5 letture recenti | Card compact |
| Empty state + guida 3-step | Step numerati: tocca, inserisci, monitora |
| **Sync status banner** | "Sincronizzazione..." con pulsante riprova in caso di errore |
| Skeleton loader | |

## 3. CRUD Misurazioni

| Feature | Note |
| --- | --- |
| Inserimento/modifica | SYS, DIA, BPM, data, ora, note |
| Classificazione live | ESC/ESH in tempo reale |
| Validazione | Range clinici + DIA < SYS |
| Rilevazione duplicati | Blocca stessa misurazione entro 10 min |
| Eliminazione | Singola con conferma, massiva da Impostazioni |
| Offline-first | IndexedDB → Supabase |

## 4. Lista Misurazioni

| Feature | Note |
| --- | --- |
| Lista cronologica | |
| Filtro categoria | 6 chip ESC/ESH |
| Ricerca testuale | Note e valori |
| Swipe-to-delete | Mobile, touch events nativi |
| Icona oraria | Sole/luna |
| Edit/Delete | Su ogni card |
| Skeleton loader | |

## 5. Analisi (Statistiche + Report unificati)

| Feature | Note |
| --- | --- |
| Filtri: 7g, 30g, custom | Date picker |
| **Chart a tab** | Andamento (linee) / Variazioni (barre) / Distribuzione (doughnut) — scroll ridotto |
| **Colori theme-aware** | `chartColors.js` — legge `--color-*` token a ogni render, si adatta a dark mode |
| **Line chart (Chart.js)** | SYS (--color-error), DIA (blu derivato), BPM (--color-text-secondary) |
| **Fascia target ESC/ESH** | Zona verde tratteggiata 90-140 mmHg con label |
| **Linea soglia 140** | Tratteggiata rossa nel grafico BP |
| **Hover tooltip interattivi** | Data/ora + valore + categoria ESC/ESH |
| **Grafico derivate dP/dt** | mmHg/ora, barre: allarme >10 (rosso pieno), positivo (red 50%), negativo (blue 50%) |
| **Allarme >10 mmHg/h** | Barre rosse + alert box |
| **Morning Surge** | Δ fasce configurabili, badge ⚠️ |
| **Carico Ipertensivo** | % fuori norma in KPI |
| **HRV** | Deviazione standard BPM |
| **Pie chart OMS cliccabile** | 4 categorie, click → filtra lista |
| **Confronto 7/30 giorni** | Tabella multi-periodo con 8 metriche |
| Auto-aggregazione | Per >50 punti |
| Skeleton loader | |
| **Fasce orarie configurabili** | Mattina/Pomeriggio/Sera/Notte con orari personalizzabili |
| **Theme watcher** | `watch(theme)` → re-render immediato dei chart al cambio tema |

## 6. Report e Condivisione (integrati in Analisi)

| Feature | Note |
| --- | --- |
| Filtri contenuto | Periodo, includi grafici, anonimizza |
| **PDF (jsPDF)** | A4 con header, stats, tabella, grafici incorporati |
| **PDF come Blob** | `generatePDFBlob()` per condivisione file |
| **Condivisione con PDF allegato** | Email, WhatsApp, Web Share API con file |
| **Link temporaneo 48h** | Token + PIN 4 cifre opzionale |
| **Revoca link** | Lista attivi + bottone revoca |
| **PIN gate** | SharedReportView con schermata PIN |
| **Dashboard medico interattiva** | SharedReportView: KPI, classificazione, alert, grafici interattivi |
| **Grafico BP nel report** | Con zona target, hover tooltip, filtro 7/30/tutto |
| **Card fasce orarie** | Media per fascia nel report condiviso |
| **Grafico derivate + doughnut** | Nel report condiviso (colori theme-aware) |
| **Vista per fascia oraria** | Toggle Lista/Per fascia con tabella raggruppata giorno+fascia |

## 7. Impostazioni

| Feature | Note |
| --- | --- |
| Lingua IT/EN | Selettore con radio button (Italiano/English) |
| Account | Username, email, ruolo |
| Modifica email/password | Password in sezione collassabile |
| **Farmaci (medications)** | **NUOVO** — tracciamento farmaci: CRUD, dosaggio, frequenza, date inizio/fine, stato attivo/storico, inclusi nel PDF |
| **Profilo** | Data di nascita con età calcolata + genere + anagrafica completa |
| Promemoria | Multipli, orari + giorni |
| **Fasce orarie configurabili** | Sezione collassabile, slider interattivo |
| CSV Export / Import | Supporto formato bp-tracker |
| Backup / Ripristino JSON | |
| Genera dati test | 30 letture |
| Gestione utenti (admin) | Ruolo, reset pw, disattiva |
| **Cache & Aggiornamenti** | Sezione collassabile, "Forza aggiornamento" |
| **Keep-Alive DB** | Sezione collassabile, toggle on/off |
| **PWA Install** | Sezione collassabile, istruzioni iOS + pulsante Android |
| Elimina tutto | Danger Zone con doppia conferma |

## 8. UI/UX

| Feature | Note |
| --- | --- |
| Design system CSS | Variabili, radius 12px, FAB rotonda |
| **Micro-interazioni** | Card: hover shadow + active scale(0.99); pulsanti: active scale(0.97) |
| **Transizioni view** | Fade + slide animato tra pagine (`view-fade`) |
| Dark mode | `prefers-color-scheme` + `[data-theme]` toggle |
| **Dark mode badge** | `text-shadow` per migliorare contrasto categorie |
| Font Inter | Google Fonts, swap |
| Icone SVG | AppIcon (18 icone) |
| Skeleton loader | Componente riutilizzabile |
| Confirm dialog | Globale via store condiviso (`confirmDialog.js`) |
| Top bar + bottom nav | Sticky, 4 tab (logo-only topbar) |
| Offline banner | Giallo se no Supabase |
| Focus-visible, reduced-motion | A11y |
| PWA | Installabile, SW, manifest |

## 9. Infrastruttura

| Feature | Note |
| --- | --- |
| Retry backoff | 2 tentativi esponenziali |
| Offline-first | Dexie sempre disponibile |
| **LocalStorage bridge** | Backup letture per compatibilità iOS PWA (IndexedDB isolato tra Safari e standalone) |
| Sync queue | Coda operazioni offline |
| RLS policies | Tutte le tabelle |
| SHA-256 hashing | Client + server |
| GDPR link TTL | 48 ore auto-scadenza |
| Stats cache | IndexedDB pronto |
| **Release script** | `scripts/deploy.sh` — git worktree isolato, safety gate .env, idempotente |
| **Version from package.json** | Single source of truth, build number da git hash |
| **Force cache clear** | `forceClearCache()` — deregistra SW, svuota caches, reload |
| **270 unit + 123 E2E** | Suite Vitest + Playwright riallineata all'app corrente |

## 10. Changelog Web (Agosto 2026)

| # | Feature | File(s) |
| --- | --- | --- |
| 1 | **AnalisiView unificata** — Stats+Report fusi con chart a tab, scroll ridotto | `AnalisiView.vue`, `router/index.js` |
| 2 | **Bottom nav 5→4** — Home, Lista, Analisi, Altro | `AppNav.vue` |
| 3 | **Topbar logo-only** — rimosso testo "Pressione" ridondante | `App.vue` |
| 4 | **Colori chart theme-aware** — `chartColors.js` legge `--color-*` token, si adatta a dark mode | `chartColors.js`, `AnalisiView.vue`, `SharedReportView.vue` |
| 5 | **Sync status banner** — "Sincronizzazione..." con pulsante riprova in HomeView | `HomeView.vue` |
| 6 | **Empty state con guida 3-step** — step numerati per primo utilizzo | `HomeView.vue` |
| 7 | **Router-view fade transition** — animazione tra pagine | `App.vue` |
| 8 | **Settings sezioni collassabili** — Password, Fasce Orarie, Dati, Keep-Alive, PWA, Cache | `SettingsView.vue`, `CollapsibleSection.vue` |
| 9 | **Card micro-interazioni** — hover shadow, active scale su `.card` e `.reading-card` | `style.css`, `ReadingCard.vue` |
| 10 | **FAB rotonda** — `border-radius: var(--radius-full)` coerente con badge/chip | `style.css` |
| 11 | **Dark mode badge contrast** — `text-shadow` per leggibilità categorie | `style.css` |
| 12 | **LocalStorage bridge** — backup letture per iOS PWA (IndexedDB isolato) | `dataService.js` |
| 13 | **Deploy script safe** — git worktree isolato, safety gate .env, idempotente | `scripts/deploy.sh` |
| 14 | **Data di nascita invece di età** — calcolo dinamico con `computeAge()` | `ProfilePrompt.vue`, `SettingsView.vue`, `auth.js`, `supabaseTableAuth.js`, `pdfReport.js` |
| 15 | **Prompt profilo non ripetitivo** — `refreshSession()` persiste flag, `initAuth()` await | `auth.js`, `App.vue` |
| 16 | **Forza aggiornamento cache** — deregistra SW + svuota caches + reload | `swUpdate.js`, `SettingsView.vue` |
| 17 | **Keep-alive DB default ON** — ping Supabase 5min + persistent storage | `keepAlive.js` |
| 18 | **Fasce orarie configurabili** — servizio `timeBands.js`, UI in Impostazioni, consumer in stats/report | `timeBands.js`, `SettingsView.vue`, `statistics.js`, `TimeOfDayIcon.vue` |
| 19 | **Vista report per fascia oraria** — toggle Lista/Per fascia, tabella raggruppata giorno+fascia | `AnalisiView.vue` |
| 20 | **PDF condiviso via Web Share API** — `generatePDFBlob()`, File allegato | `pdfReport.js`, `AnalisiView.vue` |
| 21 | **Dashboard medico interattiva** — KPI, classificazione, grafici Chart.js, filtro date, alert ESC/ESH | `SharedReportView.vue` |
| 22 | **Grafici con zone target ESC/ESH** — zona verde tratteggiata <140/90, hover tooltip con categoria | `AnalisiView.vue`, `SharedReportView.vue` |
| 23 | **Release script automatizzato** — `scripts/deploy.sh` (verifica .env → build → deploy con worktree) | `scripts/deploy.sh` |
| 24 | **Versione da package.json** — single source of truth, visibile in login e impostazioni | `vite.config.js`, `version.js`, `LoginView.vue`, `SettingsView.vue` |
| 25 | **Reset password via email** — link con token UUID, pagina `/reset-password`, RPC Supabase `app_request_password_reset` + `app_complete_password_recovery`, tabella `user_password_recovery_tokens` | `LoginView.vue`, `ResetPasswordView.vue`, `auth.js`, `supabaseTableAuth.js`, `router/index.js`, `supabase/migrations/005_email_password_recovery.sql` |

---

## 11. Feature Android

> App nativa Android (Kotlin + Jetpack Compose). Stesso backend Supabase, stesse utenze. Vedere `IPERTESO_ANDROID_PLAN.md` e `iperteso/DESIGN.md` per dettagli.

| Area | Feature | Note |
| --- | --- | --- |
| Auth | Login table-based (SHA-256) | Placeholder come la Vue, "Accesso in corso…" |
| Auth | Sessione 8h TTL | DataStore |
| Auth | Recovery password | Stub (form, RPC non cablato) |
| Auth | RBAC admin/user | Sezione Admin stub |
| Dashboard | Saluto orario + ultima lettura | Greeting time-based localizzato |
| Dashboard | 4 KPI + letture recenti + empty state | |
| CRUD | Add/Edit con validazione localizzata | Errori su `strings.xml` it/en |
| CRUD | Classificazione ESC/ESH live | 7 categorie 2024 (Ottimale→Crisi) |
| CRUD | Duplicati 10 min | |
| Lista | 4 filtri raggruppati + ricerca + swipe-to-delete | |
| Analisi | Tab Andamento/Variazioni/Distribuzione | |
| Analisi | Grafici Canvas nativi | Line, bar derivate (dP/dt), doughnut — **senza librerie esterne** |
| Analisi | Zona target 90-140 + soglia 140 | |
| Analisi | Morning Surge, Carico Ipertensivo, HRV | |
| Analisi | Filtri periodo 7/30/90 giorni | |
| Report | PDF iText7 | Header, stats, ESC/ESH, farmaci, tabella |
| Report | Link temporaneo 48h + PIN 4 cifre | Tabella Supabase `shared_reports` |
| Report | SharedReportScreen con PIN gate | |
| Report | Deep link `iperteso://share/{token}` | |
| Impostazioni | Lingua it/en con riavvio | `LocaleManager` + `values-en` |
| Impostazioni | **Farmaci (medications)** | CRUD completo, inclusi nel PDF |
| Impostazioni | Promemoria giornaliero | AlarmManager fisso 08:00 (no giorni) |
| Impostazioni | CSV export + elimina dati + info | |
| Impostazioni | Dark mode | Solo di sistema (`isSystemInDarkTheme()`), toggle non cablato |
| Infra | Offline-first Room → Supabase | SyncWorker 15 min |
| Infra | Koin DI + Navigation Compose | |
| Infra | i18n strings.xml it/en + valori | |

---

## 12. Matrice di parità Web ↔ Android

**Legenda:** ✅ parità · 🟡 parziale · ⬜ assente

| Funzione | Web | Android | Note |
| --- | --- | --- | --- |
| Login table-based + sessione 8h | ✅ | ✅ | |
| Recovery password | ✅ | 🟡 | Android stub |
| RBAC + gestione utenti admin | ✅ | 🟡 | Android: sezione Admin stub |
| CRUD letture + validazione | ✅ | ✅ | validazione localizzata su entrambe |
| Classificazione ESC/ESH | 🟡 | 🟡 | Vue 6 cat. (con Ipotensione) · Android 7 cat. 2024 |
| Dashboard (saluto/KPI/recenti/empty) | ✅ | ✅ | |
| Lista (filtri/ricerca/swipe) | ✅ | ✅ | |
| Grafici line/bar/doughnut | ✅ | ✅ | Vue Chart.js interattivo · Android Canvas statico |
| Zona target + soglia 140 | ✅ | ✅ | |
| Morning Surge / Carico / HRV | ✅ | ✅ | |
| Confronto 7/30 giorni | ✅ | 🟡 | Android solo filtri periodo |
| Fasce orarie configurabili | ✅ | ⬜ | Android fisse |
| PDF | ✅ | ✅ | jsPDF vs iText7 |
| Link 48h + PIN + revoca | ✅ | 🟡 | Android: crea link ma senza lista/revoca |
| PIN gate report condiviso | ✅ | ✅ | |
| Promemoria | ✅ | 🟡 | Web: giorni+orari multipli · Android: 1 giornaliero |
| Lingua it/en | ✅ | ✅ | Web reattivo · Android con riavvio |
| Farmaci (medications) | ✅ | ✅ | Portata su Web in questa sessione |
| CSV export | ✅ | ✅ | |
| CSV import / backup / dati test | ✅ | ⬜ | |
| Profilo esteso (anagrafica) | ✅ | ⬜ | |
| Cambio password/email | ✅ | 🟡 | Android stub |
| PWA install / SW update / offline banner | ✅ | ⬜ | N/A nativo |
| Deep link nativo | ⬜ | ✅ | `iperteso://share/{token}` |
| Notifiche native di sistema | 🟡 | ✅ | Web: Web Notifications · Android: native |

### Parità casi di test

| Suite | Web | Android |
| --- | --- | --- |
| Unit | 270 (Vitest) | 63 (JUnit4/MockK) |
| Strumentati (device) | — | 20 (Room DAO su emulatore) |
| E2E | 123 passed + 2 skipped (Playwright) | — (test manuale su emulatore) |
| Build | `vite build` ✅ | `assembleRelease` ✅ APK firmata |

**Gap copertura test (Android vs Web):** la logica statistica (derivate, morning surge, carico, HRV) è estratta in `statistics.js` su Web (29 unit test), mentre su Android è inline nelle schermate e **non ha unit test dedicati**. Per allineare i casi di test andrebbe estratta in un oggetto Kotlin testabile (`Statistics.kt`). Stessa cosa per CSV import/backup/fasce orarie, non presenti su Android.

---

*Documento unificato Web + Android — ultimo aggiornamento 2026-08-13*
