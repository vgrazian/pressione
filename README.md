# Pressione ❤️

PWA per il monitoraggio della pressione arteriosa e delle pulsazioni cardiache.
Multi-utente, con sincronizzazione cloud via Supabase.

## Funzionalità

- 📊 **Monitoraggio completo**: sistolica, diastolica, frequenza cardiaca
- 🏷️ **Classificazione automatica**: secondo linee guida ESC/ESH
- 📈 **Statistiche avanzate**: medie, trend, distribuzione categorie e oraria
- 📄 **Report**: generazione e condivisione report testuali
- 🔔 **Promemoria**: configurazione promemoria per misurazioni regolari
- 👥 **Multi-utente**: ogni utente ha i propri dati isolati
- 🔐 **Autenticazione**: login sicuro con password hashed (SHA-256)
- 📱 **PWA**: installabile su qualsiasi dispositivo (iOS, Android, desktop)
- ☁️ **Sync cloud**: dati sincronizzati su Supabase
- 🌐 **Offline-first**: funziona anche senza connessione

## Tecnologie

- **Frontend**: Vue 3 + Vite + Vue Router
- **Backend**: Supabase (PostgreSQL)
- **Database locale**: Dexie (IndexedDB)
- **Test**: Vitest + Playwright
- **PWA**: Vite PWA Plugin

## Setup

### Prerequisiti

- Node.js 18+
- Account Supabase (free tier)

### Installazione

```bash
# Clona il repository
git clone https://github.com/vgrazian/pressione.git
cd pressione

# Installa le dipendenze
npm install

# Configura le variabili d'ambiente
cp .env.example .env
# Modifica .env con le tue credenziali Supabase

# Avvia in sviluppo
npm run dev
```

### Configurazione Supabase

1. Crea un progetto Supabase
2. Esegui la migration in `supabase/migrations/001_initial_schema.sql`
3. Crea gli utenti seed:

```bash
SUPABASE_URL=... SUPABASE_SECRET_KEY=... node scripts/provision-users.mjs
```

## Utenti Predefiniti

| Username | Password | Ruolo |
| ---------- | ---------- | ------- |
| nadia | Pressione2026! | admin |
| roberto | Pressione2026! | user |
| barbara | Pressione2026! | user |
| valerio | Pressione2026! | admin |
| marco | Pressione2026! | user |
| rita | Pressione2026! | user |
| anna | Pressione2026! | user |

⚠️ **Cambia le password al primo accesso!**

## Comandi

| Comando | Descrizione |
| --------- | ------------- |
| `npm run dev` | Avvia server sviluppo |
| `npm run build` | Build di produzione |
| `npm run preview` | Anteprima build |
| `npm test` | Esegui test unitari |
| `npm run test:e2e` | Esegui test E2E |
| `npm run seed:users` | Crea utenti seed |

## Struttura Progetto

```
pressione/
├── src/
│   ├── main.js              # Entry point
│   ├── App.vue              # Root component
│   ├── style.css            # Design system
│   ├── router/              # Vue Router
│   ├── db/                  # Dexie IndexedDB
│   ├── services/            # Business logic
│   │   ├── auth.js          # Auth state + session
│   │   ├── supabaseTableAuth.js  # Supabase auth CRUD
│   │   ├── supabaseClient.js     # Supabase client
│   │   ├── dataService.js        # CRUD + sync
│   │   ├── categories.js         # BP classification
│   │   ├── statistics.js         # Statistics math
│   │   ├── rbac.js               # Role-based access
│   │   └── errorHandling.js      # Error utilities
│   ├── components/          # Reusable components
│   └── views/               # Page components
├── tests/
│   ├── unit/                # Vitest unit tests
│   └── e2e/                 # Playwright E2E tests
├── supabase/
│   └── migrations/          # SQL migrations
└── scripts/                 # Utility scripts
```

## Licenza

MIT
