# Pressione — Feature Inventory & Review

> **Versione:** 1.2.1 | **Data:** 2026-08-07  
> **URL:** <https://vgrazian.github.io/pressione/>  
> **Stack:** Vue 3 + Vite PWA · Supabase · Dexie/IndexedDB (+ localStorage bridge) · Chart.js (theme-aware) · chartjs-plugin-annotation · jsPDF  
> **Test:** 241 unit (Vitest) + 60 E2E (Playwright)

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
| Lingua IT/EN | Selettore in cima |
| Account | Username, email, ruolo |
| Modifica email/password | Password in sezione collassabile |
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
| Confirm dialog | Globale via provide/inject |
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
| **81 test** | 81 unit + 60 E2E |

## 10. Nuove Feature (Agosto 2026) — Portabili su BP-Tracker

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

*Documento generato per revisione — ultimo aggiornamento 2026-08-06*
