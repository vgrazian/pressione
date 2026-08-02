import { ref } from 'vue'

/**
 * Service Worker update detection.
 * Detects when a new version is waiting and provides applyUpdate() to activate it.
 */
const updateAvailable = ref(false)

export function useSWUpdate() {
    return { updateAvailable, applyUpdate }
}

function applyUpdate() {
    if (navigator.serviceWorker) {
        navigator.serviceWorker.getRegistrations().then(regs => {
            regs.forEach(reg => {
                if (reg.waiting) {
                    reg.waiting.postMessage({ type: 'SKIP_WAITING' })
                }
            })
        })
        // Reload after SW takes over
        navigator.serviceWorker.addEventListener('controllerchange', () => {
            window.location.reload()
        }, { once: true })
    }
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
