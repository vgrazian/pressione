# Pressione — Feature Inventory & Review Document

> **Versione:** 1.0.0 | **Data:** 2026-08-02  
> **URL:** <https://vgrazian.github.io/pressione/>  
> **Repo:** <https://github.com/vgrazian/pressione>  
> **Stack:** Vue 3 + Vite PWA · Supabase (PostgreSQL) · Dexie/IndexedDB · Chart.js · jsPDF  
> **Test:** 29 unit (Vitest) + 15 E2E (Playwright) — 44 test passanti

---

## 1. Autenticazione e Gestione Utenti

| Feature | Stato | Note |
| --- | --- | --- |
| Login/Logout | ✅ | Table-based auth su Supabase (SHA-256) |
| Sessione persistente | ✅ | localStorage con TTL 8 ore |
| Registrazione nuovi utenti | ✅ | Via admin o script seed |
| Cambio password | ✅ | Da Impostazioni |
| Recupero password | ✅ | Token-based recovery flow su login |
| Modifica email | ✅ | Da Impostazioni → Account |
| RBAC (admin/user) | ✅ | Admin: gestione utenti, reset password |
| Logout globale (topbar) | ✅ | Icona logout in alto a destra |
| 8 utenti pre-configurati | ✅ | nadia, valerio (admin) + 6 user + bot |

---

## 2. Dashboard (Home)

| Feature | Stato | Note |
| --- | --- | --- |
| Saluto personalizzato | ✅ | "Ciao, {username}" |
| Ultima misurazione | ✅ | Card con valori, categoria, data/ora |
| Statistiche rapide (4 KPI) | ✅ | Media SYS/DIA/BPM + conteggio |
| Letture recenti | ✅ | Ultime 5 in卡片 compact |
| Bottone rapido "Nuova" | ✅ | In header e empty state |
| Empty state | ✅ | Con CTA per prima misurazione |
| Skeleton loader | ✅ | Durante caricamento dati |
| Refresh da Supabase | ✅ | Con retry + backoff esponenziale |

---

## 3. CRUD Misurazioni

| Feature | Stato | Note |
| --- | --- | --- |
| Inserimento lettura | ✅ | SYS, DIA, BPM, data, ora, note |
| Modifica lettura | ✅ | Navigazione da lista o home |
| Validazione input | ✅ | Range 1-300/1-200/1-300 + DIA < SYS |
| Classificazione live | ✅ | Preview categoria ESC/ESH durante typing |
| Rilevazione duplicati | ✅ | Blocca se stessa misurazione entro 10 min |
| Eliminazione singola | ✅ | Con conferma dialog |
| Eliminazione massiva | ✅ | "Elimina tutto" da Impostazioni |
| Offline-first | ✅ | Salva in IndexedDB, sync a Supabase |

---

## 4. Lista Misurazioni

| Feature | Stato | Note |
| --- | --- | --- |
| Lista cronologica | ✅ | Ordinata per data decrescente |
| Filtro per categoria | ✅ | Chip selezionabili (6 categorie ESC/ESH) |
| Ricerca testuale | ✅ | Per note o valori |
| Modifica da lista | ✅ | Bottone "Modifica" su ogni card |
| Eliminazione da lista | ✅ | Con conferma |
| Icona oraria | ✅ | Sole/luna per momento della giornata |
| Empty state | ✅ | |
| Skeleton loader | ✅ | |

---

## 5. Statistiche Avanzate

| Feature | Stato | Note |
| --- | --- | --- |
| **Filtri temporali** | ✅ | 7gg, 30gg, personalizzato (date picker) |
| **Line chart (Chart.js)** | ✅ | SYS (rosso), DIA (blu), BPM (grigio) |
| **Doppio asse Y** | ✅ | mmHg sinistra, BPM destra |
| **Grafico derivate dP/dt** | ✅ | Variazione mmHg/ora con barre colorate |
| **Soglia allarme 10 mmHg/h** | ✅ | Barre rosse + lista segmenti critici |
| **Smoothing media mobile** | ✅ | 3-punti prima del calcolo derivata |
| **Morning Surge** | ✅ | Δ 06-09 vs 20-23 con badge allarme |
| **Carico Ipertensivo** | ✅ | % letture fuori norma + barra progresso |
| **HRV (variabilità FC)** | ✅ | Deviazione standard BPM |
| **Pie chart OMS** | ✅ | 4 categorie: Normale, Elevata, Stadio 1, Stadio 2+ |
| **Auto-aggregazione** | ✅ | Per dataset >50 punti |
| **Empty state** | ✅ | Con CTA |
| **Skeleton loader** | ✅ | |

---

## 6. Report e Condivisione

| Feature | Stato | Note |
| --- | --- | --- |
| **Filtri contenuto** | ✅ | Periodo, includi storico, anonimizza |
| **Anteprima tabella** | ✅ | Prime 20 righe visibili in-app |
| **PDF (jsPDF)** | ✅ | A4 con header, stats, tabella completa |
| **Condivisione nativa** | ✅ | Web Share API |
| **Email** | ✅ | mailto: con oggetto e corpo precompilati |
| **WhatsApp** | ✅ | wa.me link con testo formattato |
| **Link temporaneo (48h)** | ✅ | Token + PIN opzionale, auto-scadenza |
| **Pagina medico** | ✅ | SharedReportView pubblica |
| **Revoca link** | ⚠️ | Backend pronto, UI non ancora implementata |

---

## 7. Impostazioni

| Feature | Stato | Note |
| --- | --- | --- |
| Lingua IT/EN | ✅ | Selettore in cima alle Impostazioni |
| Account info | ✅ | Username, email, ruolo |
| Modifica email | ✅ | |
| Cambio password | ✅ | Richiede password attuale |
| Promemoria | ✅ | Multipli, con giorni e orari |
| CSV Export | ✅ | Scarica CSV con tutte le letture |
| Genera dati test | ✅ | 30 letture casuali via Supabase RPC |
| Gestione utenti (admin) | ✅ | Cambio ruolo, reset password, disattiva |
| Elimina tutti i dati | ✅ | Con doppia conferma |
| Versione app | ✅ | Footer |

---

## 8. UI/UX e Design System

| Feature | Stato | Note |
| --- | --- | --- |
| Design system CSS | ✅ | Variabili CSS, radius 12px consistente |
| Dark mode | ✅ | `prefers-color-scheme: dark` |
| Font Inter | ✅ | Google Fonts, font-display: swap |
| Icone SVG (no emoji) | ✅ | Componente AppIcon con 18 icone |
| Skeleton loader | ✅ | Componente riutilizzabile |
| Confirm dialog | ✅ | Globale via provide/inject |
| Top bar | ✅ | Logo + logout, sticky |
| Bottom nav | ✅ | 4 tab: Home, Lista, Statistiche, Impostazioni |
| Offline banner | ✅ | Giallo quando Supabase non raggiungibile |
| Focus visible | ✅ | `:focus-visible` su tutti gli interattivi |
| Reduced motion | ✅ | `prefers-reduced-motion: reduce` |
| Responsive | ✅ | Mobile-first, max-width 800px |
| PWA | ✅ | Installabile, service worker, manifest |

---

## 9. Infrastruttura e Affidabilità

| Feature | Stato | Note |
| --- | --- | --- |
| Retry automatico | ✅ | 2 tentativi con backoff esponenziale |
| Offline-first | ✅ | Dexie/IndexedDB sempre disponibile |
| Sync queue | ✅ | Operazioni in coda se offline |
| Connectivity check | ✅ | Ogni 30 secondi |
| RLS policies | ✅ | Tutte le tabelle protette |
| Password hashing | ✅ | SHA-256 lato client + server |
| Row Level Security | ✅ | Policies per anon e authenticated |
| GDPR: auto-scadenza link | ✅ | 48 ore TTL su shared_reports |
| Build automatico | ✅ | `npm run build` senza errori |
| Test suite | ✅ | 29 unit + 15 E2E |

---

## 10. Funzionalità bp-tracker Originali

| Feature bp-tracker | Pressione | Gap |
| --- | --- | --- |
| CRUD misurazioni | ✅ | |
| Classificazione ESC/ESH | ✅ | |
| Home dashboard | ✅ | |
| Lista con filtri | ✅ | |
| Statistiche con grafici | ✅ | Superato: Chart.js + derivate |
| Report | ✅ | Superato: PDF + link + condivisione |
| Promemoria | ✅ | |
| CSV export | ✅ | |
| Multi-lingua | ✅ | IT/EN |
| Genera dati test | ✅ | |
| Rilevazione duplicati | ✅ | |
| Trend algorithm | ✅ | Lineare + Media Mobile |
| Swipe-to-delete | ❌ | Usa bottone Elimina |
| Google Drive backup | ❌ | Sostituito da Supabase |
| Wear OS | ❌ | N/A per PWA |
| Notifiche push | ❌ | Non implementato |

---

## Aree di Miglioramento Identificate

### Priorità Alta

- [ ] **Notifiche push** per promemoria misurazione
- [ ] **Fascia di sicurezza** (OMS) sul grafico principale come area sfumata
- [ ] **Tooltip interattivo** sul line chart con long-press/tap
- [ ] **Swipe-to-delete** nella lista misurazioni
- [ ] **Revoca link** nella UI del report

### Priorità Media

- [ ] **Grafico BPM** separato come sub-chart sotto l'asse X
- [ ] **Pie chart cliccabile** per filtrare lo storico
- [ ] **Esportazione CSV** con più opzioni (solo medie, solo storico)
- [ ] **Backup automatico** su Supabase storage
- [ ] **Cache locale** delle statistiche (ricalcolo solo su nuovo dato)

### Priorità Bassa

- [ ] **PIN obbligatorio** sui link temporanei
- [ ] **Multi-lingua** esteso (FR, ES, DE, PT)
- [ ] **Dark mode toggle** manuale (oltre a prefers-color-scheme)
- [ ] **Animazioni** di transizione tra le viste
- [ ] **Accessibilità** avanzata (screen reader, aria labels)
