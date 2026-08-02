// Data service: Supabase CRUD + IndexedDB cache
import { supabase, isSupabaseConfigured } from './supabaseClient'
import { db } from '../db'
import { generateId } from './ids'
import { classifyReading } from './categories'

const TABLES = ['readings', 'reminders']

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
    await db.readings.put({
        id: normalized.id,
        username,
        timestamp: normalized.timestamp,
        systolic: normalized.systolic,
        diastolic: normalized.diastolic,
        heartRate: normalized.heart_rate,
        notes: normalized.notes,
        category: normalized.category,
        updatedAt: now
    })

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

    // Pull readings
    const { data: readings, error } = await supabase
        .from('readings')
        .select('*')
        .eq('username', username)
        .order('timestamp', { ascending: false })

    if (error) throw new Error('Errore nel recupero dati: ' + error.message)

    if (readings) {
        const mapped = readings.map(r => ({
            id: r.id,
            username: r.username,
            timestamp: r.timestamp,
            systolic: r.systolic,
            diastolic: r.diastolic,
            heartRate: r.heart_rate,
            notes: r.notes,
            category: classifyReading(r.systolic, r.diastolic),
            updatedAt: r.updated_at
        }))
        await db.readings.bulkPut(mapped)
    }

    // Pull reminders
    const { data: reminders } = await supabase
        .from('reminders')
        .select('*')
        .eq('username', username)

    if (reminders) {
        await db.reminders.bulkPut(reminders)
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
 * Export readings as CSV and trigger download
 */
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
 * Generate test data via Supabase RPC
 */
export async function generateTestData(username, count = 30) {
  if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

  const { error } = await supabase.rpc('generate_test_data', {
    p_username: username,
    p_count: count
  })

  if (error) throw new Error('Errore generazione dati: ' + error.message)
  return count
}
