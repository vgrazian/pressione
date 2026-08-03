# Pressione — Feature Inventory & Review

> **Versione:** 1.1.0 | **Data:** 2026-08-03  
> **URL:** <https://vgrazian.github.io/pressione/>  
> **Stack:** Vue 3 + Vite PWA · Supabase · Dexie/IndexedDB · Chart.js · chartjs-plugin-annotation · jsPDF  
> **Test:** 29 unit (Vitest) + 15 E2E (Playwright)

---

## 1. Auth & User Management

| Feature | Note |
| --- | --- |
| Login/Logout | Table-based auth, SHA-256 |
| Sessione TTL 8h | localStorage + IndexedDB fallback |
| Recupero password | Token-based flow su login |
| Cambio password | Da Impostazioni (richiede pw attuale) |
| Modifica email | Da Impostazioni → Account |
| RBAC admin/user | Admin: gestione utenti, reset pw |
| Topbar logout | Icona in alto a destra su ogni pagina |
| 8 utenti seed | nadia, valerio (admin) + 6 user + bot |
| **Profilo utente** | Data di nascita + genere, età calcolata dinamicamente |
| **Prompt profilo** | Mostrato solo se incompleto, mai più se skippato/salvato |

## 2. Dashboard (Home)

| Feature | Note |
| --- | --- |
| Saluto + ultima lettura | Card con categoria e valori |
| 4 KPI rapidi | Media SYS/DIA/BPM + conteggio |
| 5 letture recenti | Card compact |
| Empty state + CTA | |
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

## 5. Statistiche Avanzate

| Feature | Note |
| --- | --- |
| Filtri: 7g, 30g, custom | Date picker |
| **Line chart (Chart.js)** | SYS (rosso), DIA (blu), BPM (grigio tratteggiato) |
| **Fascia target ESC/ESH** | Zona verde tratteggiata 90-140 mmHg con label |
| **Linea soglia 140** | Tratteggiata rossa nel grafico BP |
| **Hover tooltip interattivi** | Data/ora + valore + categoria ESC/ESH |
| **Grafico derivate dP/dt** | mmHg/ora, smoothing media mobile 3pt |
| **Allarme >10 mmHg/h** | Barre rosse + lista segmenti critici |
| **Morning Surge** | Δ fasce configurabili, badge ⚠️ |
| **Carico Ipertensivo** | % fuori norma + barra progresso |
| **HRV** | Deviazione standard BPM |
| **Pie chart OMS cliccabile** | 4 categorie, click → filtra lista |
| **Trend lines** | Lineare + Media mobile |
| Auto-aggregazione | Per >50 punti |
| Skeleton loader | |
| **Fasce orarie configurabili** | Mattina/Pomeriggio/Sera/Notte con orari personalizzabili | |

## 6. Report e Condivisione

| Feature | Note |
| --- | --- |
| Filtri contenuto | Periodo, storico, anonimizza |
| **Grafico interattivo inline** | Chart.js nel ReportView con zone target ESC/ESH |
| **PDF (jsPDF)** | A4 con header, stats, tabella, grafici incorporati |
| **PDF come Blob** | `generatePDFBlob()` per condivisione file |
| **Condivisione con PDF allegato** | Email, WhatsApp, Condividi via Web Share API con file |
| **Link temporaneo 48h** | Token + PIN 4 cifre opzionale |
| **Revoca link** | Lista attivi + bottone revoca |
| **PIN gate** | SharedReportView con schermata PIN |
| **Dashboard medico interattiva** | SharedReportView: KPI, classificazione, alert, grafici interattivi |
| **Grafico BP nel report** | Con zona target, hover tooltip, filtro 7/30/tutto |
| **Card fasce orarie** | Media per fascia nel report condiviso |
| **Grafico derivate + doughnut** | Nel report condiviso |
| **Vista per fascia oraria** | Toggle Lista/Per fascia con tabella raggruppata giorno+fascia |

## 7. Impostazioni

| Feature | Note |
| --- | --- |
| Lingua IT/EN | Selettore in cima |
| Account | Username, email, ruolo |
| **Profilo** | Data di nascita con età calcolata + genere |
| Modifica email/password | |
| Promemoria | Multipli, orari + giorni |
| **Fasce orarie configurabili** | Mattina/Pomeriggio/Sera/Notte, orari personalizzabili per fascia |
| CSV Export | |
| Genera dati test | 30 letture via RPC Supabase |
| Gestione utenti (admin) | Ruolo, reset pw, disattiva |
| **Cache & Aggiornamenti** | Pulsante "Forza aggiornamento" (svuota SW cache) |
| **Keep-Alive DB** | Attivo di default, ping Supabase ogni 5 min + storage persistente |
| Elimina tutto | Doppia conferma |

## 8. UI/UX

| Feature | Note |
| --- | --- |
| Design system CSS | Variabili, radius 12px |
| Dark mode | `prefers-color-scheme` |
| Font Inter | Google Fonts, swap |
| Icone SVG | AppIcon (18 icone) |
| Skeleton loader | Componente riutilizzabile |
| Confirm dialog | Globale via provide/inject |
| Top bar + bottom nav | Sticky, 4 tab |
| Offline banner | Giallo se no Supabase |
| Focus-visible, reduced-motion | A11y |
| PWA | Installabile, SW, manifest |

## 9. Infrastruttura

| Feature | Note |
| --- | --- |
| Retry backoff | 2 tentativi esponenziali |
| Offline-first | Dexie sempre disponibile |
| Sync queue | Coda operazioni offline |
| RLS policies | Tutte le tabelle |
| SHA-256 hashing | Client + server |
| GDPR link TTL | 48 ore auto-scadenza |
| Stats cache | IndexedDB pronto |
| **Release script** | `scripts/deploy.sh` — verifica .env, build, deploy gh-pages |
| **Version from package.json** | Single source of truth, build number da git hash |
| **Force cache clear** | `forceClearCache()` — deregistra SW, svuota caches, reload |
| **44 test** | 29 unit + 15 E2E |

## 10. Nuove Feature (Agosto 2026) — Portabili su BP-Tracker

| # | Feature | File(s) |
| --- | --- | --- |
| 1 | **Data di nascita invece di età** — calcolo dinamico con `computeAge()` | `ProfilePrompt.vue`, `SettingsView.vue`, `auth.js`, `supabaseTableAuth.js`, `pdfReport.js` |
| 2 | **Prompt profilo non ripetitivo** — `refreshSession()` persiste flag, `initAuth()` await | `auth.js`, `App.vue` |
| 3 | **Forza aggiornamento cache** — deregistra SW + svuota caches + reload | `swUpdate.js`, `SettingsView.vue` |
| 4 | **Keep-alive DB default ON** — ping Supabase 5min + persistent storage | `keepAlive.js` |
| 5 | **Fasce orarie configurabili** — servizio `timeBands.js`, UI in Impostazioni, consumer in stats/report | `timeBands.js`, `SettingsView.vue`, `statistics.js`, `TimeOfDayIcon.vue`, `ReportView.vue` |
| 6 | **Vista report per fascia oraria** — toggle Lista/Per fascia, tabella raggruppata giorno+fascia | `ReportView.vue` |
| 7 | **PDF condiviso via Web Share API** — `generatePDFBlob()`, File allegato | `pdfReport.js`, `ReportView.vue` |
| 8 | **Dashboard medico interattiva** — KPI, classificazione, grafici Chart.js, filtro date, alert ESC/ESH | `SharedReportView.vue` |
| 9 | **Grafici con zone target ESC/ESH** — zona verde tratteggiata <140/90, hover tooltip con categoria | `StatisticsView.vue`, `ReportView.vue`, `SharedReportView.vue` |
| 10 | **Release script automatizzato** — `scripts/deploy.sh` (verifica .env → build → deploy) | `scripts/deploy.sh` |
| 11 | **Versione da package.json** — single source of truth, visibile in login e impostazioni | `vite.config.js`, `version.js`, `LoginView.vue`, `SettingsView.vue` |

---

*Documento generato per revisione — ultimo aggiornamento 2026-08-02*
