import { ref } from 'vue'

/**
 * Service Worker update detection.
 * Detects when a new version is waiting and provides applyUpdate() to activate it.
 */
const updateAvailable = ref(false)
const updateFailed = ref(false)

export function useSWUpdate() {
    return { updateAvailable, updateFailed, applyUpdate, forceClearCache }
}

async function applyUpdate() {
    updateFailed.value = false

    // 1. Activate waiting service worker
    if (navigator.serviceWorker) {
        try {
            const registrations = await navigator.serviceWorker.getRegistrations()
            for (const reg of registrations) {
                if (reg.waiting) reg.waiting.postMessage({ type: 'SKIP_WAITING' })
                if (reg.installing) reg.installing.postMessage({ type: 'SKIP_WAITING' })
            }
        } catch { /* continue */ }
    }

    // 2. Listen for controller change (SW takes over) — instant reload
    let reloaded = false
    const doReload = () => {
        if (!reloaded) { reloaded = true; window.location.reload() }
    }
    if (navigator.serviceWorker) {
        navigator.serviceWorker.addEventListener('controllerchange', doReload, { once: true })
    }

    // 3. Fallback: if controllerchange didn't fire within 1.5s, reload anyway
    setTimeout(() => {
        if (!reloaded) {
            updateFailed.value = true
            doReload()
        }
    }, 1500)
}

/**
 * Force-clear all service workers and caches, then hard-reload.
 * Use when the app is stuck on an old cached version.
 */
async function forceClearCache() {
    // 1. Unregister all service workers
    if ('serviceWorker' in navigator) {
        const registrations = await navigator.serviceWorker.getRegistrations()
        for (const reg of registrations) {
            await reg.unregister()
        }
    }

    // 2. Delete all cache storage
    if ('caches' in window) {
        const keys = await caches.keys()
        for (const key of keys) {
            await caches.delete(key)
        }
    }

    // 3. Hard reload (bypass browser cache)
    window.location.reload(true)
}

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.ready.then(reg => {
        // Check for existing waiting SW
        if (reg.waiting) {
            updateAvailable.value = true
        }
        // Listen for new updates
        reg.addEventListener('updatefound', () => {
            const newWorker = reg.installing
            if (!newWorker) return
            newWorker.addEventListener('statechange', () => {
                if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                    updateAvailable.value = true
                }
            })
        })
    })

    // Also listen globally for updates
    navigator.serviceWorker.addEventListener('controllerchange', () => {
        // SW took over — could reload here, but we handle it in applyUpdate
    })
}
