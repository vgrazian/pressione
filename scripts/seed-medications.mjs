// Seed farmaci per gli utenti di test direttamente su Supabase (usa la chiave anon).
// Uso: node scripts/seed-medications.mjs [username1 username2 ...]
// Default: semina i farmaci per 'bot' (utente di test dell'emulatore).
import { createClient } from '@supabase/supabase-js'
import { readFileSync } from 'fs'
import { resolve } from 'path'

// Carica .env (parser minimale, nessuna dipendenza esterna)
const env = {}
for (const line of readFileSync(resolve('.env'), 'utf8').split('\n')) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/)
    if (m) env[m[1]] = m[2].replace(/^["']|["']$/g, '')
}

const SUPABASE_URL = env.VITE_SUPABASE_URL
const SUPABASE_KEY = env.VITE_SUPABASE_PUBLISHABLE_KEY
if (!SUPABASE_URL || !SUPABASE_KEY) {
    console.error('ERRORE: Imposta VITE_SUPABASE_URL e VITE_SUPABASE_PUBLISHABLE_KEY in .env')
    process.exit(1)
}

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY)

const users = process.argv.slice(2).length ? process.argv.slice(2) : ['bot']

const daysAgo = (d) => new Date(Date.now() - d * 86400000).toISOString()

// Timeline terapia realistica: date di inizio/fine entro gli ultimi 30 giorni,
// così i marker di milestone sono visibili nel grafico insieme alle letture.
const MEDS = [
    { name: 'Losartan', activeIngredient: 'Losartan potassico', dosage: '50 mg', frequency: '1 volta al giorno', notes: 'al mattino', startDaysAgo: 30, endDaysAgo: null },
    { name: 'Amlodipina', activeIngredient: 'Amlodipina besilato', dosage: '5 mg', frequency: '1 volta al giorno', notes: 'alla sera', startDaysAgo: 20, endDaysAgo: null },
    { name: 'Bisoprololo', activeIngredient: 'Bisoprololo fumarato', dosage: '2,5 mg', frequency: '1 volta al giorno', notes: 'al mattino', startDaysAgo: 30, endDaysAgo: 10 },
    { name: 'Ramipril', activeIngredient: 'Ramipril', dosage: '5 mg', frequency: '1 volta al giorno', notes: '', startDaysAgo: 12, endDaysAgo: 3 }
]

async function main() {
    for (const username of users) {
        console.log(`Farmaci per ${username}...`)
        // Rimuove i farmaci esistenti per evitare duplicati
        const { error: delErr } = await supabase.from('medications').delete().eq('username', username)
        if (delErr) console.error(`  Avviso (delete):`, delErr.message)

        for (const m of MEDS) {
            const row = {
                id: crypto.randomUUID(),
                username,
                name: m.name,
                active_ingredient: m.activeIngredient,
                dosage: m.dosage,
                frequency: m.frequency,
                notes: m.notes,
                start_date: daysAgo(m.startDaysAgo),
                end_date: m.endDaysAgo != null ? daysAgo(m.endDaysAgo) : null,
                created_at: new Date().toISOString(),
                updated_at: new Date().toISOString()
            }
            const { error } = await supabase.from('medications').insert(row)
            if (error) {
                console.error(`  ERRORE ${m.name}:`, error.message)
            } else {
                const fine = m.endDaysAgo != null ? `fine ${m.endDaysAgo}g fa` : 'in corso'
                console.log(`  OK ${m.name} (inizio ${m.startDaysAgo}g fa, ${fine})`)
            }
        }
    }
    console.log('\nOperazione completata.')
}

main().catch((e) => {
    console.error(e)
    process.exit(1)
})
