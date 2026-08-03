// Table-based authentication service (mirrors MediTrace pattern)
import { supabase, isSupabaseConfigured } from './supabaseClient'
import { db } from '../db'

/**
 * Hash password using SHA-256
 */
async function hashPassword(password) {
    const encoder = new TextEncoder()
    const data = encoder.encode(password)
    const hashBuffer = await crypto.subtle.digest('SHA-256', data)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

/**
 * Verify password against stored hash
 */
async function verifyPassword(password, storedHash) {
    const hash = await hashPassword(password)
    return hash === storedHash
}

/**
 * Create a new user
 */
export async function createUserWithTable({ username, email, password, role = 'user' }) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

    const passwordHash = await hashPassword(password)

    const { data, error } = await supabase
        .from('users')
        .insert({
            username: username.toLowerCase().trim(),
            email: email.toLowerCase().trim(),
            password_hash: passwordHash,
            role
        })
        .select('id, username, email, role, disabled')
        .single()

    if (error) {
        if (error.code === '23505') throw new Error('Username o email già esistente')
        throw new Error('Errore nella creazione utente: ' + error.message)
    }

    return data
}

/**
 * Login with username and password
 */
export async function loginWithTable({ username, password }) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

    const { data: users, error } = await supabase
        .from('users')
        .select('*')
        .eq('username', username.toLowerCase().trim())
        .eq('disabled', false)
        .limit(1)

    if (error) throw new Error('Errore di autenticazione: ' + error.message)
    if (!users || users.length === 0) throw new Error('Username o password non validi')

    const user = users[0]
    const valid = await verifyPassword(password, user.password_hash)
    if (!valid) throw new Error('Username o password non validi')

    return {
        username: user.username,
        email: user.email,
        role: user.role,
        birthDate: user.birth_date || null,
        gender: user.gender || null,
        profileCompleted: user.profile_completed || false,
        skipProfilePrompt: user.skip_profile_prompt || false
    }
}

/**
 * Change password
 */
export async function changePasswordWithTable({ username, currentPassword, newPassword }) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

    const { data: users, error } = await supabase
        .from('users')
        .select('*')
        .eq('username', username)
        .limit(1)

    if (error || !users?.length) throw new Error('Utente non trovato')

    const valid = await verifyPassword(currentPassword, users[0].password_hash)
    if (!valid) throw new Error('Password corrente non valida')

    const newHash = await hashPassword(newPassword)
    const { error: updateError } = await supabase
        .from('users')
        .update({ password_hash: newHash, updated_at: new Date().toISOString() })
        .eq('username', username)

    if (updateError) throw new Error('Errore nel cambio password')
}

/**
 * Set user role (admin only)
 */
export async function setUserRoleWithTable({ username, role }) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

    const { error } = await supabase
        .from('users')
        .update({ role, updated_at: new Date().toISOString() })
        .eq('username', username)

    if (error) throw new Error('Errore nella modifica del ruolo')
}

/**
 * Delete user (soft-delete)
 */
export async function deleteUserWithTable({ username }) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

    const { error } = await supabase
        .from('users')
        .update({ disabled: true, updated_at: new Date().toISOString() })
        .eq('username', username)

    if (error) throw new Error('Errore nella disattivazione utente')
}

/**
 * Get all users (admin only)
 */
export async function getAllUsers() {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

    const { data, error } = await supabase
        .from('users')
        .select('id, username, email, role, disabled, created_at')
        .order('username', { ascending: true })
    if (error) throw new Error('Errore nel recupero utenti')
    return data
}

/**
 * Update email
 */
export async function updateEmail({ username, newEmail }) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')
    const { error } = await supabase.rpc('update_user_email', {
        p_username: username,
        p_new_email: newEmail
    })
    if (error) throw new Error('Errore aggiornamento email: ' + error.message)
}

/**
 * Create a password recovery token
 */
export async function createRecoveryToken(username) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')
    const { data, error } = await supabase.rpc('create_recovery_token', {
        p_username: username.toLowerCase().trim()
    })
    if (error) throw new Error('Errore: ' + error.message)
    return data
}

/**
 * Reset password using recovery token
 */
export async function resetPasswordWithToken(token, newPassword) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')
    const { error } = await supabase.rpc('reset_password_with_token', {
        p_token: token,
        p_new_password: newPassword
    })
    if (error) throw new Error('Errore: ' + error.message)
}

/**
 * Admin reset password for any user
 */
export async function adminResetPassword(adminUsername, targetUsername, newPassword) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')
    const { error } = await supabase.rpc('admin_reset_password', {
        p_admin_username: adminUsername,
        p_target_username: targetUsername,
        p_new_password: newPassword
    })
    if (error) throw new Error('Errore: ' + error.message)
}

/**
 * Update user profile (age, gender, flags).
 * Stores in settings table to avoid PostgREST schema cache issues.
 */
export async function updateProfile(username, { birthDate, gender, profileCompleted, skipProfilePrompt }) {
    if (!isSupabaseConfigured) throw new Error('Supabase non configurato')

    // First, get existing profile
    const { data: existing } = await supabase.from('settings')
        .select('value')
        .eq('username', username)
        .eq('key', '_profile')
        .maybeSingle()

    const profile = existing ? JSON.parse(existing.value) : {}

    if (birthDate !== undefined) profile.birthDate = birthDate
    if (gender !== undefined) profile.gender = gender
    if (profileCompleted !== undefined) profile.profileCompleted = profileCompleted
    if (skipProfilePrompt !== undefined) profile.skipProfilePrompt = skipProfilePrompt

    const { error } = await supabase.from('settings').upsert({
        username,
        key: '_profile',
        value: JSON.stringify(profile),
        updated_at: new Date().toISOString()
    })
    if (error) throw new Error('Errore aggiornamento profilo: ' + error.message)

    // Also try to update users table (best effort — may fail due to PostgREST cache)
    try {
        const userUpdates = { updated_at: new Date().toISOString() }
        if (birthDate !== undefined) userUpdates.birth_date = birthDate
        if (gender !== undefined) userUpdates.gender = gender
        if (profileCompleted !== undefined) userUpdates.profile_completed = profileCompleted
        if (skipProfilePrompt !== undefined) userUpdates.skip_profile_prompt = skipProfilePrompt
        await supabase.from('users').update(userUpdates).eq('username', username)
    } catch { /* users table update is best-effort */ }
}

/**
 * Get user profile from settings table
 */
export async function getProfile(username) {
    if (!isSupabaseConfigured) return {}
    const { data } = await supabase.from('settings')
        .select('value')
        .eq('username', username)
        .eq('key', '_profile')
        .maybeSingle()
    return data ? JSON.parse(data.value) : {}
}
