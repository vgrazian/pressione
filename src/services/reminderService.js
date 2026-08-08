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
    if (navigator.vibrate) { try { navigator.vibrate([200, 100, 200]) } catch { } }
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

const REMINDER_MESSAGES = {
    morning: [
        "Bip bip! È ora di stringerti il braccio prima che il traffico ti stringa lo stomaco. Misuriamoci!",
        "Non toccare quella tazzina! Prima scopriamo se il tuo cuore sta già andando a giri massimi da solo.",
        "È lunedì. Sappiamo che non vorresti vedere quel numero, ma dobbiamo farlo. Coraggio.",
        "Il buongiorno si vede dal bracciale. Forza, misurati!"
    ],
    afternoon: [
        "Abbiamo sentito quel sospiro da qui. Posa la tastiera, prendi lo sfigmomanometro e calmati.",
        "La call con i colleghi è finita? Perfetto, vediamo quanti danni ha fatto alle tue arterie.",
        "La tua pazienza è quasi a zero, ma come sta la massima? Scoprilo ora (se hai il coraggio)."
    ],
    evening: [
        "Ti sei finalmente seduto? Ottimo, è il momento perfetto per farti venire un po' d'ansia con i numeri della sera.",
        "Quella pizza era squisita, vero? Ora vieni a pagare il conto in millimetri di mercurio.",
        "Un ultimo controllo prima di dormire. Giusto per assicurarsi che tu non stia sognando l'ufficio delle tasse."
    ],
    nag: [
        "Ci stai ignorando. Guarda che se non la misuri tu, la pressione sale lo stesso per il dispetto!",
        "Sono tre giorni che non ti misuri. Se non apri l'app, mandiamo una notifica di insulti direttamente al tuo cardiologo."
    ]
}

function pickMessage() {
    const h = new Date().getHours()
    let pool
    if (h < 12) pool = REMINDER_MESSAGES.morning
    else if (h < 18) pool = REMINDER_MESSAGES.afternoon
    else pool = REMINDER_MESSAGES.evening
    return pool[Math.floor(Math.random() * pool.length)]
}

function tryNativeNotification() {
    if (!('Notification' in window)) return false
    if (Notification.permission !== 'granted') return false
    try {
        new Notification('\u23F0 Promemoria IperTeso', {
            body: pickMessage(),
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
                reminderAlert.value = { message: '\u23F0 ' + pickMessage(), timestamp: Date.now() }
                vibrate()
                break
            }
        }
    } catch { }
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
