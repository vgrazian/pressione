import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * Auth session management tests.
 *
 * Tests focus on client-side validation logic and session state management
 * without requiring real Supabase connectivity.
 */

// Ensure localStorage polyfill is in place before any module imports
if (typeof localStorage === 'undefined' || !localStorage || typeof localStorage.setItem !== 'function') {
    const store = {}
    globalThis.localStorage = {
        getItem: (key) => store[key] || null,
        setItem: (key, value) => { store[key] = String(value) },
        removeItem: (key) => { delete store[key] },
        clear: () => { Object.keys(store).forEach(k => delete store[k]) },
        get length() { return Object.keys(store).length },
        key: (i) => Object.keys(store)[i] || null
    }
}

// Mock Supabase and DB dependencies before importing auth
vi.mock('@/services/supabaseClient.js', () => ({
    supabase: null,
    isSupabaseConfigured: true
}))

vi.mock('@/db/index.js', () => ({
    db: {},
    getSetting: vi.fn(() => Promise.resolve(null)),
    setSetting: vi.fn(() => Promise.resolve())
}))

vi.mock('@/services/supabaseTableAuth.js', () => ({
    loginWithTable: vi.fn(),
    createUserWithTable: vi.fn(),
    changePasswordWithTable: vi.fn(),
    getAllUsers: vi.fn(),
    setUserRoleWithTable: vi.fn(),
    deleteUserWithTable: vi.fn(),
    updateEmail: vi.fn(),
    createRecoveryToken: vi.fn(),
    resetPasswordWithToken: vi.fn(),
    adminResetPassword: vi.fn(),
    updateProfile: vi.fn(),
    getProfile: vi.fn(() => Promise.resolve({})),
    requestPasswordResetWithTable: vi.fn(),
    completePasswordRecoveryWithTable: vi.fn()
}))

const { useAuth, initAuth } = await import('@/services/auth.js')

const SESSION_KEY = 'pressione_session'

beforeEach(async () => {
    // Clear localStorage
    for (const key of Object.keys(localStorage)) {
        localStorage.removeItem(key)
    }
    // Reset auth state (module-level reactive state persists across tests)
    const { logout, isAuthenticated } = useAuth()
    if (isAuthenticated.value) {
        await logout()
    }
    vi.clearAllMocks()
})

describe('useAuth — session state', () => {
    it('initial state: not authenticated', () => {
        const { isAuthenticated } = useAuth()
        expect(isAuthenticated.value).toBe(false)
    })

    it('initial state: user is null', () => {
        const { user } = useAuth()
        expect(user.value).toBeNull()
    })

    it('initial state: isAuthReady is false before initAuth', () => {
        const { isAuthReady } = useAuth()
        expect(isAuthReady.value).toBe(false)
    })

    it('logout clears localStorage session', async () => {
        // Set up a fake session first
        const session = {
            username: 'test',
            email: 'test@test.com',
            role: 'user',
            expiresAt: new Date(Date.now() + 3600000).toISOString()
        }
        localStorage.setItem(SESSION_KEY, JSON.stringify(session))

        const { logout, isAuthenticated } = useAuth()
        await logout()

        expect(localStorage.getItem(SESSION_KEY)).toBeNull()
        expect(isAuthenticated.value).toBe(false)
    })

    it('supportsEmailReset is true when Supabase is configured', () => {
        const { supportsEmailReset } = useAuth()
        expect(supportsEmailReset).toBe(true)
    })
})

describe('initAuth — session restore', () => {
    it('sets isAuthReady to true', async () => {
        await initAuth()
        const { isAuthReady } = useAuth()
        expect(isAuthReady.value).toBe(true)
    })

    it('restores valid session from localStorage', async () => {
        const session = {
            username: 'testuser',
            email: 'test@test.com',
            role: 'user',
            expiresAt: new Date(Date.now() + 3600000).toISOString()
        }
        localStorage.setItem(SESSION_KEY, JSON.stringify(session))

        await initAuth()

        const { isAuthenticated, user } = useAuth()
        expect(isAuthenticated.value).toBe(true)
        expect(user.value.username).toBe('testuser')
        expect(user.value.email).toBe('test@test.com')
        expect(user.value.role).toBe('user')
    })

    it('does NOT restore expired session', async () => {
        const session = {
            username: 'olduser',
            email: 'old@test.com',
            role: 'user',
            expiresAt: new Date(Date.now() - 3600000).toISOString() // 1 hour ago
        }
        localStorage.setItem(SESSION_KEY, JSON.stringify(session))

        await initAuth()

        const { isAuthenticated } = useAuth()
        expect(isAuthenticated.value).toBe(false)
    })

    it('handles corrupted session data gracefully', async () => {
        localStorage.setItem(SESSION_KEY, '{invalid json!!!')

        await initAuth()

        const { isAuthReady, isAuthenticated } = useAuth()
        expect(isAuthReady.value).toBe(true)
        expect(isAuthenticated.value).toBe(false)
    })
})

describe('register — client-side validation', () => {
    it('throws when password is too short', async () => {
        const { register } = useAuth()
        await expect(register('user', 'a@b.com', '1234567')).rejects.toThrow(
            'La password deve essere di almeno 8 caratteri'
        )
    })

    it('throws when username is empty', async () => {
        const { register } = useAuth()
        await expect(register('', 'a@b.com', '12345678')).rejects.toThrow(
            'Tutti i campi sono obbligatori'
        )
    })

    it('throws when email is empty', async () => {
        const { register } = useAuth()
        await expect(register('user', '', '12345678')).rejects.toThrow(
            'Tutti i campi sono obbligatori'
        )
    })

    it('trims username and email before sending', async () => {
        const { createUserWithTable } = await import('@/services/supabaseTableAuth.js')
        createUserWithTable.mockResolvedValue({
            username: 'user',
            email: 'a@b.com',
            role: 'user'
        })

        const { register } = useAuth()
        await register('  User  ', '  A@B.COM  ', '12345678')

        expect(createUserWithTable).toHaveBeenCalledWith({
            username: 'user',
            email: 'a@b.com',
            password: '12345678',
            role: 'user'
        })
    })
})

describe('login — client-side validation', () => {
    it('throws when username is empty', async () => {
        const { login } = useAuth()
        await expect(login('', 'password')).rejects.toThrow(
            'Username e password sono obbligatori'
        )
    })

    it('throws when password is empty', async () => {
        const { login } = useAuth()
        await expect(login('user', '')).rejects.toThrow(
            'Username e password sono obbligatori'
        )
    })

    it('trims username before calling loginWithTable', async () => {
        const { loginWithTable } = await import('@/services/supabaseTableAuth.js')
        loginWithTable.mockResolvedValue({
            username: 'user',
            email: 'a@b.com',
            role: 'user'
        })

        const { login } = useAuth()
        await login('  User  ', 'password')

        expect(loginWithTable).toHaveBeenCalledWith({
            username: 'user',
            password: 'password'
        })
    })
})

describe('requestPasswordResetByEmail — validation', () => {
    it('throws when email is empty', async () => {
        const { requestPasswordResetByEmail } = useAuth()
        await expect(requestPasswordResetByEmail('')).rejects.toThrow(
            'Inserisci un indirizzo email valido'
        )
    })

    it('throws when email has no @', async () => {
        const { requestPasswordResetByEmail } = useAuth()
        await expect(requestPasswordResetByEmail('notanemail')).rejects.toThrow(
            'Inserisci un indirizzo email valido'
        )
    })

    it('calls requestPasswordResetWithTable with trimmed email', async () => {
        const { requestPasswordResetWithTable } = await import('@/services/supabaseTableAuth.js')
        requestPasswordResetWithTable.mockResolvedValue({
            email: 'test@test.com',
            resetUrl: 'https://example.com/reset?token=abc',
            expiresAt: null
        })

        const { requestPasswordResetByEmail } = useAuth()
        const result = await requestPasswordResetByEmail('  Test@Test.com  ')

        expect(requestPasswordResetWithTable).toHaveBeenCalledWith(
            expect.objectContaining({ email: 'test@test.com' })
        )
        expect(result.resetUrl).toBe('https://example.com/reset?token=abc')
    })
})

describe('completePasswordRecovery — validation', () => {
    it('throws when token is empty', async () => {
        const { completePasswordRecovery } = useAuth()
        await expect(
            completePasswordRecovery({ token: '', newPassword: '12345678', confirmPassword: '12345678' })
        ).rejects.toThrow('Token reset non valido')
    })

    it('throws when token is null', async () => {
        const { completePasswordRecovery } = useAuth()
        await expect(
            completePasswordRecovery({ token: null, newPassword: '12345678', confirmPassword: '12345678' })
        ).rejects.toThrow('Token reset non valido')
    })

    it('throws when new password is too short', async () => {
        const { completePasswordRecovery } = useAuth()
        await expect(
            completePasswordRecovery({ token: 'valid-token', newPassword: '1234567', confirmPassword: '1234567' })
        ).rejects.toThrow('La password deve essere di almeno 8 caratteri')
    })

    it('throws when passwords do not match', async () => {
        const { completePasswordRecovery } = useAuth()
        await expect(
            completePasswordRecovery({ token: 'valid-token', newPassword: '12345678', confirmPassword: '87654321' })
        ).rejects.toThrow('Le password non coincidono')
    })

    it('trims token before passing to service', async () => {
        const { completePasswordRecoveryWithTable } = await import('@/services/supabaseTableAuth.js')
        completePasswordRecoveryWithTable.mockResolvedValue({ username: 'test' })

        const { completePasswordRecovery } = useAuth()
        await completePasswordRecovery({
            token: '  token-with-spaces  ',
            newPassword: '12345678',
            confirmPassword: '12345678'
        })

        expect(completePasswordRecoveryWithTable).toHaveBeenCalledWith({
            token: 'token-with-spaces',
            newPassword: '12345678'
        })
    })
})
