// Data service: Supabase CRUD + IndexedDB cache
import { supabase, isSupabaseConfigured } from './supabaseClient'
import { db } from '../db'
import { generateId } from './ids'
import { classifyReading } from './categories'

const MAX_RETRIES = 2
const RETRY_DELAY = 1000

// --- localStorage bridge for readings (iOS PWA compatibility) ---
function lsReadingsKey(username) { return 'pressione_readings_' + username }

function saveReadingsToLocalStorage(username, readings) {
    try {
        const slim = readings.map(r => ({
            id: r.id, username: r.username, timestamp: r.timestamp,
            systolic: r.systolic, diastolic: r.diastolic,
            heartRate: r.heartRate, notes: r.notes || '',
            category: r.category, updatedAt: r.updatedAt || r.timestamp
        }))
        localStorage.setItem(lsReadingsKey(username), JSON.stringify(slim))
        console.log('[LS-bridge] Saved', slim.length, 'readings to localStorage for', username)
    } catch (e) {
        console.warn('[LS-bridge] Save failed:', e.message)
    }
}

function loadReadingsFromLocalStorage(username) {
    try {
        const raw = localStorage.getItem(lsReadingsKey(username))
        console.log('[LS-bridge] localStorage key:', lsReadingsKey(username), 'exists:', !!raw, 'size:', raw ? raw.length : 0)
        if (!raw) return null
        const readings = JSON.parse(raw)
        if (!Array.isArray(readings) || readings.length === 0) return null
        console.log('[LS-bridge] Loaded', readings.length, 'readings from localStorage')
        return readings
    } catch (e) {
        console.warn('[LS-bridge] Load failed:', e.message)
        return null
    }
}

/**
 * Add or update a single reading in localStorage (synchronous helper).
 * Avoids the async getReadings round-trip used by saveReadingsToLocalStorage.
 */
function addReadingToLocalStorage(username, reading) {
    try {
        const key = lsReadingsKey(username)
        const raw = localStorage.getItem(key)
        let readings = raw ? JSON.parse(raw) : []
        if (!Array.isArray(readings)) readings = []
        // Replace if exists, otherwise prepend
        const idx = readings.findIndex(r => r.id === reading.id)
        if (idx >= 0) readings[idx] = reading
        else readings.unshift(reading)
        localStorage.setItem(key, JSON.stringify(readings))
        console.log('[LS-bridge] addReadingToLocalStorage:', reading.id, 'total:', readings.length)
    } catch (e) {
        console.warn('[LS-bridge] addReadingToLocalStorage failed:', e.message)
    }
}

function removeReadingFromLocalStorage(username, id) {
    try {
        const key = lsReadingsKey(username)
        const raw = localStorage.getItem(key)
        if (!raw) return
        let readings = JSON.parse(raw)
        if (!Array.isArray(readings)) return
        readings = readings.filter(r => r.id !== id)
        localStorage.setItem(key, JSON.stringify(readings))
        console.log('[LS-bridge] Removed reading', id, 'total:', readings.length)
    } catch (e) {
        console.warn('[LS-bridge] removeReadingFromLocalStorage failed:', e.message)
    }
}

/**
 * Retry wrapper with exponential backoff
 */
async function withRetry(fn, retries = MAX_RETRIES) {
    for (let i = 0; i <= retries; i++) {
        try {
            return await fn()
        } catch (e) {
            if (i === retries) throw e
            await new Promise(r => setTimeout(r, RETRY_DELAY * Math.pow(2, i)))
        }
    }
}

/**
 * Check if we can reach Supabase
 */
let lastOnlineCheck = 0
let lastOnlineStatus = true
export async function isOnline() {
    // Trust browser's online status for fast response
    if (!navigator.onLine) {
        lastOnlineStatus = false
        lastOnlineCheck = Date.now()
        return false
    }
    // Debounce: reuse last result if checked recently
    if (Date.now() - lastOnlineCheck < 30000) return lastOnlineStatus
    if (!isSupabaseConfigured) { lastOnlineStatus = false; lastOnlineCheck = Date.now(); return false }
    try {
        const controller = new AbortController()
        const timeout = setTimeout(() => controller.abort(), 3000)
        // Use a lightweight health check — just ping the REST API
        const { error } = await supabase.from('readings').select('id').limit(1).abortSignal(controller.signal)
        clearTimeout(timeout)
        // Accept both success and empty-result as "online"
        lastOnlineStatus = !error || error.code === 'PGRST116'
    } catch { lastOnlineStatus = false }
    lastOnlineCheck = Date.now()
    return lastOnlineStatus
}

/**
 * Upsert a reading (offline-first)
 */
export async function upsertReading(reading, username) {
    const now = new Date().toISOString()
    const category = classifyReading(reading.systolic, reading.diastolic)

    const normalized = {
        id: reading.id || generateId(),
        username,
        systolic: reading.systolic,
        diastolic: reading.diastolic,
        heart_rate: reading.heartRate,
        timestamp: reading.timestamp || now,
        notes: reading.notes || '',
        created_at: reading.created_at || now,
        updated_at: now
    }
    // Supabase record (no 'category' — column doesn't exist in schema)
    const supabaseRecord = { ...normalized }

    // 1. Save to IndexedDB first
    const idbRecord = {
        id: normalized.id,
        username,
        timestamp: normalized.timestamp,
        systolic: normalized.systolic,
        diastolic: normalized.diastolic,
        heartRate: normalized.heart_rate,
        notes: normalized.notes,
        category,
        updatedAt: now
    }
    await db.readings.put(idbRecord)

    // Direct localStorage update (avoids async getReadings race)
    addReadingToLocalStorage(username, {
        id: idbRecord.id, username, timestamp: idbRecord.timestamp,
        systolic: idbRecord.systolic, diastolic: idbRecord.diastolic,
        heartRate: idbRecord.heartRate, notes: idbRecord.notes || '',
        category, updatedAt: idbRecord.updatedAt
    })

    // 2. Sync to Supabase if online
    if (isSupabaseConfigured) {
        try {
            await supabase.from('readings').upsert(supabaseRecord)
        } catch (e) {
            // Enqueue for later sync
            await db.syncQueue.put({
                username,
                operation: 'upsert',
                tableName: 'readings',
                recordId: normalized.id,
                recordData: supabaseRecord,
                createdAt: now
            })
        }
    }

    return normalized
}

/**
 * Delete a reading (soft-delete via sync)
 */
export async function deleteReading(id, username) {
    // 1. Remove from IndexedDB
    await db.readings.delete(id)

    // Direct localStorage update
    removeReadingFromLocalStorage(username, id)

    // 2. Delete from Supabase
    if (isSupabaseConfigured) {
        try {
            await supabase.from('readings').delete().eq('id', id).eq('username', username)
        } catch (e) {
            await db.syncQueue.put({
                username,
                operation: 'delete',
                tableName: 'readings',
                recordId: id,
                recordData: null,
                createdAt: new Date().toISOString()
            })
        }
    }
}

/**
 * Delete all readings for a user
 */
export async function deleteAllReadings(username) {
    await db.readings.where('username').equals(username).delete()

    try { localStorage.removeItem(lsReadingsKey(username)) } catch { }

    if (isSupabaseConfigured) {
        await supabase.from('readings').delete().eq('username', username)
    }
}

/**
 * Get readings from IndexedDB (with optional filters)
 */
export async function getReadings(username, filters = {}) {
    let collection = db.readings.where('username').equals(username)

    // Sort by timestamp descending
    let readings = await collection.toArray()
    console.log('[getReadings] IndexedDB returned', readings.length, 'readings for', username)

    // Fallback: if IndexedDB is empty, try localStorage (iOS PWA isolation)
    if (readings.length === 0) {
        console.log('[getReadings] IndexedDB empty, trying localStorage fallback...')
        const lsReadings = loadReadingsFromLocalStorage(username)
        if (lsReadings && lsReadings.length > 0) {
            try { await db.readings.bulkPut(lsReadings) } catch { }
            readings = lsReadings
            console.log('[getReadings] Restored', readings.length, 'readings from localStorage')
        }
    }
    // Final fallback: if still empty, try Supabase directly
    // (iOS 18 isolates both IndexedDB and localStorage — Supabase is the only bridge)
    if (readings.length === 0 && isSupabaseConfigured) {
        console.log('[getReadings] Both empty, trying Supabase fallback...')
        try {
            const { data, error } = await supabase
                .from('readings')
                .select('*')
                .eq('username', username)
                .order('timestamp', { ascending: false })
            if (!error && data && data.length > 0) {
                const mapped = data.map(r => ({
                    id: r.id, username: r.username, timestamp: r.timestamp,
                    systolic: r.systolic, diastolic: r.diastolic,
                    heartRate: r.heart_rate, notes: r.notes || '',
                    category: classifyReading(r.systolic, r.diastolic),
                    updatedAt: r.updated_at
                }))
                await db.readings.bulkPut(mapped)
                // Also save to localStorage for non-iOS-18 contexts
                saveReadingsToLocalStorage(username, mapped)
                readings = mapped
                console.log('[getReadings] Supabase fallback returned', readings.length, 'readings')
            } else {
                console.log('[getReadings] Supabase fallback: no data or error:', error?.message || 'empty')
            }
        } catch (e) {
            console.warn('[getReadings] Supabase fallback failed:', e.message)
        }
    }
    readings.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))

    // Apply filters
    if (filters.category) {
        readings = readings.filter(r => r.category === filters.category)
    }
    if (filters.fromDate) {
        readings = readings.filter(r => new Date(r.timestamp) >= new Date(filters.fromDate))
    }
    if (filters.toDate) {
        readings = readings.filter(r => new Date(r.timestamp) <= new Date(filters.toDate))
    }
    if (filters.search) {
        const q = filters.search.toLowerCase()
        readings = readings.filter(r =>
            r.notes?.toLowerCase().includes(q) ||
            String(r.systolic).includes(q) ||
            String(r.diastolic).includes(q) ||
            String(r.heartRate).includes(q)
        )
    }
    if (filters.limit) {
        readings = readings.slice(0, filters.limit)
    }

    return readings
}

/**
 * Get a single reading by ID
 */
export async function getReadingById(id) {
    return db.readings.get(id)
}

/**
 * Refresh data from Supabase
 */
export async function refreshFromServer(username) {
    if (!isSupabaseConfigured) {
        console.log('[refreshFromServer] Supabase not configured, skipping')
        return
    }

    try {
        await withRetry(async () => {
            const { data: readings, error } = await supabase
                .from('readings')
                .select('*')
                .eq('username', username)
                .order('timestamp', { ascending: false })

            if (error) throw error

            console.log('[refreshFromServer] Supabase returned', readings ? readings.length : 0, 'readings for', username)

            const mapped = readings && readings.length > 0
                ? readings.map(r => ({
                    id: r.id, username: r.username, timestamp: r.timestamp,
                    systolic: r.systolic, diastolic: r.diastolic,
                    heartRate: r.heart_rate, notes: r.notes || '',
                    category: classifyReading(r.systolic, r.diastolic),
                    updatedAt: r.updated_at
                }))
                : []

            // Safety: if Supabase returns 0 but we have local data, don't wipe (Supabase may be unreachable)
            if (mapped.length === 0) {
                const localCount = await db.readings.where('username').equals(username).count()
                if (localCount > 0) {
                    console.log('[refreshFromServer] Supabase returned 0 but local has', localCount, '— keeping local data')
                    return
                }
            }

            // Clear existing IndexedDB data for this user to properly reflect deletions
            await db.readings.where('username').equals(username).delete()

            if (mapped.length > 0) {
                await db.readings.bulkPut(mapped)
                saveReadingsToLocalStorage(username, mapped)
                console.log('[refreshFromServer] Synced', mapped.length, 'readings to IndexedDB + localStorage')
            } else {
                try { localStorage.removeItem(lsReadingsKey(username)) } catch { }
                console.log('[refreshFromServer] No readings on server, cleared local cache')
            }
        })
    } catch (e) {
        console.warn('refreshFromServer failed (will retry later):', e.message)
    }
}

/**
 * Retry failed sync operations
 */
export async function retrySyncQueue(username) {
    const pending = await db.syncQueue.where('username').equals(username).toArray()

    for (const item of pending) {
        try {
            if (item.operation === 'upsert') {
                await supabase.from(item.tableName).upsert(item.recordData)
            } else if (item.operation === 'delete') {
                await supabase.from(item.tableName).delete().eq('id', item.recordId).eq('username', item.username)
            }
            await db.syncQueue.delete(item.id)
        } catch (e) {
            console.warn('Sync retry failed for', item.id, e)
        }
    }
}

/**
 * Upsert a reminder
 */
export async function upsertReminder(reminder, username) {
    const now = new Date().toISOString()
    const normalized = {
        id: reminder.id || generateId(),
        username,
        enabled: reminder.enabled !== false,
        time: reminder.time,
        days_of_week: reminder.daysOfWeek || [1, 2, 3, 4, 5, 6, 7],
        updated_at: now
    }

    await db.reminders.put({
        ...normalized,
        daysOfWeek: normalized.days_of_week,
        updatedAt: now
    })

    if (isSupabaseConfigured) {
        try {
            await supabase.from('reminders').upsert(normalized)
        } catch (e) {
            console.warn('Reminder sync failed:', e)
        }
    }

    return normalized
}

/**
 * Delete a reminder
 */
export async function deleteReminder(id, username) {
    await db.reminders.delete(id)
    if (isSupabaseConfigured) {
        try {
            await supabase.from('reminders').delete().eq('id', id).eq('username', username)
        } catch (e) {
            console.warn('Reminder delete sync failed:', e)
        }
    }
}

/**
 * Get all reminders for a user
 */
export async function getReminders(username) {
    return db.reminders.where('username').equals(username).toArray()
}

/**
 * Backup all user data as JSON download
 */
export async function backupData(username) {
    const readings = await db.readings.where('username').equals(username).toArray()
    const reminders = await db.reminders.where('username').equals(username).toArray()
    const settings = await db.settings.where('username').equals(username).toArray()

    const backup = {
        version: 1,
        exportedAt: new Date().toISOString(),
        username,
        readings: readings.map(r => ({
            systolic: r.systolic, diastolic: r.diastolic, heartRate: r.heartRate,
            timestamp: r.timestamp, notes: r.notes, category: r.category
        })),
        reminders,
        settings
    }

    const json = JSON.stringify(backup, null, 2)
    const blob = new Blob([json], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `pressione_backup_${username}_${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
    return backup.readings.length
}

/**
 * Restore data from a JSON backup file
 */
export async function restoreData(username, file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = async (e) => {
            try {
                const backup = JSON.parse(e.target.result)
                if (!backup.readings || !Array.isArray(backup.readings)) {
                    throw new Error('Formato backup non valido')
                }
                // Import readings
                for (const r of backup.readings) {
                    await upsertReading({
                        systolic: r.systolic, diastolic: r.diastolic, heartRate: r.heartRate,
                        timestamp: r.timestamp, notes: r.notes || ''
                    }, username)
                }
                resolve(backup.readings.length)
            } catch (err) { reject(err) }
        }
        reader.onerror = () => reject(new Error('Errore lettura file'))
        reader.readAsText(file)
    })
}
export async function exportCSV(readings) {
    const headers = ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note']
    const rows = readings.map(r => {
        const d = new Date(r.timestamp)
        return [
            d.toLocaleDateString('it-IT'),
            d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }),
            r.systolic,
            r.diastolic,
            r.heartRate,
            r.category || '',
            `"${(r.notes || '').replace(/"/g, '""')}"`
        ].join(',')
    })

    const csv = '\uFEFF' + headers.join(',') + '\n' + rows.join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)

    const a = document.createElement('a')
    a.href = url
    a.download = `pressione_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
}

/**
 * Import CSV data (compatible with IperTeso and bp-tracker formats)
 */
export async function importCSV(username, file, mode = 'add') {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = async (e) => {
            try {
                const text = e.target.result
                const lines = text.split(/\r?\n/).filter(l => l.trim())
                if (lines.length < 2) throw new Error('CSV vuoto')
                const header = lines[0].toLowerCase()
                const isPressione = header.includes('sistolica')

                // Pre-load existing readings for dedup (skip/overwrite modes)
                let existingTimestamps = new Set()
                if (mode === 'skip' || mode === 'overwrite') {
                    const existing = await getReadings(username)
                    existingTimestamps = new Set(existing.map(r => new Date(r.timestamp).getTime()))
                }

                let imported = 0, skipped = 0, overwritten = 0
                const errors = []
                for (let i = 1; i < lines.length; i++) {
                    const cols = lines[i].split(',').map(c => c.trim().replace(/^"|"$/g, ''))
                    if (cols.length < 3) continue
                    try {
                        let date, time, sys, dia, hr, notes = ''
                        if (isPressione) {
                            date = cols[0]; time = cols[1]; sys = parseInt(cols[2])
                            dia = parseInt(cols[3]); hr = parseInt(cols[4]); notes = cols[6] || ''
                        } else {
                            if (cols[0].includes('/') || cols[0].includes('-')) {
                                date = cols[0]; time = cols[1] || '12:00'
                                sys = parseInt(cols[2]); dia = parseInt(cols[3]); hr = parseInt(cols[4])
                                notes = cols[5] || ''
                            } else {
                                const ts = new Date(cols[0])
                                if (isNaN(ts.getTime())) throw new Error('Formato data non riconosciuto')
                                date = ts.toISOString().split('T')[0]
                                time = ts.toTimeString().slice(0, 5)
                                sys = parseInt(cols[1]); dia = parseInt(cols[2]); hr = parseInt(cols[3])
                                notes = cols[4] || ''
                            }
                        }
                        if (isNaN(sys) || isNaN(dia) || isNaN(hr)) throw new Error('Valori non validi')
                        if (sys < 1 || sys > 300 || dia < 1 || dia > 200 || hr < 1 || hr > 300)
                            throw new Error(`Fuori range: ${sys}/${dia} ${hr}`)
                        const timestamp = new Date(`${date}T${time}`).getTime()

                        // Merge logic
                        if (mode === 'skip' && existingTimestamps.has(timestamp)) {
                            skipped++
                            continue
                        }
                        if (mode === 'overwrite' && existingTimestamps.has(timestamp)) {
                            overwritten++
                        }

                        await upsertReading({ systolic: sys, diastolic: dia, heartRate: hr, timestamp: new Date(timestamp).toISOString(), notes }, username)
                        imported++
                    } catch (err) { errors.push(`Riga ${i}: ${err.message}`) }
                }
                resolve({ imported, skipped, overwritten, errors })
            } catch (err) { reject(err) }
        }
        reader.onerror = () => reject(new Error('Errore lettura file'))
        reader.readAsText(file)
    })
}

/**
 * Generate test data — tries RPC, falls back to client-side generation
 */
export async function generateTestData(username, count = 30) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

    // Always use client-side generation for full control over BP + time distribution
    const now = new Date().toISOString()
    const readings = []

    // BP distribution weights (sum ≈ 100%)
    const bpProfiles = [
        // { weight, systolic range, diastolic range, desc }
        { w: 40, sMin: 90, sMax: 119, dMin: 60, dMax: 79 },   // NORMAL
        { w: 20, sMin: 120, sMax: 129, dMin: 65, dMax: 84 },  // ELEVATED
        { w: 15, sMin: 130, sMax: 139, dMin: 80, dMax: 89 },  // STAGE 1
        { w: 10, sMin: 140, sMax: 179, dMin: 90, dMax: 109 }, // STAGE 2
        { w: 3, sMin: 180, sMax: 220, dMin: 110, dMax: 130 },// CRISIS
        { w: 7, sMin: 70, sMax: 99, dMin: 40, dMax: 64 },  // HYPOTENSION
        { w: 5, sMin: 80, sMax: 200, dMin: 40, dMax: 130 }, // UNCLASSIFIED (mixed)
    ]
    // Build cumulative weights for weighted random
    const totalW = bpProfiles.reduce((s, p) => s + p.w, 0)
    const cumulative = []
    let sum = 0
    for (const p of bpProfiles) { sum += p.w; cumulative.push(sum / totalW) }

    function pickBPProfile() {
        const r = Math.random()
        for (let i = 0; i < cumulative.length; i++) {
            if (r <= cumulative[i]) return bpProfiles[i]
        }
        return bpProfiles[0]
    }

    // Time-of-day distribution: morning(6-11) 30%, afternoon(12-17) 35%, evening(18-22) 25%, night(23-5) 10%
    function randomHour() {
        const r = Math.random()
        if (r < 0.30) return 6 + Math.floor(Math.random() * 6)   // 6-11
        if (r < 0.65) return 12 + Math.floor(Math.random() * 6)  // 12-17
        if (r < 0.90) return 18 + Math.floor(Math.random() * 5)  // 18-22
        return Math.random() < 0.5 ? 23 : Math.floor(Math.random() * 6) // 23,0,1,2,3,4,5
    }

    for (let i = 0; i < count; i++) {
        const profile = pickBPProfile()
        const systolic = profile.sMin + Math.floor(Math.random() * (profile.sMax - profile.sMin + 1))
        const diastolic = profile.dMin + Math.floor(Math.random() * (profile.dMax - profile.dMin + 1))
        const heartRate = 60 + Math.floor(Math.random() * 30)

        // Random day in last 30, random time of day (pure ms arithmetic, immune to DST)
        const dayOffset = Math.floor(Math.random() * 30)
        const hour = randomHour()
        const minute = Math.floor(Math.random() * 60)
        const second = Math.floor(Math.random() * 60)
        const nowMs = Date.now()
        const startOfToday = nowMs - (nowMs % 86400000)
        const ts = new Date(startOfToday - dayOffset * 86400000 + hour * 3600000 + minute * 60000 + second * 1000).toISOString()

        const category = classifyReading(systolic, diastolic)
        const id = generateId()

        readings.push({
            id,
            username,
            systolic,
            diastolic,
            heart_rate: heartRate,
            timestamp: ts,
            notes: 'Dato test auto-generato',
            category,
            created_at: ts,
            updated_at: now
        })

        // Save to IndexedDB
        await db.readings.put({
            id, username, timestamp: ts, systolic, diastolic,
            heartRate, notes: 'Dato test auto-generato', category, updatedAt: now
        })
    }

    // Batch sync to Supabase (note: category is computed client-side, not a DB column)
    if (isSupabaseConfigured) {
        const { error } = await supabase.from('readings').upsert(readings)
        if (error) {
            console.error('Test data sync to Supabase failed:', error.message)
            throw new Error('Errore nel salvataggio su Supabase: ' + error.message)
        }
    }

    return count
}
