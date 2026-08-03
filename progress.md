# Pressione — Progress & Conventions

> Ultimo aggiornamento: 2026-08-03

---

## Version Management

La versione dell'app è derivata automaticamente da **`package.json`** (campo `version`) durante la build Vite:

```
vite.config.js  →  legge package.json  →  __APP_VERSION__
```

**Per aggiornare la versione:**

1. Modifica solo `"version"` in `package.json` (es. `"1.1.0"` → `"1.2.0"`)
2. Tutti i punti di visualizzazione (login, impostazioni) riflettono automaticamente il nuovo valore
3. Il build number (`__BUILD_NUMBER__`) è l'hash git breve, generato automaticamente

**Dove appare la versione:**

- **Login**: `v1.1.0 — build abc1234 — 03/08/2026, 12:00:00` (sotto il pannello, `flex-direction: column`)
- **Impostazioni**: `Pressione v1.1.0 — build abc1234 — 03/08/2026, 12:00:00` (in fondo alla pagina)
- **i18n**: il campo `version` in `i18n.js` contiene solo `'Pressione'` (senza numero, il numero viene da `APP_VERSION`)

---

## Release Checklist

**⚠️ Usa sempre `bash scripts/deploy.sh` per pubblicare.** Un comando solo:

```bash
bash scripts/deploy.sh "messaggio opzionale"
```

Lo script fa: verifica `.env` → `npm install` → build → verifica Supabase nel bundle → deploy `gh-pages` → pulizia.

**Requisiti prima del deploy:**

1. `.env` presente con `VITE_SUPABASE_URL` e `VITE_SUPABASE_PUBLISHABLE_KEY` reali
2. Se la versione cambia: aggiornare `package.json` → `version`
3. Migrazioni DB applicate via Supabase MCP (`apply_migration`) se presenti
4. Attendere ~1-2 min per propagazione CDN, poi hard refresh (`Cmd+Shift+R`) o "Forza aggiornamento"

---

## Migrations DB

I file SQL in `supabase/migrations/` vanno applicati tramite Supabase MCP:

| # | File | Descrizione | Stato |
| --- | --- | --- | --- |
| 001 | `001_initial_schema.sql` | Schema iniziale | ✅ |
| 002 | `002_profile_fields.sql` | Campi profilo (age, gender) | ✅ |
| 003 | `003_birth_date.sql` | Sostituisce age con birth_date | ✅ |

---

## Sessioni Recenti

### 2026-08-03 (sessione completa)

**Profilo utente:**

- Sostituito `age` (statico) con `birthDate` (data di nascita) per calcolo dinamico dell'età
- `computeAge()` nei componenti ProfilePrompt, SettingsView, pdfReport
- Fix: `refreshSession()` ora persiste `profileCompleted`/`skipProfilePrompt` → niente più prompt ripetuto
- Fix: `initAuth()` ora attende `getProfile()` prima di `isAuthReady`
- Fix watch App.vue con guard `isAuthReady`

**Cache & Aggiornamenti:**

- Pulsante "Forza aggiornamento" in Impostazioni (deregistra SW + svuota cache + reload)
- Keep-alive database attivo di default per tutti gli utenti

**Release & Versioning:**

- Versione derivata da `package.json` (single source of truth)
- Build number da git hash, visibile in login e impostazioni
- Layout login fixato (`flex-direction: column`)
- `scripts/deploy.sh` — script di release automatico (verifica .env, build, deploy gh-pages)
- `.gitignore` ripulito

**Migrazioni DB:**

- `003_birth_date.sql` applicata via Supabase MCP (aggiunge `birth_date`, rimuove `age`)

**Condivisione PDF:**

- PDF ora allegato via Web Share API (non più solo testo mailto/wa.me)
- `generatePDFBlob()` in pdfReport.js per generare File da condividere
- Pulsanti Email/WhatsApp/Condividi mostrano "Preparo PDF..." durante generazione

**Fasce orarie configurabili:**

- Nuovo servizio `src/services/timeBands.js` (default, get/save per-user, band-for-hour, groupByDayAndBand)
- Impostazioni → ⏰ Fasce Orarie: 4 input numerici per fascia, salva/ripristina
- `statistics.js`, `TimeOfDayIcon.vue`, `ReportView.vue` usano fasce configurate

**Report interattivo:**

- ReportView: toggle Lista/Per fascia con tabella raggruppata giorno+fascia
- ReportView: grafico interattivo Chart.js inline con zone target ESC/ESH
- SharedReportView completamente riscritto: dashboard clinico con KPI, grafici, alert, filtro date
- Grafico BP con zona target verde tratteggiata, hover tooltip con categoria
- Grafico derivate (dP/dt bar chart), doughnut distribuzione, card fasce orarie
- Filtro date (Tutto/30gg/7gg) nel report condiviso
