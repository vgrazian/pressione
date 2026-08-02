import { createClient } from '@supabase/supabase-js'

const supabaseUrl = String(
    (typeof import.meta !== 'undefined' && import.meta.env?.VITE_SUPABASE_URL) ||
    process.env.VITE_SUPABASE_URL ||
    ''
).trim()
const supabasePublishableKey = String(
    (typeof import.meta !== 'undefined' && import.meta.env?.VITE_SUPABASE_PUBLISHABLE_KEY) ||
    process.env.VITE_SUPABASE_PUBLISHABLE_KEY ||
    ''
).trim()

export const isSupabaseConfigured = Boolean(supabaseUrl && supabasePublishableKey)

export const supabase = isSupabaseConfigured
    ? createClient(supabaseUrl, supabasePublishableKey, {
        auth: {
            persistSession: true,
            autoRefreshToken: true,
            detectSessionInUrl: false
        }
    })
    : null

export function getSupabaseConfigStatus() {
    if (!supabaseUrl && !supabasePublishableKey) return 'missing_both'
    if (!supabaseUrl) return 'missing_url'
    if (!supabasePublishableKey) return 'missing_key'
    return 'ok'
}
