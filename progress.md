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

1. Modifiche al codice committate e pushate su `main`
2. **Se ci sono migrazioni DB**: applicate via Supabase MCP (`apply_migration`) prima o insieme al push
3. **Se la versione cambia**: aggiornare `package.json` → `version`
4. **Build e deploy su GitHub Pages**:

   ```bash
   npm run build
   cp -r dist /tmp/pressione-dist
   git checkout gh-pages
   find . -maxdepth 1 -not -name '.git' -not -name '.' -not -name '..' -exec rm -rf {} \;
   cp -r /tmp/pressione-dist/* .
   git add -A && git commit -m "deploy: v$(node -p "require('./package.json').version")"
   git push origin gh-pages
   git checkout main
   rm -rf /tmp/pressione-dist
   ```

5. Attendere ~1-2 min per propagazione CDN (GitHub Pages)
6. Verificare con hard refresh (`Cmd+Shift+R`) o "Forza aggiornamento" in Impostazioni

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

### 2026-08-03

- Sostituito `age` (statico) con `birthDate` (data di nascita) per calcolo dinamico dell'età
- Fix: `refreshSession()` ora persiste `profileCompleted`/`skipProfilePrompt` → niente più prompt ripetuto
- Fix: `initAuth()` ora attende `getProfile()` prima di `isAuthReady`
- Aggiunto pulsante "Forza aggiornamento" in Impostazioni (svuota SW cache + ricarica)
- Keep-alive database attivo di default per tutti gli utenti
- Versione derivata da `package.json`, layout login fixato (`flex-direction: column`)
- Migrazione 003 applicata via Supabase MCP
