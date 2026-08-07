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
    enableUserWithTable: vi.fn(),
    hardDeleteUserWithTable: vi.fn(),
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

describe('admin user management', () => {
    // Helper: authenticate as admin before each admin test
    const loginAsAdmin = async () => {
        const { loginWithTable } = await import('@/services/supabaseTableAuth.js')
        loginWithTable.mockResolvedValue({
            username: 'admin',
            email: 'admin@test.com',
            role: 'admin',
            birthDate: null,
            gender: null,
            profileCompleted: false,
            skipProfilePrompt: false
        })
        const { login } = useAuth()
        await login('admin', 'password')
    }

    it('fetchUsers calls getAllUsers and returns list', async () => {
        await loginAsAdmin()

        const { getAllUsers } = await import('@/services/supabaseTableAuth.js')
        const mockUsers = [
            { username: 'admin1', email: 'a@b.com', role: 'admin', disabled: false },
            { username: 'user1', email: 'u@b.com', role: 'user', disabled: false },
            { username: 'disabled1', email: 'd@b.com', role: 'user', disabled: true }
        ]
        getAllUsers.mockResolvedValue(mockUsers)

        const { fetchUsers } = useAuth()
        const users = await fetchUsers()

        expect(getAllUsers).toHaveBeenCalled()
        expect(users).toHaveLength(3)
        expect(users[0].username).toBe('admin1')
        expect(users[2].disabled).toBe(true)
    })

    it('fetchUsers throws for non-admin user', async () => {
        // Login as regular user (not admin)
        const { loginWithTable } = await import('@/services/supabaseTableAuth.js')
        loginWithTable.mockResolvedValue({
            username: 'user',
            email: 'user@test.com',
            role: 'user',
            birthDate: null,
            gender: null,
            profileCompleted: false,
            skipProfilePrompt: false
        })
        const { login } = useAuth()
        await login('user', 'password')

        const { fetchUsers } = useAuth()
        await expect(fetchUsers()).rejects.toThrow('Accesso non autorizzato')
    })

    it('updateUserRole calls setUserRoleWithTable', async () => {
        await loginAsAdmin()

        const { setUserRoleWithTable } = await import('@/services/supabaseTableAuth.js')
        setUserRoleWithTable.mockResolvedValue()

        const { updateUserRole } = useAuth()
        await updateUserRole('testuser', 'admin')

        expect(setUserRoleWithTable).toHaveBeenCalledWith({
            username: 'testuser',
            role: 'admin'
        })
    })

    it('deactivateUser calls deleteUserWithTable (soft delete)', async () => {
        await loginAsAdmin()

        const { deleteUserWithTable } = await import('@/services/supabaseTableAuth.js')
        deleteUserWithTable.mockResolvedValue()

        const { deactivateUser } = useAuth()
        await deactivateUser('testuser')

        expect(deleteUserWithTable).toHaveBeenCalledWith({
            username: 'testuser'
        })
    })

    it('activateUser calls enableUserWithTable', async () => {
        await loginAsAdmin()

        const { enableUserWithTable } = await import('@/services/supabaseTableAuth.js')
        enableUserWithTable.mockResolvedValue()

        const { activateUser } = useAuth()
        await activateUser('testuser')

        expect(enableUserWithTable).toHaveBeenCalledWith({
            username: 'testuser'
        })
    })

    it('hardDeleteUser calls hardDeleteUserWithTable (permanent delete)', async () => {
        await loginAsAdmin()

        const { hardDeleteUserWithTable } = await import('@/services/supabaseTableAuth.js')
        hardDeleteUserWithTable.mockResolvedValue()

        const { hardDeleteUser } = useAuth()
        await hardDeleteUser('testuser')

        expect(hardDeleteUserWithTable).toHaveBeenCalledWith({
            username: 'testuser'
        })
    })

    it('adminResetUserPassword calls adminResetPassword', async () => {
        await loginAsAdmin()

        const { adminResetPassword } = await import('@/services/supabaseTableAuth.js')
        adminResetPassword.mockResolvedValue()

        const { adminResetUserPassword } = useAuth()
        await adminResetUserPassword('testuser', 'newpassword123')

        expect(adminResetPassword).toHaveBeenCalledWith('admin', 'testuser', 'newpassword123')
    })

    it('adminUpdateUserEmail calls updateEmail', async () => {
        await loginAsAdmin()

        const { updateEmail } = await import('@/services/supabaseTableAuth.js')
        updateEmail.mockResolvedValue()

        const { adminUpdateUserEmail } = useAuth()
        await adminUpdateUserEmail('testuser', 'new@email.com')

        expect(updateEmail).toHaveBeenCalledWith({
            username: 'testuser',
            newEmail: 'new@email.com'
        })
    })

    it('register creates user with default role "user"', async () => {
        const { createUserWithTable } = await import('@/services/supabaseTableAuth.js')
        createUserWithTable.mockResolvedValue({
            username: 'newuser',
            email: 'new@test.com',
            role: 'user'
        })

        const { register } = useAuth()
        await register('newuser', 'new@test.com', 'password123')

        expect(createUserWithTable).toHaveBeenCalledWith({
            username: 'newuser',
            email: 'new@test.com',
            password: 'password123',
            role: 'user'
        })
    })

    it('register creates user with admin role when specified', async () => {
        const { createUserWithTable } = await import('@/services/supabaseTableAuth.js')
        createUserWithTable.mockResolvedValue({
            username: 'admin2',
            email: 'admin2@test.com',
            role: 'admin'
        })

        const { register } = useAuth()
        await register('admin2', 'admin2@test.com', 'password123', 'admin')

        expect(createUserWithTable).toHaveBeenCalledWith({
            username: 'admin2',
            email: 'admin2@test.com',
            password: 'password123',
            role: 'admin'
        })
    })
})
