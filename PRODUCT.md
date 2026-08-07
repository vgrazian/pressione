# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Pazienti che si automonitorano la pressione arteriosa a casa, registrando misurazioni quotidiane (sistolica, diastolica, frequenza cardiaca, note). L'operatore tipo è una persona che convive con ipertensione e ha bisogno di tenere traccia delle letture nel tempo per condividerle col proprio medico.

L'app supporta anche un ruolo **admin** per la gestione di più utenti (creazione, disattivazione, eliminazione, reset password, cambio email), pensato per un uso ristretto tra amici e familiari.

## Product Purpose

Un diario digitale della pressione arteriosa che sostituisce il taccuino cartaceo. Permette di registrare misurazioni, visualizzare l'andamento nel tempo, analizzare le statistiche cliniche rilevanti (surge mattutino, carico ipertensivo, HRV) e generare **report PDF professionali** da condividere col medico.

## Positioning

Pressione combina la semplicità di un diario personale con strumenti analitici da studio medico (classificazione ESC/ESH, surge mattutino, carico ipertensivo) e report PDF condivisibili via link. Non è un'app commerciale: è un tool costruito per un uso privato tra persone che si conoscono.

## Operating Context

- Utilizzo quotidiano, spesso al mattino e alla sera, su dispositivo mobile (PWA installabile su home screen)
- Le misurazioni seguono le fasce orarie personalizzabili (default: mattina 6-12, pomeriggio 12-18, sera 18-22, notte 22-6)
- I dati sono sincronizzati su Supabase e disponibili offline via IndexedDB (Dexie)
- Il report PDF può essere condiviso via link pubblico (token-based) o scaricato

## Capabilities and Constraints

**Funzionalità:**

- Registrazione misurazioni (SYS, DIA, HR, note)
- Classificazione automatica ESC/ESH (ottimale, normale, elevata, ipertensione grado 1/2/3, crisi)
- Statistiche: medie, surge mattutino, carico ipertensivo, HRV, distribuzione per fascia oraria
- Report PDF con grafici Chart.js
- Condivisione report via link pubblico con token
- Gestione utenti (admin): creazione, modifica ruolo, disattivazione/attivazione, eliminazione permanente, reset password, cambio email
- Promemoria configurabili per giorno e ora
- Dark mode, i18n (italiano, inglese)
- PWA con service worker per uso offline

**Vincoli tecnici:**

- Supabase come backend (auth table-based, non Supabase Auth)
- Deploy su GitHub Pages (static hosting, hash-based routing)
- Privacy dati sanitari: i dati non devono essere accessibili a terzi non autorizzati
- I report condivisi sono accessibili solo via token univoco
- L'accesso admin è protetto da controllo ruolo lato client e RLS lato database

**Terminologia:**

- "Misurazione" o "lettura" = una singola rilevazione (SYS/DIA/HR)
- "Fascia oraria" = intervallo della giornata (mattina, pomeriggio, sera, notte)
- "Report" = PDF generato con letture, statistiche e grafici
- "Operatore" = utente con ruolo admin

## Brand Commitments

- **Nome**: "Pressione" (scelto dal creatore, modificabile)
- **Claim**: "fatto con ❤️ per i miei amici" — gesto d'affetto, l'app è distribuita privatamente
- **Colore primario**: verde medicale (#006C4C in light mode, #4DD9A0 in dark mode) — scelta intenzionale per comunicare fiducia e ambito salute
- **Tipografia**: Inter (Google Fonts, self-hosted via CDN)
- **Tono**: informale ma professionale, in italiano

## Evidence on Hand

- Design system CSS completo in `src/style.css` (tokens, componenti, utilità)
- Schema database Supabase in `supabase/migrations/`
- Suite di test: 24 file, 228 test (unit + E2E)
- `progress.md` con cronologia sessioni di sviluppo
- `README.md` con panoramica del progetto

## Product Principles

1. **I dati sanitari sono privati.** Ogni decisione di design e architettura deve proteggere la riservatezza delle misurazioni.
2. **Semplice come un taccuino, utile come un tool medico.** L'interfaccia non deve intimorire; la potenza analitica emerge quando serve.
3. **Funziona offline.** L'app deve essere utilizzabile anche senza connessione (PWA, IndexedDB, sync).
4. **Fatto per amici, non per il mercato.** Le scelte di design privilegiano l'usabilità reale su metriche di conversione o growth.

## Accessibility & Inclusion

- Supporto dark mode per ridurre l'affaticamento visivo
- Target di pubblico adulto/anziano: tipografia leggibile (Inter, 14px+), contrasto elevato, touch target generosi (min 32px per bottoni compatti)
- i18n: interfaccia disponibile in italiano e inglese
