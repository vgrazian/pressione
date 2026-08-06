# Pressione

PWA per il monitoraggio della pressione arteriosa e delle pulsazioni cardiache.
Multi-utente, offline-first, con sincronizzazione cloud via Supabase.

**URL:** <https://vgrazian.github.io/pressione/>

## Funzionalità

### Autenticazione e Utenti

- Login/Logout con password hashed (SHA-256), sessione persistente 8 ore
- **Recupero password via email**: link con token (30 min) → pagina reset dedicata (`/reset-password?token=...`)
- **Strumenti versione in login**: pulsanti 📋 Copia (debug) e 🔄 Aggiorna (force clear cache + SW reload)
- RBAC: admin (gestione utenti, reset password) e user
- 9 utenti pre-configurati (2 admin, 7 user)

### Navigazione

- **Top bar**: logo, toggle tema (☀️/🌙/sistema), logout
- **Bottom nav 4 tab**: Home, Lista, Analisi, Altro
- **FAB globale rotonda**: pulsante "+" flottante su tutte le pagine per aggiungere una misurazione

### Monitoraggio

- CRUD misurazioni: sistolica, diastolica, frequenza cardiaca, data/ora, note
- Classificazione automatica ESC/ESH in tempo reale (6 categorie)
- Rilevazione duplicati (stessa misurazione entro 10 minuti)
- Validazione input con range clinici

### Dashboard (Home)

- Ultima misurazione con categoria e valori
- 4 KPI rapidi: media SYS/DIA/BPM + conteggio
- Trend settimanale con derivata dP/dt e conteggio allarmi
- Banner stato sincronizzazione con pulsante riprova
- Empty state con guida 3-step per il primo utilizzo

### Lista Misurazioni

- Lista cronologica con filtri per **periodo** (Tutte/7gg/30gg), categoria e ricerca testuale
- Swipe-to-delete su mobile
- Icona momento giornata (mattina/pomeriggio/sera/notte)

### Analisi (Statistiche + Report unificati)

- **Chart a tab**: Andamento (linee SYS/DIA/BPM) | Variazioni (barre dP/dt) | Distribuzione (doughnut OMS)
- Colori theme-aware via CSS tokens — si adattano automaticamente a light/dark mode
- **Fascia target ESC/ESH**: zona verde tratteggiata con label
- **Linea soglia 140 mmHg**: tratteggiata rossa
- **Hover tooltip interattivi**: data/ora + valore + categoria ESC/ESH
- **Confronto 7/30 giorni**: tabella multi-periodo (letture, medie, variazioni, allarmi, carico ipertensivo, picco mattutino)
- **Morning Surge**: Δ fasce configurabili con badge allarme
- **Carico Ipertensivo**: % letture fuori norma
- **HRV**: deviazione standard frequenza cardiaca
- **Fasce orarie**: 4 card con media per fascia (Mattina/Pomeriggio/Sera/Notte)
- Filtri temporali: 7/30 giorni + personalizzato
- **Storico** con toggle Lista / Per fascia oraria, tabella raggruppata giorno+fascia

### Report e Condivisione

- **PDF (jsPDF)**: A4 con header, statistiche, tabella completa
- **Condivisione**: Email, WhatsApp, Web Share API nativa, copia appunti
- **Link temporaneo (48h)**: URL condivisibile con PIN opzionale 4 cifre (SHA-256)
- **Scadenza visibile**: data e ora esatte sul link e nella lista link attivi
- **Revoca link**: lista link attivi con bottone revoca immediata
- **Pagina medico**: SharedReportView pubblica con PIN gate e dashboard interattiva
- Filtri contenuto: periodo, includi grafici, anonimizza

### Impostazioni

- **Lingua**: Italiano / English
- **Account**: username, email, ruolo, modifica email, cambio password
- **Profilo**: data di nascita con età calcolata, genere, anagrafica completa (nome, cognome, CF, telefono, indirizzo)
- **Tema**: chiaro / scuro / sistema (con toggle nella top bar)
- **Sezioni raggruppate**: Password, Promemoria, Fasce Orarie, Dati, Keep-Alive, PWA Install, Cache — tutte collassabili
- **Promemoria**: orari + giorni settimana configurabili
- **Keep-Alive DB**: ping periodici Supabase + richiesta archiviazione persistente IndexedDB
- **Installa App**: pulsante nativo Android/Chrome + istruzioni passo-passo per iOS Safari
- **Dati**: CSV Export, Backup/Ripristino JSON, Import CSV (bp-tracker), genera dati test (30 letture)
- **Admin**: gestione utenti (ruolo, reset password, disattivazione)
- **Danger Zone**: elimina tutti i dati

### UI/UX

- Design system con **dark mode** (data-theme + prefers-color-scheme)
- Font Inter, icone SVG inline (18 icone), radius 12px consistente
- **Micro-interazioni**: hover shadow, active scale su card e pulsanti
- **Transizioni view**: fade animato tra pagine
- Skeleton loader in tutte le viste
- Offline banner quando Supabase non raggiungibile
- **PWA installabile**: service worker, manifest, meta tag iOS (standalone, status bar)
- Checkbox personalizzate, toggle switch animati
- Reduced motion support
- **Dark mode badge**: contrasto migliorato per categorie su sfondo scuro

### Affidabilità

- **Offline-first**: IndexedDB (Dexie) sempre disponibile, sync queue
- **LocalStorage bridge**: backup letture per compatibilità iOS PWA (IndexedDB isolato tra Safari e standalone)
- Retry automatico con backoff esponenziale
- Keep-alive database: ping Supabase ogni 5 min + navigator.storage.persist()
- Gestione errori resiliente (try/catch su tutte le operazioni)
- 81 test (81 unit Vitest + 15 E2E Playwright)

## Tecnologie

| Layer | Tecnologia |
| --- | --- |
| Frontend | Vue 3 + Vite + Vue Router (hash history) |
| Grafici | Chart.js 4 + chartjs-plugin-annotation (colori theme-aware) |
| PDF | jsPDF (A4) |
| Backend | Supabase (PostgreSQL + REST API) |
| Database locale | Dexie (IndexedDB v1) + localStorage bridge |
| Test | Vitest (81) + Playwright (15) |
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
| --- | --- |
| `npm run dev` | Server sviluppo |
| `npm run build` | Build produzione |
| `npm test` | Test unitari (Vitest) |
| `npm run test:e2e` | Test E2E (Playwright) |
| `npm run seed:users` | Crea utenti seed |
| `bash scripts/deploy.sh` | Build + deploy su GitHub Pages (worktree isolato) |

## Utenti

| Username | Password | Ruolo |
| --- | --- | --- |
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
│   ├── dataService.js       # CRUD offline-first + sync + retry + CSV/backup + localStorage bridge
│   ├── categories.js        # Classificazione ESC/ESH (6 categorie)
│   ├── statistics.js        # Stats, derivate, morning surge, HRV, carico ipertensivo
│   ├── chartColors.js       # Colori chart theme-aware via CSS tokens
│   ├── i18n.js              # IT/EN translations
│   ├── theme.js             # Light/dark/system toggle
│   ├── keepAlive.js         # Ping Supabase + navigator.storage.persist()
│   ├── pwaInstall.js        # beforeinstallprompt + iOS detection
│   ├── timeBands.js         # Fasce orarie configurabili
│   ├── rbac.js, ids.js, errorHandling.js, swUpdate.js, version.js
├── components/      # AppNav, AppIcon (18 SVG), ReadingCard, SkeletonLoader, CollapsibleSection, etc.
├── views/           # Home, Login, AddEdit, List, Analisi (Stats+Report unificati), Settings, Operators, SharedReport
tests/
├── unit/            # 81 Vitest tests
├── e2e/             # 15 Playwright tests
supabase/migrations/ # SQL schema
```

## Licenza

MIT
