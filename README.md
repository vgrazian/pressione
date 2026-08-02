# Pressione

PWA per il monitoraggio della pressione arteriosa e delle pulsazioni cardiache.
Multi-utente, offline-first, con sincronizzazione cloud via Supabase.

**URL:** https://vgrazian.github.io/pressione/

## Funzionalità

### Autenticazione e Utenti
- Login/Logout con password hashed (SHA-256), sessione persistente 8 ore
- Recupero password via token, modifica email
- RBAC: admin (gestione utenti, reset password) e user
- 8 utenti pre-configurati

### Navigazione
- **Top bar**: logo, toggle tema (☀️/🌙/sistema), logout
- **Bottom nav 5 tab**: Home, Lista, Statistiche, Report, Impostazioni
- **FAB globale**: pulsante "+" flottante su tutte le pagine per aggiungere una misurazione

### Monitoraggio
- CRUD misurazioni: sistolica, diastolica, frequenza cardiaca, data/ora, note
- Classificazione automatica ESC/ESH in tempo reale (6 categorie)
- Rilevazione duplicati (stessa misurazione entro 10 minuti)
- Validazione input con range clinici

### Dashboard (Home)
- Ultima misurazione con categoria e valori
- 4 KPI rapidi: media SYS/DIA/BPM + conteggio
- Trend settimanale con derivata dP/dt e conteggio allarmi
- Empty state con CTA

### Lista Misurazioni
- Lista cronologica con filtri per **periodo** (Tutte/7gg/30gg), categoria e ricerca testuale
- Swipe-to-delete su mobile
- Icona momento giornata (mattina/pomeriggio/sera/notte)

### Statistiche Avanzate
- **Line chart (Chart.js)**: Sistolica (rossa) + Diastolica (blu) + BPM (grigia)
- **Fascia sicurezza OMS**: rettangoli sfumati (SYS 90-140, DIA 60-90)
- **Grafico derivate dP/dt**: variazione mmHg/ora, allarme a >10 mmHg/h con lista segmenti
- **Morning Surge**: Δ mattina (06-09) vs sera (20-23) con badge allarme
- **Carico Ipertensivo**: % letture fuori norma con barra progresso
- **HRV**: deviazione standard frequenza cardiaca
- **Pie chart OMS**: 4 categorie cliccabili (filtrano la lista)
- **Trend algorithm**: regressione lineare e media mobile a 3 punti
- Filtri temporali: 7/30 giorni + personalizzato

### Report e Condivisione
- **PDF (jsPDF)**: A4 con header, statistiche, tabella completa, footer
- **Condivisione**: Email, WhatsApp, Web Share API nativa, copia appunti
- **Link temporaneo (48h)**: URL condivisibile con PIN opzionale 4 cifre (SHA-256)
- **Scadenza visibile**: data e ora esatte di scadenza sul link generato e nella lista link attivi
- **Revoca link**: lista link attivi con bottone revoca immediata
- **Pagina medico**: SharedReportView pubblica con PIN gate e tabella responsive
- Filtri contenuto: periodo, includi storico, includi grafici, anonimizza

### Impostazioni
- **Lingua**: Italiano / English
- **Account**: modifica email, cambio password
- **Tema**: chiaro / scuro / sistema (con toggle nella top bar)
- **Promemoria**: orari + giorni settimana configurabili
- **Keep-Alive DB**: ping periodici Supabase + richiesta archiviazione persistente IndexedDB (evita eliminazione dati dal browser)
- **Installa App**: pulsante nativo Android/Chrome + istruzioni passo-passo per iOS Safari
- **Dati**: CSV Export, Backup/Ripristino JSON, genera dati test (30 letture)
- **Admin**: gestione utenti (ruolo, reset password, disattivazione)
- **Danger Zone**: elimina tutti i dati

### UI/UX
- Design system con **dark mode** (data-theme + prefers-color-scheme)
- Font Inter, icone SVG inline (18 icone), radius 12px consistente
- Skeleton loader in tutte le viste
- Offline banner quando Supabase non raggiungibile
- **PWA installabile**: service worker, manifest, meta tag iOS (standalone, status bar)
- Checkbox personalizzate, toggle switch animati
- Reduced motion support

### Affidabilità
- **Offline-first**: IndexedDB (Dexie) sempre disponibile, sync queue
- Retry automatico con backoff esponenziale
- Keep-alive database: ping Supabase ogni 5 min + navigator.storage.persist()
- Gestione errori resiliente (try/catch su tutte le operazioni)
- 44 test (29 unit Vitest + 15 E2E Playwright)

## Tecnologie

| Layer | Tecnologia |
|---|---|
| Frontend | Vue 3 + Vite + Vue Router (hash history) |
| Grafici | Chart.js 4 + chartjs-plugin-annotation |
| PDF | jsPDF (A4) |
| Backend | Supabase (PostgreSQL + REST API) |
| Database locale | Dexie (IndexedDB v1) |
| Test | Vitest (29) + Playwright (15) |
| PWA | Vite PWA Plugin (generateSW, autoUpdate) |
| UI | Inter font, CSS custom properties, 18 SVG icons |

## Setup

```bash
git clone https://github.com/vgrazian/pressione.git
cd pressione
npm install
cp .env.example .env  # configura credenziali Supabase
npm run dev            # http://localhost:5173
```

## Comandi

| Comando | Descrizione |
|---|---|
| `npm run dev` | Server sviluppo |
| `npm run build` | Build produzione |
| `npm test` | Test unitari (Vitest) |
| `npm run test:e2e` | Test E2E (Playwright) |
| `npm run seed:users` | Crea utenti seed |

## Utenti

| Username | Password | Ruolo |
|---|---|---|
| nadia | Pressione2026! | admin |
| valerio | Pressione2026! | admin |
| roberto | Pressione2026! | user |
| barbara | Pressione2026! | user |
| marco | Pressione2026! | user |
| rita | Pressione2026! | user |
| anna | Pressione2026! | user |
| bot | test1234 | user (test) |

⚠️ Cambia la password al primo accesso.

## Struttura

```
src/
├── main.js, App.vue, style.css
├── router/          # Vue Router (hash history)
├── db/              # Dexie IndexedDB schema + helpers
├── services/
│   ├── auth.js              # Auth state reattivo + sessione localStorage
│   ├── supabaseTableAuth.js # Auth CRUD su tabella users
│   ├── supabaseClient.js    # Client Supabase
│   ├── dataService.js       # CRUD offline-first + sync + retry + CSV/backup
│   ├── categories.js        # Classificazione ESC/ESH (6 categorie)
│   ├── statistics.js        # Stats, derivate, morning surge, HRV, carico ipertensivo
│   ├── i18n.js              # IT/EN translations
│   ├── theme.js             # Light/dark/system toggle
│   ├── keepAlive.js         # Ping Supabase + navigator.storage.persist()
│   ├── pwaInstall.js        # beforeinstallprompt + iOS detection
│   ├── rbac.js, ids.js, errorHandling.js
├── components/      # AppNav, AppIcon (18 SVG), ReadingCard, SkeletonLoader, etc.
├── views/           # Home, Login, AddEdit, List, Stats, Report, Settings, Operators, SharedReport
tests/
├── unit/            # 29 Vitest tests
├── e2e/             # 15 Playwright tests
supabase/migrations/ # SQL schema
```

## Licenza

MIT
