/**
 * Reminder Notification Scheduler — Cross-Platform
 *
 * Checks saved reminders every 60 seconds. Notification strategy:
 *   1. Web Notification API (Android Chrome PWA, desktop)
 *   2. In-app reactive banner (iOS Safari/PWA, all platforms)
 *   3. Vibration (mobile devices)
 */

import { ref } from 'vue'
import { getReminders } from './dataService.js'

let schedulerInterval = null

// Reactive state for in-app banner
export const reminderAlert = ref(null)

export function dismissReminderAlert() {
    reminderAlert.value = null
}

async function ensurePermission() {
    if (!('Notification' in window)) return false
    if (Notification.permission === 'granted') return true
    if (Notification.permission === 'denied') return false
    try { return (await Notification.requestPermission()) === 'granted' } catch { return false }
}

function vibrate() {
    if (navigator.vibrate) { try { navigator.vibrate([200, 100, 200]) } catch {} }
}

function shouldFire(reminder) {
    if (!reminder.enabled) return false
    const now = new Date()
    const [rh, rm] = (reminder.time || '00:00').split(':').map(Number)
    if (now.getHours() !== rh) return false
    if (now.getMinutes() !== rm) return false
    const jsDay = now.getDay() === 0 ? 7 : now.getDay()
    const days = reminder.daysOfWeek || [1, 2, 3, 4, 5, 6, 7]
    return days.includes(jsDay)
}

function tryNativeNotification() {
    if (!('Notification' in window)) return false
    if (Notification.permission !== 'granted') return false
    try {
        new Notification('\u23F0 Promemoria Pressione', {
            body: '\u00C8 ora di misurare la pressione arteriosa.',
            icon: '/pressione/icon-192.png',
            badge: '/pressione/icon-192.png',
            tag: 'pressione-reminder',
            requireInteraction: true,
            silent: false
        })
        return true
    } catch { return false }
}

async function tick(username) {
    if (!username) return
    try {
        const reminders = await getReminders(username)
        for (const r of reminders) {
            if (shouldFire(r)) {
                tryNativeNotification()
                reminderAlert.value = { message: '\u23F0 \u00C8 ora di misurare la pressione arteriosa!', timestamp: Date.now() }
                vibrate()
                break
            }
        }
    } catch {}
}

export function startReminderScheduler(username) {
    stopReminderScheduler()
    if (!username) return
    ensurePermission().then(g => { if (g) console.log('[Reminder] Notifiche native abilitate') })
    schedulerInterval = setInterval(() => tick(username), 60_000)
    tick(username)
}

export function stopReminderScheduler() {
    if (schedulerInterval) { clearInterval(schedulerInterval); schedulerInterval = null }
    reminderAlert.value = null
}
