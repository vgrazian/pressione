// Script per creare gli utenti seed su Supabase
// Uso: node scripts/provision-users.mjs
// Richiede SUPABASE_URL e SUPABASE_SECRET_KEY nell'ambiente

import { createClient } from '@supabase/supabase-js'
import { createHash } from 'crypto'

const SUPABASE_URL = process.env.SUPABASE_URL
const SUPABASE_SECRET_KEY = process.env.SUPABASE_SECRET_KEY

if (!SUPABASE_URL || !SUPABASE_SECRET_KEY) {
    console.error('ERRORE: Imposta SUPABASE_URL e SUPABASE_SECRET_KEY')
    process.exit(1)
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SECRET_KEY)

function hashPassword(password) {
    return createHash('sha256').update(password).digest('hex')
}

const DEFAULT_PASSWORD = 'Pressione2026!'
const passwordHash = hashPassword(DEFAULT_PASSWORD)

const SEED_USERS = [
    { username: 'nadia', email: 'nadia@pressione.local', role: 'admin' },
    { username: 'roberto', email: 'roberto@pressione.local', role: 'user' },
    { username: 'barbara', email: 'barbara@pressione.local', role: 'user' },
    { username: 'valerio', email: 'valerio@pressione.local', role: 'admin' },
    { username: 'marco', email: 'marco@pressione.local', role: 'user' },
    { username: 'rita', email: 'rita@pressione.local', role: 'user' },
    { username: 'anna', email: 'anna@pressione.local', role: 'user' }
]

async function main() {
    console.log('🔐 Creazione utenti seed...\n')

    for (const user of SEED_USERS) {
        const { data, error } = await supabase
            .from('users')
            .upsert({
                username: user.username,
                email: user.email,
                password_hash: passwordHash,
                role: user.role,
        disabled: false,
        } else {
            console.log(`✅ ${user.username} (${user.role}) - Password: ${DEFAULT_PASSWORD}`)
        }
    }

    console.log('\n✨ Operazione completata!')
    console.log(`Password default per tutti: ${DEFAULT_PASSWORD}`)
}

main().catch(console.error)
