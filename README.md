# Pressione

PWA per il monitoraggio della pressione arteriosa e delle pulsazioni cardiache.
Multi-utente, con sincronizzazione cloud via Supabase.

**URL:** https://vgrazian.github.io/pressione/

## Funzionalità

### Autenticazione e Utenti
- Login/Logout con password hashed (SHA-256), sessione persistente 8 ore
- Recupero password via token, modifica email
- RBAC: admin (gestione utenti, reset password) e user
- 8 utenti pre-configurati

### Monitoraggio
- CRUD misurazioni: sistolica, diastolica, frequenza cardiaca, data/ora, note
- Classificazione automatica ESC/ESH in tempo reale (6 categorie)
- Rilevazione duplicati (stessa misurazione entro 10 minuti)
- Validazione input con range clinici

### Dashboard
- Ultima misurazione con categoria e valori
- 4 KPI rapidi: media SYS/DIA/BPM + conteggio
- Ultime 5 letture recenti
- Empty state con CTA

### Lista Misurazioni
- Lista cronologica con filtri per categoria e ricerca testuale
- Swipe-to-delete su mobile
- Icona momento giornata (mattina/pomeriggio/sera/notte)

### Statistiche Avanzate
- **Line chart (Chart.js)**: Sistolica (rossa) + Diastolica (blu) + BPM (grigia)
- **Fascia sicurezza OMS**: rettangoli sfumati (SYS 90-140, DIA 60-90)
- **Grafico derivate dP/dt**: variazione mmHg/ora, allarme a >10 mmHg/h
- **Morning Surge**: Δ mattina (06-09) vs sera (20-23) con badge allarme
- **Carico Ipertensivo**: % letture fuori norma con barra progresso
- **HRV**: deviazione standard frequenza cardiaca
- **Pie chart OMS**: 4 categorie cliccabili (filtrano la lista)
- **Trend algorithm**: lineare e media mobile
- Filtri temporali: 7/30 giorni + personalizzato

### Report e Condivisione
- **PDF (jsPDF)**: A4 con header, statistiche, tabella completa
- **Condivisione**: Email, WhatsApp, Web Share API nativa
- **Link temporaneo (48h)**: URL condivisibile con PIN opzionale 4 cifre
- **Revoca link**: lista link attivi con bottone revoca immediata
- **Pagina medico**: SharedReportView pubblica con PIN gate
- Filtri contenuto: includi storico, anonimizza

### Impostazioni
- Lingua IT/EN
- Modifica email e password
- Promemoria configurabili (orari + giorni settimana)
- CSV Export
- Genera dati test (30 letture casuali)
- Gestione utenti (admin): cambio ruolo, reset password, disattivazione

### UI/UX
- Design system con dark mode automatica
- Font Inter, icone SVG, radius 12px consistente
- Skeleton loader in tutte le viste
- Offline banner quando Supabase non raggiungibile
- PWA installabile (service worker, manifest)
- Top bar con logout, bottom navigation 4 tab

### Affidabilità
- Offline-first: IndexedDB (Dexie) sempre disponibile
- Retry automatico con backoff esponenziale
- Sync queue per operazioni offline
- 44 test (29 unit Vitest + 15 E2E Playwright)

## Tecnologie

| Layer | Tecnologia |
|---|---|
| Frontend | Vue 3 + Vite + Vue Router |
| Grafici | Chart.js + chartjs-plugin-annotation |
| PDF | jsPDF |
| Backend | Supabase (PostgreSQL + RLS + RPC) |
| Database locale | Dexie (IndexedDB) |
| Test | Vitest (29) + Playwright (15) |
| PWA | Vite PWA Plugin |
| UI | Inter font, CSS custom properties |

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
├── db/              # Dexie IndexedDB schema
├── services/
│   ├── auth.js              # Auth state + session
│   ├── supabaseTableAuth.js # Supabase auth CRUD
│   ├── supabaseClient.js    # Supabase client
│   ├── dataService.js       # CRUD + sync + retry
│   ├── categories.js        # Classificazione ESC/ESH
│   ├── statistics.js        # Stats, derivate, HRV, morning surge
│   ├── i18n.js              # IT/EN translations
│   ├── rbac.js, ids.js, errorHandling.js
├── components/      # AppNav, AppIcon, CategoryBadge, ReadingCard, etc.
├── views/           # Home, Login, AddEdit, List, Stats, Report, Settings, Operators, SharedReport
tests/
├── unit/            # 29 Vitest tests
├── e2e/             # 15 Playwright tests
supabase/migrations/ # SQL schema
```

## Licenza

MIT
