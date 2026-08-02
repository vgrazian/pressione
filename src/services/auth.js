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
    deleteUserWithTable
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
        fetchUsers,
        updateUserRole,
        deactivateUser,
        refreshSession
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
                    role: session.role
                }
                state.isAuthenticated = true
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
                        role: session.role
                    }
                    state.isAuthenticated = true
                    localStorage.setItem(SESSION_KEY, dbSession)
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
        expiresAt: expiresAt.toISOString()
    }

    const sessionJson = JSON.stringify(session)
    localStorage.setItem(SESSION_KEY, sessionJson)
    setSetting('_system', 'session', sessionJson).catch(() => { })

    state.user = {
        username: userData.username,
        email: userData.email,
        role: userData.role
    }
    state.isAuthenticated = true

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
    if (!state.isAuthenticated) return
    const session = JSON.parse(localStorage.getItem(SESSION_KEY) || '{}')
    session.expiresAt = new Date(Date.now() + state.sessionTtlMinutes * 60 * 1000).toISOString()
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
