---
name: IperTeso Android
description: Diario digitale della pressione arteriosa per Android — Jetpack Compose + Material 3
colors:
  primary: "#3B5D45"
  primary-light: "#AAD0B2"
  primary-bg: "#C6ECCD"
  primary-dark-bg: "#2C4E37"
  primary-variant: "#43664D"
  error: "#BA1A1A"
  error-container: "#FFDAD6"
  warning: "#EF6C00"
  critical: "#D32F2F"
  light-bg: "#F8FDF8"
  light-surface: "#F8FDF8"
  light-surface-variant: "#DBE4DD"
  light-on-bg: "#191C1A"
  light-on-surface-variant: "#404943"
  light-outline: "#707973"
  dark-bg: "#191C1A"
  dark-surface: "#191C1A"
  dark-surface-variant: "#404943"
  dark-on-bg: "#E1E3DF"
  dark-on-surface-variant: "#BFC9C2"
  dark-outline: "#89938C"
  category-optimal: "#2E7D32"
  category-normal: "#66BB6A"
  category-high-normal: "#FFA726"
  category-grade1: "#EF6C00"
  category-grade2: "#D32F2F"
  category-grade3: "#BA1A1A"
  category-crisis: "#880E4F"
typography:
  font-family: "sans-serif (system default)"
  display-large: "57sp / 64sp line / Normal weight / -0.25sp tracking"
  headline-large: "32sp / 40sp line / SemiBold"
  title-large: "22sp / 28sp line / Medium"
  title-medium: "16sp / 24sp line / Medium"
  body-large: "16sp / 24sp line / Normal / 0.5sp tracking"
  body-medium: "14sp / 20sp line / Normal / 0.25sp tracking"
  body-small: "12sp / 16sp line / Normal / 0.4sp tracking"
  label-large: "14sp / 20sp line / Medium"
  label-small: "11sp / 16sp line / Medium — da evitare per utenti anziani"
rounded:
  card: "12dp"
  badge: "12dp"
  chip: "24dp (FilterChip default)"
  skeleton: "12dp"
  button: "24dp (Button default)"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
components:
  reading-card:
    backgroundColor: "{colors.light-surface}"
    rounded: "{rounded.card}"
    padding: "16dp"
  category-badge:
    padding: "10dp horizontal / 4dp vertical"
    rounded: "{rounded.badge}"
    typography: "label-small (12sp Medium)"
  kpi-card:
    backgroundColor: "{colors.light-surface-variant}"
    padding: "12dp"
    typography: "title-large for value, label-small for label"
  skeleton-loader:
    backgroundColor: "surface-variant (theme-aware shimmer)"
    rounded: "{rounded.skeleton}"
---
# IperTeso Android — Design System

> **Stato:** ✅ implementato e allineato all'app (2026-08-13).  
> Questo documento descrive il design system **Android** (Jetpack Compose + Material 3).  
> Per l'inventario funzionale unificato **Web + Android** (matrice di parità, casi di test) vedi [`../FEATURE_INVENTORY.md`](../FEATURE_INVENTORY.md). Per lo stato di sviluppo vedi [`../IPERTESO_ANDROID_PLAN.md`](../IPERTESO_ANDROID_PLAN.md).

## Overview

IperTeso è un diario della pressione arteriosa nativo Android costruito con **Jetpack Compose + Material 3**. Il design system estende M3 con un tema medical-green personalizzato, una palette ESC/ESH per la classificazione della pressione, e pattern offline-first coerenti con la PWA web esistente.

**Modalità**: Operate (app medicale per completare task).
**Target**: Samsung S24 (6.2" 1080×2340, xxhdpi, Android 14 API 34).
**Font**: system default sans-serif (equivale a Inter/Roboto su Samsung).

## Colors

### Primary: Medical Green

Il colore primario è il verde medicale `#3B5D45` (allineato al colore dell'icona dell'app), scelto per comunicare fiducia in ambito salute. In dark mode diventa `#AAD0B2` (verde chiaro su sfondo scuro). Il colore di sfondo è un verde leggermente tinto `#F8FDF8` in light, `#191C1A` in dark.

### ESC/ESH Semantic Colors

Le 7 categorie di pressione usano colori semantici crescenti per gravità:

- **Ottimale** → verde `#2E7D32`
- **Normale** → verde chiaro `#66BB6A`
- **Normale-Alta** → arancione `#FFA726`
- **Grado 1** → arancione scuro `#EF6C00`
- **Grado 2** → rosso `#D32F2F`
- **Grado 3** → rosso profondo `#BA1A1A`
- **Crisi** → porpora `#880E4F`

### Regola per ReadingCard

Il valore **sistolico** NON va sempre in rosso. Usa:

- `#2E7D32` per Ottimale/Normale
- `#EF6C00` per Normale-Alta
- `MaterialTheme.colorScheme.error` da Grado 1 in su

## Typography

Scala M3 standard con `fontFamily = FontFamily.Default` (sans-serif di sistema). Per utenti anziani, evitare `labelSmall` (11sp) e preferire `bodyMedium` (14sp) o superiore per testo informativo. I titoli delle card KPI usano `titleLarge` (22sp Medium), i valori di pressione in `ReadingCard` usano `titleLarge Bold`.

## Layout

Layout a scorrimento verticale con `Scaffold` + `TopAppBar` su ogni schermata. Pattern comune:

```
Scaffold(
    topBar = TopAppBar(title, navigationIcon = back),
    floatingActionButton = FAB (solo Home)
) {
    LazyColumn o Column(scrollable)
}
```

Spaziatura coerente: `16dp` padding orizzontale container, `12dp` tra card, `8dp` tra elementi correlati.
Le card KPI sono in `Row` con `weight(1f)` ciascuna per distribuzione uniforme.

## Elevation & Depth

Elevazione piatta con tonal layering (Material 3):

- **Surface**: `CardDefaults.cardElevation(1.dp)` per card letture
- **Container**: `surfaceVariant` o `primaryContainer` per sezioni distinte
- **Dialog**: `AlertDialog` standard M3 per conferme
- **TopAppBar**: `primary` container color, nessuna elevazione separata

## Shapes

Angoli arrotondati uniformi:

- Card: `RoundedCornerShape(12.dp)`
- Badge categoria: `RoundedCornerShape(12.dp)`
- FilterChip: default M3 (24dp altezza)
- Skeleton shimmer: `RoundedCornerShape(12.dp)` per card, `4.dp` per linee interne

## Components

### ReadingCard

Card che mostra una singola misurazione: data/ora, SYS/DIA/BPM, note, badge categoria. Supporta `onClick` per navigare a edit. Il valore sistolico è color-coded per gravità ESC/ESH.

### CategoryBadge

Badge colorato con nome categoria ESC/ESH. Usa `Surface` con colore di sfondo al 15% opacity e testo full opacity del colore categoria.

### SkeletonLoader

Placeholder animato con effetto shimmer. I colori shimmer sono **theme-aware** (`MaterialTheme.colorScheme.surfaceVariant`) per funzionare in dark mode.

### KpiCard / StatCard

Card compatta per visualizzare un valore numerico con etichetta. Valore in `titleLarge Bold primary`, etichetta in `labelSmall onSurfaceVariant`. Da unificare in un unico componente condiviso.

### MedicationItem

Card per farmaco: icona pillola, nome, dosaggio, date inizio-fine. Sfondo `primaryContainer` se attivo, `surfaceVariant` se storico. Pulsanti "Stop" e "Elimina".

### Filter Chips (ReadingList)

4 chip raggruppati per gravità: Tutte | Normale | Ipertensione | Crisi. Il raggruppamento riduce il carico cognitivo da 8 a 4 opzioni.

## Web ↔ Android design mapping

Coerenza visiva tra le due versioni (stessa identità, implementazioni diverse):

| Elemento | Web (CSS token) | Android (Compose) |
| --- | --- | --- |
| Primary verde medicale | `--color-accent` ≈ `#3B5D45` | `Color.kt` Primary `#3B5D45` |
| Background light tinto | `--color-background` ≈ `#F8FDF8` | LightBackground `#F8FDF8` |
| Error | `--color-error` ≈ `#BA1A1A` | LightError `#BA1A1A` |
| Card radius | `--radius-md` 12px | `RoundedCornerShape(12.dp)` |
| Badge radius | 12px (pill-ish) | `RoundedCornerShape(12.dp)` |
| KPI card | `.insight-chip` / `.card` | `StatCard` (surfaceVariant, 12dp) |
| Skeleton | shimmer su `--color-surface` | `SkeletonLoader` theme-aware |
| Reading card | `.reading-card` hover/active | `ReadingCard` (elevation 1.dp) |
| Categorie ESC/ESH | `categories.js` (6 cat.) | `Category.kt` (7 cat. 2024, label+labelEn) |
| Font | Inter (Google Fonts) | system sans-serif |
| Dark mode | `[data-theme]` toggle | `isSystemInDarkTheme()` |

> **Differenza chiave categorie:** il Web usa 6 categorie (aggiunge `Ipotensione` ed `Elevata`), l'Android usa le 7 categorie ESC/ESH 2024 (Ottimale/Normale/Normale-Alta/Grado 1-3/Crisi). Le palette sono coerenti per le categorie condivise.

## Do's and Don'ts

**✅ Do:**

- Usa `collectAsState()` per osservare StateFlow nei composable
- Usa `MaterialTheme.colorScheme.*` per tutti i colori (mai hardcodati)
- Fornisci `contentDescription` su tutte le icone
- Raggruppa i filtri per gravità (max 4 opzioni)
- Usa `key` su `LazyColumn items()` per ricomposizioni corrette
- Metti `DateTimeFormatter` in `remember {}`

**❌ Don't:**

- Non usare `Color.LightGray` — rompe la dark mode
- Non esporre 7+ FilterChip simultaneamente
- Non rendere il sistolico sempre rosso — è fuorviante
- Non lasciare `contentDescription = null` sulle icone
- Non chiamare `.value` su `StateFlow` senza `collectAsState()`
- Non lasciare voci di menu senza handler (chevron fantasma)
