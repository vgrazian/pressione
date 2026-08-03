/**
 * Configurable time bands for pressure readings.
 * Users can customize the hour ranges for morning, afternoon, evening, night.
 * Default bands follow clinical recommendations (ESC/ESH).
 */
import { getSetting, setSetting } from '@/db/index.js'

const BANDS_KEY = '_timeBands'

/** Default clinical time bands */
export function getDefaultBands() {
    return [
        { key: 'MORNING', label: 'Mattina', icon: '☀️', start: 6, end: 12 },
        { key: 'AFTERNOON', label: 'Pomeriggio', icon: '🌤️', start: 12, end: 17 },
        { key: 'EVENING', label: 'Sera', icon: '🌅', start: 17, end: 22 },
        { key: 'NIGHT', label: 'Notte', icon: '🌙', start: 22, end: 6 }
    ]
}

/**
 * Get the time band for a given hour.
 * NIGHT band wraps around midnight (start > end).
 */
export function getBandForHour(hour, bands) {
    for (const band of bands) {
        if (band.start <= band.end) {
            if (hour >= band.start && hour < band.end) return band
        } else {
            // Wraps midnight (e.g., NIGHT: 22-6)
            if (hour >= band.start || hour < band.end) return band
        }
    }
    // Fallback: return last band
    return bands[bands.length - 1] || bands[0]
}

/**
 * Load user's configured time bands from settings.
 * Falls back to defaults if not configured.
 */
export async function getUserBands(username) {
    if (!username) return getDefaultBands()
    try {
        const stored = await getSetting(username, BANDS_KEY, null)
        if (stored && Array.isArray(stored) && stored.length === 4) {
            return stored
        }
    } catch { /* fall through */ }
    return getDefaultBands()
}

/**
 * Save user's configured time bands to settings.
 */
export async function saveUserBands(username, bands) {
    if (!username || !bands) return
    await setSetting(username, BANDS_KEY, bands)
}

/**
 * Build a time-of-day distribution from readings using configured bands.
 * Returns a map: { MORNING: [systolic, ...], AFTERNOON: [...], ... }
 */
export function buildTimeOfDayDistribution(readings, bands) {
    const dist = {}
    const systolicByBand = {}
    for (const b of bands) {
        dist[b.key] = 0
        systolicByBand[b.key] = []
    }

    for (const r of readings) {
        const hour = new Date(r.timestamp).getHours()
        const band = getBandForHour(hour, bands)
        dist[band.key] = (dist[band.key] || 0) + 1
        systolicByBand[band.key].push(r.systolic)
    }

    return { dist, systolicByBand }
}

/**
 * Group readings by day, then by time band.
 * For the time-band grouped report view.
 * Returns: [{ date: '2026-08-03', bands: { MORNING: [r1, r2], AFTERNOON: [r3], ... } }, ...]
 */
export function groupReadingsByDayAndBand(readings, bands) {
    const grouped = {}

    for (const r of [...readings].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))) {
        const date = new Date(r.timestamp).toLocaleDateString('it-IT')
        const hour = new Date(r.timestamp).getHours()
        const band = getBandForHour(hour, bands)

        if (!grouped[date]) {
            grouped[date] = {}
            for (const b of bands) grouped[date][b.key] = []
        }
        grouped[date][band.key].push(r)
    }

    // Convert to sorted array (newest first)
    return Object.entries(grouped)
        .sort(([a], [b]) => new Date(b.split('/').reverse().join('-')) - new Date(a.split('/').reverse().join('-')))
        .map(([date, bands]) => ({ date, bands }))
}
