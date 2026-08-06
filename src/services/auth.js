// Reactive auth state management
import { reactive, readonly, toRefs } from 'vue'
import { db, getSetting, setSetting } from '../db'
import { supabase, isSupabaseConfigured } from './supabaseClient'
import {
    loginWithTable,
    createUserWithTable,
    changePasswordWithTable,
    getAllUsers,
    setUserRoleWithTable,
    deleteUserWithTable,
    enableUserWithTable,
    hardDeleteUserWithTable,
    updateEmail,
    createRecoveryToken,
    resetPasswordWithToken,
    adminResetPassword,
    updateProfile,
    getProfile,
    requestPasswordResetWithTable,
    completePasswordRecoveryWithTable
} from './supabaseTableAuth'

const SESSION_KEY = 'pressione_session'
const DEFAULT_TTL_MINUTES = 480 // 8 hours

const state = reactive({
    user: null,
    isAuthenticated: false,
    isAuthReady: false,
    sessionTtlMinutes: DEFAULT_TTL_MINUTES
})

export function useAuth() {
    return {
        ...toRefs(readonly(state)),
        login,
        logout,
        register,
        changePassword,
        updateUserEmail,
        requestPasswordReset,
        completePasswordReset,
        requestPasswordResetByEmail,
        completePasswordRecovery,
        adminResetUserPassword,
        fetchUsers,
        updateUserRole,
        deactivateUser,
        activateUser,
        hardDeleteUser,
        refreshSession,
        updateUserProfile,
        supportsEmailReset: isSupabaseConfigured
    }
}

/**
 * Initialize auth state from stored session
 */
export async function initAuth() {
    try {
        // Try to restore from localStorage
        const stored = localStorage.getItem(SESSION_KEY)
        if (stored) {
            const session = JSON.parse(stored)
            const expiresAt = new Date(session.expiresAt)
            if (expiresAt > new Date()) {
                state.user = {
                    username: session.username,
                    email: session.email,
                    role: session.role,
                    birthDate: session.birthDate || null,
                    gender: session.gender || null,
                    profileCompleted: session.profileCompleted || false,
                    skipProfilePrompt: session.skipProfilePrompt || false,
                    firstName: session.firstName || '',
                    lastName: session.lastName || '',
                    fiscalCode: session.fiscalCode || '',
                    phone: session.phone || '',
                    street: session.street || '',
                    streetNumber: session.streetNumber || '',
                    city: session.city || '',
                    postalCode: session.postalCode || ''
                }
                state.isAuthenticated = true
                // Refresh profile from settings (bypasses PostgREST cache) — with 5s timeout
                try {
                    const p = await Promise.race([
                        getProfile(session.username),
                        new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), 5000))
                    ])
                    if (p) Object.assign(state.user, {
                        birthDate: p.birthDate ?? state.user.birthDate,
                        gender: p.gender ?? state.user.gender,
                        profileCompleted: p.profileCompleted ?? state.user.profileCompleted,
                        skipProfilePrompt: p.skipProfilePrompt ?? state.user.skipProfilePrompt,
                        firstName: p.firstName ?? state.user.firstName,
                        lastName: p.lastName ?? state.user.lastName,
                        fiscalCode: p.fiscalCode ?? state.user.fiscalCode,
                        phone: p.phone ?? state.user.phone,
                        street: p.street ?? state.user.street,
                        streetNumber: p.streetNumber ?? state.user.streetNumber,
                        city: p.city ?? state.user.city,
                        postalCode: p.postalCode ?? state.user.postalCode
                    })
                    refreshSession()
                } catch { /* best effort */ }
            } else {
                localStorage.removeItem(SESSION_KEY)
            }
        }

        // Also try to restore from IndexedDB as fallback
        if (!state.isAuthenticated) {
            const dbSession = await getSetting('_system', 'session', null)
            if (dbSession) {
                const session = JSON.parse(dbSession)
                const expiresAt = new Date(session.expiresAt)
                if (expiresAt > new Date()) {
                    state.user = {
                        username: session.username,
                        email: session.email,
                        role: session.role,
                        birthDate: session.birthDate || null,
                        gender: session.gender || null,
                        profileCompleted: session.profileCompleted || false,
                        skipProfilePrompt: session.skipProfilePrompt || false,
                        firstName: session.firstName || '',
                        lastName: session.lastName || '',
                        fiscalCode: session.fiscalCode || '',
                        phone: session.phone || '',
                        street: session.street || '',
                        streetNumber: session.streetNumber || '',
                        city: session.city || '',
                        postalCode: session.postalCode || ''
                    }
                    state.isAuthenticated = true
                    localStorage.setItem(SESSION_KEY, dbSession)
                    try {
                        const p = await Promise.race([
                            getProfile(session.username),
                            new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), 5000))
                        ])
                        if (p) Object.assign(state.user, {
                            birthDate: p.birthDate ?? state.user.birthDate,
                            gender: p.gender ?? state.user.gender,
                            profileCompleted: p.profileCompleted ?? state.user.profileCompleted,
                            skipProfilePrompt: p.skipProfilePrompt ?? state.user.skipProfilePrompt,
                            firstName: p.firstName ?? state.user.firstName,
                            lastName: p.lastName ?? state.user.lastName,
                            fiscalCode: p.fiscalCode ?? state.user.fiscalCode,
                            phone: p.phone ?? state.user.phone,
                            street: p.street ?? state.user.street,
                            streetNumber: p.streetNumber ?? state.user.streetNumber,
                            city: p.city ?? state.user.city,
                            postalCode: p.postalCode ?? state.user.postalCode
                        })
                        refreshSession()
                    } catch { /* best effort */ }
                }
            }
        }
    } catch (e) {
        console.warn('Session restore failed:', e)
    } finally {
        state.isAuthReady = true
    }
}

/**
 * Login
 */
async function login(username, password) {
    if (!username || !password) {
        throw new Error('Username e password sono obbligatori')
    }

    const user = await loginWithTable({
        username: username.toLowerCase().trim(),
        password
    })

    return createSession(user)
}

/**
 * Register new user
 */
async function register(username, email, password, role = 'user') {
    if (!username || !email || !password) {
        throw new Error('Tutti i campi sono obbligatori')
    }
    if (password.length < 8) {
        throw new Error('La password deve essere di almeno 8 caratteri')
    }

    return createUserWithTable({
        username: username.toLowerCase().trim(),
        email: email.toLowerCase().trim(),
        password,
        role
    })
}

/**
 * Create session after successful login
 */
function createSession(userData) {
    const expiresAt = new Date(Date.now() + state.sessionTtlMinutes * 60 * 1000)
    const session = {
        username: userData.username,
        email: userData.email,
        role: userData.role,
        birthDate: userData.birthDate || null,
        gender: userData.gender || null,
        profileCompleted: userData.profileCompleted || false,
        skipProfilePrompt: userData.skipProfilePrompt || false,
        firstName: userData.firstName || '',
        lastName: userData.lastName || '',
        fiscalCode: userData.fiscalCode || '',
        phone: userData.phone || '',
        street: userData.street || '',
        streetNumber: userData.streetNumber || '',
        city: userData.city || '',
        postalCode: userData.postalCode || '',
        expiresAt: expiresAt.toISOString()
    }

    const sessionJson = JSON.stringify(session)
    localStorage.setItem(SESSION_KEY, sessionJson)
    setSetting('_system', 'session', sessionJson).catch(() => { })

    state.user = {
        username: userData.username,
        email: userData.email,
        role: userData.role,
        birthDate: userData.birthDate || null,
        gender: userData.gender || null,
        profileCompleted: userData.profileCompleted || false,
        skipProfilePrompt: userData.skipProfilePrompt || false,
        firstName: userData.firstName || '',
        lastName: userData.lastName || '',
        fiscalCode: userData.fiscalCode || '',
        phone: userData.phone || '',
        street: userData.street || '',
        streetNumber: userData.streetNumber || '',
        city: userData.city || '',
        postalCode: userData.postalCode || ''
    }
    state.isAuthenticated = true

    // Async: fetch profile from settings table (bypasses PostgREST cache)
    getProfile(userData.username).then(profile => {
        if (profile && Object.keys(profile).length > 0) {
            if (profile.birthDate !== undefined) state.user.birthDate = profile.birthDate
            if (profile.gender !== undefined) state.user.gender = profile.gender
            if (profile.profileCompleted !== undefined) state.user.profileCompleted = profile.profileCompleted
            if (profile.skipProfilePrompt !== undefined) state.user.skipProfilePrompt = profile.skipProfilePrompt
            if (profile.firstName !== undefined) state.user.firstName = profile.firstName
            if (profile.lastName !== undefined) state.user.lastName = profile.lastName
            if (profile.fiscalCode !== undefined) state.user.fiscalCode = profile.fiscalCode
            if (profile.phone !== undefined) state.user.phone = profile.phone
            if (profile.street !== undefined) state.user.street = profile.street
            if (profile.streetNumber !== undefined) state.user.streetNumber = profile.streetNumber
            if (profile.city !== undefined) state.user.city = profile.city
            if (profile.postalCode !== undefined) state.user.postalCode = profile.postalCode
            refreshSession()
        }
    }).catch(() => { /* best effort */ })

    return state.user
}

/**
 * Logout
 */
async function logout() {
    localStorage.removeItem(SESSION_KEY)
    await setSetting('_system', 'session', '').catch(() => { })
    state.user = null
    state.isAuthenticated = false
}

/**
 * Change password
 */
async function changePassword(currentPassword, newPassword) {
    if (!state.user) throw new Error('Non autenticato')
    await changePasswordWithTable({
        username: state.user.username,
        currentPassword,
        newPassword
    })
}

/**
 * Refresh session TTL
 */
function refreshSession() {
    if (!state.isAuthenticated || !state.user) return
    const session = JSON.parse(localStorage.getItem(SESSION_KEY) || '{}')
    session.expiresAt = new Date(Date.now() + state.sessionTtlMinutes * 60 * 1000).toISOString()
    // Persist current profile flags so they survive app restarts
    session.birthDate = state.user.birthDate || null
    session.gender = state.user.gender || null
    session.profileCompleted = state.user.profileCompleted || false
    session.skipProfilePrompt = state.user.skipProfilePrompt || false
    session.firstName = state.user.firstName || ''
    session.lastName = state.user.lastName || ''
    session.fiscalCode = state.user.fiscalCode || ''
    session.phone = state.user.phone || ''
    session.street = state.user.street || ''
    session.streetNumber = state.user.streetNumber || ''
    session.city = state.user.city || ''
    session.postalCode = state.user.postalCode || ''
    const sessionJson = JSON.stringify(session)
    localStorage.setItem(SESSION_KEY, sessionJson)
    setSetting('_system', 'session', sessionJson).catch(() => { })
}

/**
 * Fetch all users (admin only)
 */
async function fetchUsers() {
    if (!state.user || state.user.role !== 'admin') {
        throw new Error('Accesso non autorizzato')
    }
    return getAllUsers()
}

/**
 * Update user role (admin only)
 */
async function updateUserRole(username, role) {
    await setUserRoleWithTable({ username, role })
}

/**
 * Deactivate user (admin only)
 */
async function deactivateUser(username) {
    await deleteUserWithTable({ username })
}

/**
 * Re-activate a disabled user (admin only)
 */
async function activateUser(username) {
    await enableUserWithTable({ username })
}

/**
 * Hard-delete a user permanently (admin only)
 */
async function hardDeleteUser(username) {
    await hardDeleteUserWithTable({ username })
}

/**
 * Update current user's email
 */
async function updateUserEmail(newEmail) {
    if (!state.user) throw new Error('Non autenticato')
    await updateEmail({ username: state.user.username, newEmail })
    state.user.email = newEmail.toLowerCase().trim()
    // Update stored session
    const stored = JSON.parse(localStorage.getItem(SESSION_KEY) || '{}')
    stored.email = newEmail.toLowerCase().trim()
    localStorage.setItem(SESSION_KEY, JSON.stringify(stored))
}

/**
 * Update current user's profile (age, gender, flags)
 */
async function updateUserProfile({ birthDate, gender, profileCompleted, skipProfilePrompt, firstName, lastName, fiscalCode, phone, street, streetNumber, city, postalCode }) {
    if (!state.user) throw new Error('Non autenticato')
    await updateProfile(state.user.username, { birthDate, gender, profileCompleted, skipProfilePrompt, firstName, lastName, fiscalCode, phone, street, streetNumber, city, postalCode })
    if (birthDate !== undefined) state.user.birthDate = birthDate
    if (gender !== undefined) state.user.gender = gender
    if (profileCompleted !== undefined) state.user.profileCompleted = profileCompleted
    if (skipProfilePrompt !== undefined) state.user.skipProfilePrompt = skipProfilePrompt
    if (firstName !== undefined) state.user.firstName = firstName
    if (lastName !== undefined) state.user.lastName = lastName
    if (fiscalCode !== undefined) state.user.fiscalCode = fiscalCode
    if (phone !== undefined) state.user.phone = phone
    if (street !== undefined) state.user.street = street
    if (streetNumber !== undefined) state.user.streetNumber = streetNumber
    if (city !== undefined) state.user.city = city
    if (postalCode !== undefined) state.user.postalCode = postalCode
    refreshSession()
}

/**
 * Request password recovery token
 */
async function requestPasswordReset(username) {
    return createRecoveryToken(username)
}

/**
 * Complete password reset with token
 */
async function completePasswordReset(token, newPassword) {
    await resetPasswordWithToken(token, newPassword)
}

/**
 * Admin resets another user's password
 */
async function adminResetUserPassword(targetUsername, newPassword) {
    if (!state.user || state.user.role !== 'admin') throw new Error('Accesso non autorizzato')
    await adminResetPassword(state.user.username, targetUsername, newPassword)
}

/**
 * Request password reset by email (new flow).
 * Returns { resetUrl, emailSent }. When no email API is configured,
 * the reset URL is returned directly so the UI can display it.
 */
async function requestPasswordResetByEmail(email) {
    if (!email || !email.includes('@')) {
        throw new Error('Inserisci un indirizzo email valido')
    }

    const payload = await requestPasswordResetWithTable({
        email: email.toLowerCase().trim(),
        redirectTo: window.location.origin + '/#/reset-password',
        resetTtlMinutes: 30
    })

    // No email API configured — return the URL for display in the UI
    return {
        resetUrl: payload.resetUrl,
        emailSent: false
    }
}

/**
 * Complete password recovery using a token from the reset link.
 */
async function completePasswordRecovery({ token, newPassword, confirmPassword }) {
    if (!token) throw new Error('Token reset non valido')
    if (!newPassword || newPassword.length < 8) {
        throw new Error('La password deve essere di almeno 8 caratteri')
    }
    if (newPassword !== confirmPassword) {
        throw new Error('Le password non coincidono')
    }

    await completePasswordRecoveryWithTable({
        token: String(token).trim(),
        newPassword
    })
}
