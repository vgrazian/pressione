# Pressione — Feature Inventory & Review

> **Versione:** 1.1.0 | **Data:** 2026-08-02  
> **URL:** https://vgrazian.github.io/pressione/  
> **Stack:** Vue 3 + Vite PWA · Supabase · Dexie/IndexedDB · Chart.js · chartjs-plugin-annotation · jsPDF  
> **Test:** 29 unit (Vitest) + 15 E2E (Playwright)

---

## 1. Auth & User Management

| Feature | Note |
|---|---|
| Login/Logout | Table-based auth, SHA-256 |
| Sessione TTL 8h | localStorage + IndexedDB fallback |
| Recupero password | Token-based flow su login |
| Cambio password | Da Impostazioni (richiede pw attuale) |
| Modifica email | Da Impostazioni → Account |
| RBAC admin/user | Admin: gestione utenti, reset pw |
| Topbar logout | Icona in alto a destra su ogni pagina |
| 8 utenti seed | nadia, valerio (admin) + 6 user + bot |

## 2. Dashboard (Home)

| Feature | Note |
|---|---|
| Saluto + ultima lettura | Card con categoria e valori |
| 4 KPI rapidi | Media SYS/DIA/BPM + conteggio |
| 5 letture recenti | Card compact |
| Empty state + CTA | |
| Skeleton loader | |

## 3. CRUD Misurazioni

| Feature | Note |
|---|---|
| Inserimento/modifica | SYS, DIA, BPM, data, ora, note |
| Classificazione live | ESC/ESH in tempo reale |
| Validazione | Range clinici + DIA < SYS |
| Rilevazione duplicati | Blocca stessa misurazione entro 10 min |
| Eliminazione | Singola con conferma, massiva da Impostazioni |
| Offline-first | IndexedDB → Supabase |

## 4. Lista Misurazioni

| Feature | Note |
|---|---|
| Lista cronologica | |
| Filtro categoria | 6 chip ESC/ESH |
| Ricerca testuale | Note e valori |
| Swipe-to-delete | Mobile, touch events nativi |
| Icona oraria | Sole/luna |
| Edit/Delete | Su ogni card |
| Skeleton loader | |

## 5. Statistiche Avanzate

| Feature | Note |
|---|---|
| Filtri: 7g, 30g, custom | Date picker |
| **Line chart (Chart.js)** | SYS (rosso), DIA (blu), BPM (grigio tratteggiato) |
| **Fascia sicurezza OMS** | Rettangoli sfumati SYS 90-140, DIA 60-90 |
| **Grafico derivate dP/dt** | mmHg/ora, smoothing media mobile 3pt |
| **Allarme >10 mmHg/h** | Barre rosse + lista segmenti critici |
| **Morning Surge** | Δ 06-09 vs 20-23, badge ⚠️ |
| **Carico Ipertensivo** | % fuori norma + barra progresso |
| **HRV** | Deviazione standard BPM |
| **Pie chart OMS cliccabile** | 4 categorie, click → filtra lista |
| **Trend lines** | Lineare + Media mobile |
| Auto-aggregazione | Per >50 punti |
| Skeleton loader | |

## 6. Report e Condivisione

| Feature | Note |
|---|---|
| Filtri contenuto | Periodo, storico, anonimizza |
| **PDF (jsPDF)** | A4 con header, stats, tabella |
| **Condivisione** | Email, WhatsApp, Web Share API |
| **Link temporaneo 48h** | Token + PIN 4 cifre opzionale |
| **Revoca link** | Lista attivi + bottone revoca |
| **PIN gate** | SharedReportView con schermata PIN |
| **Pagina medico** | Tabella completa, responsive |

## 7. Impostazioni

| Feature | Note |
|---|---|
| Lingua IT/EN | Selettore in cima |
| Account | Username, email, ruolo |
| Modifica email/password | |
| Promemoria | Multipli, orari + giorni |
| CSV Export | |
| Genera dati test | 30 letture via RPC Supabase |
| Gestione utenti (admin) | Ruolo, reset pw, disattiva |
| Elimina tutto | Doppia conferma |

## 8. UI/UX

| Feature | Note |
|---|---|
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
|---|---|
| Retry backoff | 2 tentativi esponenziali |
| Offline-first | Dexie sempre disponibile |
| Sync queue | Coda operazioni offline |
| RLS policies | Tutte le tabelle |
| SHA-256 hashing | Client + server |
| GDPR link TTL | 48 ore auto-scadenza |
| Stats cache | IndexedDB pronto |
| **44 test** | 29 unit + 15 E2E |

## 10. Area Miglioramenti Futuri

- [ ] Notifiche push per promemoria
- [ ] Grafico BPM come sub-chart separato
- [ ] Dark mode toggle manuale
- [ ] Multi-lingua esteso (FR, ES, DE, PT)
- [ ] Animazioni transizione viste
- [ ] Accessibilità screen reader
- [ ] Backup automatico Supabase storage
- [ ] Esportazione PDF con grafici incorporati

---

*Documento generato per revisione — ultimo aggiornamento 2026-08-02*
