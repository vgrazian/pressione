/**
 * Theme-aware chart color utility.
 * Reads CSS custom properties from :root so charts always reflect
 * the current light/dark theme. Call getChartColors() at render time
 * to get fresh values.
 */

/**
 * Read a CSS custom property from :root.
 * Returns the raw value (e.g., '#006C4C' or 'rgba(...)').
 */
function cssVar(name) {
    if (typeof window === 'undefined') return ''
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

/**
 * Parse a CSS hex/rgb color and return { r, g, b }.
 */
function parseColor(color) {
    if (!color) return { r: 0, g: 0, b: 0 }
    // Handle hex
    const hex = color.match(/^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i)
    if (hex) {
        return { r: parseInt(hex[1], 16), g: parseInt(hex[2], 16), b: parseInt(hex[3], 16) }
    }
    // Handle rgb/rgba
    const rgb = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/)
    if (rgb) {
        return { r: parseInt(rgb[1]), g: parseInt(rgb[2]), b: parseInt(rgb[3]) }
    }
    return { r: 128, g: 128, b: 128 }
}

/**
 * Create an rgba string from rgb + alpha.
 */
function rgba(color, alpha) {
    const c = typeof color === 'string' ? parseColor(color) : color
    return `rgba(${c.r},${c.g},${c.b},${alpha})`
}

/**
 * Lighten or darken a color by mixing with white/black.
 * factor > 0 = lighter, factor < 0 = darker.
 */
function adjust(color, factor) {
    const c = parseColor(color)
    const mix = factor > 0 ? 255 : 0
    const a = Math.abs(factor)
    return {
        r: Math.round(c.r + (mix - c.r) * a),
        g: Math.round(c.g + (mix - c.g) * a),
        b: Math.round(c.b + (mix - c.b) * a)
    }
}

/**
 * Derive a complementary blue from the accent green.
 * Used for diastolic line to create visual distinction.
 */
function diastolicColor(accent) {
    const c = parseColor(accent)
    // Rotate hue: green → steel blue
    return { r: Math.round(c.r * 0.3 + 69), g: Math.round(c.g * 0.6 + 123), b: Math.round(c.b * 0.4 + 157) }
}

/**
 * Main export: returns a complete set of theme-aware chart colors.
 * Call this at chart render time to always get current theme values.
 */
export function getChartColors() {
    const accent = cssVar('--color-accent') || '#3B5D45'
    const error = cssVar('--color-error') || '#BA1A1A'
    const accentMuted = cssVar('--color-accent-muted') || '#DCE8DF'
    const errorMuted = cssVar('--color-error-muted') || '#FFECEB'
    const surfaceRaised = cssVar('--color-surface-raised') || '#FFFFFF'
    const textPrimary = cssVar('--color-text-primary') || '#191C1A'
    const textSecondary = cssVar('--color-text-secondary') || '#5F6B62'
    const textTertiary = cssVar('--color-text-tertiary') || '#8B968E'
    const border = cssVar('--color-border') || '#E0E4DF'

    const accentRgb = parseColor(accent)
    const errorRgb = parseColor(error)
    const secondaryRgb = parseColor(textSecondary)

    const diastolic = diastolicColor(accent)

    // Determine if we're in dark mode by checking the surface luminance
    const isDark = accentRgb.r + accentRgb.g + accentRgb.b > 380 // light green = dark mode

    return {
        // ── Line colors ──────────────────────────────────────
        systolic: error,                                      // red for systolic
        systolicBg: rgba(errorRgb, 0.08),
        diastolic: `rgb(${diastolic.r},${diastolic.g},${diastolic.b})`, // steel blue
        diastolicBg: rgba(diastolic, 0.08),
        bpm: textSecondary,                                   // muted for BPM
        bpmBg: rgba(secondaryRgb, 0.06),

        // ── Annotation zones ────────────────────────────────
        targetZoneBg: rgba(accentRgb, 0.05),
        targetZoneBorder: rgba(accentRgb, 0.15),
        targetLabelBg: rgba(parseColor(surfaceRaised), isDark ? 0.9 : 0.85),
        targetLabelText: accent,
        sys140Line: rgba(errorRgb, 0.4),

        // ── Derivative chart ────────────────────────────────
        derivAlarm: '#D90429',                                // hard red — high visibility needed
        derivPositive: rgba(errorRgb, 0.5),
        derivNegative: rgba(diastolic, 0.5),

        // ── Pie / category colors ───────────────────────────
        catNormal: accent,
        catElevated: '#F9A825',                               // amber — consistent across themes
        catStage1: '#EF6C00',                                 // orange
        catStage2: error,
        catCrisis: '#7B1FA2',                                 // purple
        catHypotension: '#1976D2',                            // blue

        // ── Category map (for table rows, badges) ──────────
        categoryMap: {
            'NORMAL': accent,
            'ELEVATED': '#F9A825',
            'HYPERTENSION_STAGE_1': '#EF6C00',
            'HYPERTENSION_STAGE_2': error,
            'HYPERTENSIVE_CRISIS': '#7B1FA2',
            'HYPOTENSION': '#1976D2'
        },

        // ── Table / UI colors ───────────────────────────────
        tableBg: rgba(parseColor(accentMuted), 0.3),
        tableBorder: border,
        textPrimary,
        textSecondary,
        textTertiary,
        surfaceRaised,
        error,
        accent,
        accentMuted,
        errorMuted,

        // ── Helpers ─────────────────────────────────────────
        isDark,
        rgba,

        // ── Text colors used in SharedReportView inline styles ──
        warningBg: isDark ? 'rgba(255,152,0,0.15)' : '#FFF3E0',
        warningBorder: '#EF6C00',
        dangerBg: isDark ? 'rgba(186,26,26,0.15)' : '#FFEBEE',
        dangerBorder: error,
        successBg: isDark ? rgba(accentRgb, 0.15) : '#E8F5E9',
        successBorder: accent,
        infoBg: isDark ? 'rgba(249,168,37,0.12)' : '#FFF8E1',
        infoBorder: '#F9A825',
    }
}

/**
 * Category color lookup (convenience wrapper).
 */
export function catColor(category) {
    const colors = getChartColors()
    return colors.categoryMap[category] || '#999'
}
