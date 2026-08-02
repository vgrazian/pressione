/**
 * PWA Install Service
 * Captures beforeinstallprompt for Android/Chrome and detects iOS for manual instructions.
 */

let deferredPrompt = null
const installCallbacks = []

/**
 * Initialize PWA install event listeners. Call once on app mount.
 */
export function initPWAInstall() {
    window.addEventListener('beforeinstallprompt', (e) => {
        e.preventDefault()
        deferredPrompt = e
        installCallbacks.forEach(cb => cb(true))
    })

    window.addEventListener('appinstalled', () => {
        deferredPrompt = null
        installCallbacks.forEach(cb => cb(false))
    })
}

/**
 * Whether a native install prompt is currently available (Android/Chrome).
 */
export function isInstallPromptAvailable() {
    return deferredPrompt !== null
}

/**
 * Detect iOS (iPhone, iPad, iPod).
 */
export function isIOS() {
    return /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream
}

/**
 * Whether the app is already running in standalone (installed) mode.
 */
export function isStandalone() {
    const mqMatches = typeof window !== 'undefined' && window.matchMedia
        ? window.matchMedia('(display-mode: standalone)').matches
        : false
    const iosStandalone = typeof navigator !== 'undefined' && navigator.standalone
    return !!(mqMatches || iosStandalone)
}

/**
 * Trigger the native install prompt. Returns true if user accepted.
 */
export async function promptInstall() {
    if (!deferredPrompt) return false

    deferredPrompt.prompt()
    const { outcome } = await deferredPrompt.userChoice
    deferredPrompt = null
    installCallbacks.forEach(cb => cb(false))
    return outcome === 'accepted'
}

/**
 * Register a callback that fires when install becomes available or unavailable.
 */
export function onInstallAvailable(callback) {
    installCallbacks.push(callback)
    if (deferredPrompt) callback(true)
}
