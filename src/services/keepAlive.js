/**
 * Database Keep-Alive Service
 * Periodically pings Supabase to keep the connection alive and
 * requests persistent IndexedDB storage to prevent data eviction.
 */
import { supabase, isSupabaseConfigured } from './supabaseClient.js'
import { getSetting, setSetting } from '@/db/index.js'

let keepAliveInterval = null
let keepAliveUsername = null
const KEEP_ALIVE_KEY = 'keepAlive'
const PING_INTERVAL_MS = 5 * 60 * 1000 // 5 minutes

/**
 * Check if keep-alive is enabled for a user
 */
export async function isKeepAliveEnabled(username) {
    return await getSetting(username, '_system_' + KEEP_ALIVE_KEY, true)
}

/**
 * Start database keep-alive
 */
export async function startKeepAlive(username) {
    if (keepAliveInterval) return // already running

    keepAliveUsername = username

    // Request persistent storage (prevents IndexedDB eviction)
    if (navigator.storage && navigator.storage.persist) {
        try {
            const granted = await navigator.storage.persist()
            console.log('[KeepAlive] Persistent storage ' + (granted ? 'granted' : 'denied'))
        } catch (e) {
            console.warn('[KeepAlive] persist() failed:', e)
        }
    }

    // Periodic Supabase ping
    keepAliveInterval = setInterval(async () => {
        try {
            if (isSupabaseConfigured && supabase) {
                const start = Date.now()
                const { error } = await supabase.from('users').select('username').limit(1)
                const ms = Date.now() - start
                if (error) {
                    console.warn('[KeepAlive] Ping failed:', error.message)
                } else {
                    console.log('[KeepAlive] Ping OK:', ms + 'ms')
                }
            }
        } catch (e) {
            console.warn('[KeepAlive] Ping error:', e)
        }
    }, PING_INTERVAL_MS)

    // Run first ping immediately
    if (isSupabaseConfigured && supabase) {
        supabase.from('users').select('username').limit(1).then(({ error }) => {
            if (error) console.warn('[KeepAlive] Initial ping failed:', error.message)
            else console.log('[KeepAlive] Initial ping OK')
        })
    }

    // Save preference
    await setSetting(username, '_system_' + KEEP_ALIVE_KEY, true)

    console.log('[KeepAlive] Started for', username)
}

/**
 * Stop database keep-alive
 */
export async function stopKeepAlive() {
    if (keepAliveInterval) {
        clearInterval(keepAliveInterval)
        keepAliveInterval = null
    }

    if (keepAliveUsername) {
        await setSetting(keepAliveUsername, '_system_' + KEEP_ALIVE_KEY, false)
    }

    keepAliveUsername = null
    console.log('[KeepAlive] Stopped')
}

/**
 * Check if keep-alive is currently active
 */
export function isKeepAliveActive() {
    return keepAliveInterval !== null
}

/**
 * Get storage usage info
 */
export async function getStorageInfo() {
    const info = {
        usage: null,
        quota: null,
        persisted: false,
        percent: null
    }

    if (navigator.storage && navigator.storage.estimate) {
        try {
            const estimate = await navigator.storage.estimate()
            info.usage = estimate.usage
            info.quota = estimate.quota
            info.percent = estimate.quota ? Math.round((estimate.usage / estimate.quota) * 100) : null
        } catch (e) {
            console.warn('[KeepAlive] estimate() failed:', e)
        }
    }

    if (navigator.storage && navigator.storage.persisted) {
        try {
            info.persisted = await navigator.storage.persisted()
        } catch (e) {
            console.warn('[KeepAlive] persisted() failed:', e)
        }
    }

    return info
}

/**
 * Format bytes to human-readable string
 */
export function formatBytes(bytes) {
    if (bytes === null || bytes === undefined) return 'N/D'
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * Initialize keep-alive on app startup if previously enabled
 */
export async function initKeepAlive(username) {
    const enabled = await isKeepAliveEnabled(username)
    if (enabled) {
        console.log('[KeepAlive] Auto-starting for', username)
        await startKeepAlive(username)
    }
}
