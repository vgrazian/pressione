import Dexie from 'dexie'

export const db = new Dexie('pressione')

db.version(1).stores({
    readings: 'id, username, timestamp, systolic, diastolic, heartRate',
    settings: '[username+key], username',
    reminders: 'id, username',
    syncQueue: '++id, username, tableName, recordId',
    cachedUsers: 'username',
    cachedStats: '[username+dateRange]'
})

// Helper to get a setting
export async function getSetting(username, key, defaultValue = null) {
    const row = await db.settings.get([username, key])
    return row ? row.value : defaultValue
}

// Helper to set a setting
export async function setSetting(username, key, value) {
    await db.settings.put({ username, key, value, updatedAt: new Date().toISOString() })
}
