/**
 * Reminder Notification Scheduler
 *
 * Checks saved reminders every 60 seconds and shows browser
 * notifications when a reminder time matches the current time
 * (±30s tolerance) on the configured days of the week.
 *
 * Requires Notification permission (requested on first schedule).
 * Works in PWA standalone mode on Chrome/macOS.
 */

import { getReminders } from './dataService.js'

let schedulerInterval = null
let lastPermission = 'default'

/**
 * Request notification permission if not already granted.
 */
async function ensurePermission() {
    if (!('Notification' in window)) return false
    if (Notification.permission === 'granted') return true
    if (Notification.permission === 'denied') return false
    const result = await Notification.requestPermission()
    return result === 'granted'
}

/**
 * Check if a reminder should fire right now.
 * @param {Object} reminder - { time: '08:00', daysOfWeek: [1,2,3,4,5], enabled: true }
 * @returns {boolean}
 */
function shouldFire(reminder) {
    if (!reminder.enabled) return false

    const now = new Date()
    const [rh, rm] = (reminder.time || '00:00').split(':').map(Number)

    // Check time: current hour/minute matches within tolerance
    if (now.getHours() !== rh) return false
    if (now.getMinutes() !== rm) return false

    // Check day of week: JS getDay() returns 0=Sun, our schema uses 1=Mon..7=Sun
    const jsDay = now.getDay() === 0 ? 7 : now.getDay()
    const days = reminder.daysOfWeek || [1, 2, 3, 4, 5, 6, 7]
    return days.includes(jsDay)
}

/**
 * Show a notification for a reminder.
 */
function showNotification(reminder) {
    if (!('Notification' in window)) return
    if (Notification.permission !== 'granted') return

    try {
        new Notification('⏰ Promemoria Pressione', {
            body: 'È ora di misurare la pressione arteriosa.',
            icon: '/pressione/icon-192.png',
            badge: '/pressione/icon-192.png',
            tag: `pressione-reminder-${reminder.id || reminder.time}`,
            requireInteraction: true,
            silent: false
        })
    } catch (e) {
        console.warn('[Reminder] Notification failed:', e.message)
    }
}

/**
 * Main tick: load reminders and fire any that match current time.
 */
async function tick(username) {
    if (!username) return

    try {
        const reminders = await getReminders(username)
        const now = new Date()
        const timeKey = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`

        for (const r of reminders) {
            if (shouldFire(r)) {
                showNotification(r)
            }
        }
    } catch (e) {
        // Silently ignore — IndexedDB might be unavailable
    }
}

/**
 * Start the reminder scheduler for a user.
 * Call this after login.
 */
export function startReminderScheduler(username) {
    stopReminderScheduler()

    if (!username) return

    // Request permission upfront (best effort)
    ensurePermission().then(granted => {
        if (granted) {
            console.log('[Reminder] Notification permission granted')
        }
    })

    // Check every 60 seconds
    schedulerInterval = setInterval(() => tick(username), 60_000)

    // Also check immediately
    tick(username)

    console.log('[Reminder] Scheduler started for', username)
}

/**
 * Stop the reminder scheduler.
 * Call this on logout.
 */
export function stopReminderScheduler() {
    if (schedulerInterval) {
        clearInterval(schedulerInterval)
        schedulerInterval = null
    }
}

/**
 * Check if scheduler is running.
 */
export function isSchedulerRunning() {
    return schedulerInterval !== null
}
