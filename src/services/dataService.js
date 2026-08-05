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
    } catch {}
}

function loadReadingsFromLocalStorage(username) {
    try {
        const raw = localStorage.getItem(lsReadingsKey(username))
        if (!raw) return null
        const readings = JSON.parse(raw)
        if (!Array.isArray(readings) || readings.length === 0) return null
        return readings
    } catch { return null }
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
        category,
        created_at: reading.created_at || now,
        updated_at: now
    }

    // 1. Save to IndexedDB first
    const idbRecord = {
        id: normalized.id,
        username,
        timestamp: normalized.timestamp,
        systolic: normalized.systolic,
        diastolic: normalized.diastolic,
        heartRate: normalized.heart_rate,
        notes: normalized.notes,
        category: normalized.category,
        updatedAt: now
    }
    await db.readings.put(idbRecord)

    // Update localStorage backup (fire-and-forget)
    getReadings(username).then(all => saveReadingsToLocalStorage(username, all)).catch(() => {})

    // 2. Sync to Supabase if online
    if (isSupabaseConfigured) {
        try {
            await supabase.from('readings').upsert(normalized)
        } catch (e) {
            // Enqueue for later sync
            await db.syncQueue.put({
                username,
                operation: 'upsert',
                tableName: 'readings',
                recordId: normalized.id,
                recordData: normalized,
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

    // Update localStorage backup
    getReadings(username).then(all => saveReadingsToLocalStorage(username, all)).catch(() => {})

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

    try { localStorage.removeItem(lsReadingsKey(username)) } catch {}

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

    // Fallback: if IndexedDB is empty, try localStorage (iOS PWA isolation)
    if (readings.length === 0) {
        const lsReadings = loadReadingsFromLocalStorage(username)
        if (lsReadings && lsReadings.length > 0) {
            try { await db.readings.bulkPut(lsReadings) } catch {}
            readings = lsReadings
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
    if (!isSupabaseConfigured) return

    try {
        await withRetry(async () => {
            const { data: readings, error } = await supabase
                .from('readings')
                .select('*')
                .eq('username', username)
                .order('timestamp', { ascending: false })

            if (error) throw error

            if (readings && readings.length > 0) {
                const mapped = readings.map(r => ({
                    id: r.id, username: r.username, timestamp: r.timestamp,
                    systolic: r.systolic, diastolic: r.diastolic,
                    heartRate: r.heart_rate, notes: r.notes || '',
                    category: classifyReading(r.systolic, r.diastolic),
                    updatedAt: r.updated_at
                }))
                await db.readings.bulkPut(mapped)
                saveReadingsToLocalStorage(username, mapped)
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
                await supabase.from(item.tableName).delete().eq('id', item.recordId)
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
 * Import CSV data (compatible with Pressione and bp-tracker formats)
 */
export async function importCSV(username, file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = async (e) => {
            try {
                const text = e.target.result
                const lines = text.split(/\r?\n/).filter(l => l.trim())
                if (lines.length < 2) throw new Error('CSV vuoto')
                const header = lines[0].toLowerCase()
                const isPressione = header.includes('sistolica')
                let imported = 0
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
                        const timestamp = new Date(`${date}T${time}`).toISOString()
                        await upsertReading({ systolic: sys, diastolic: dia, heartRate: hr, timestamp, notes }, username)
                        imported++
                    } catch (err) { errors.push(`Riga ${i}: ${err.message}`) }
                }
                resolve({ imported, errors })
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

    // Try RPC first
    try {
        const { error } = await supabase.rpc('generate_test_data', {
            p_username: username,
            p_count: count
        })
        if (!error) return count
    } catch { /* fall back to client-side */ }

    // Client-side fallback: generate readings locally
    const now = new Date().toISOString()
    const readings = []
    for (let i = 0; i < count; i++) {
        const systolic = 110 + Math.floor(Math.random() * 50)
        const diastolic = 65 + Math.floor(Math.random() * 30)
        const heartRate = 60 + Math.floor(Math.random() * 30)
        const ts = new Date(Date.now() - Math.random() * 30 * 86400000).toISOString()
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

    // Batch sync to Supabase
    if (isSupabaseConfigured) {
        try {
            await supabase.from('readings').upsert(readings)
        } catch (e) {
            console.warn('Test data sync to Supabase failed, saved locally:', e)
        }
    }

    return count
}
