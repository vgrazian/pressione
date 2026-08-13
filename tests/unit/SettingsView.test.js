import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'

// Mock vue-router
vi.mock('vue-router', () => ({
    useRoute: () => ({ path: '/settings' }),
    useRouter: () => ({ push: vi.fn() })
}))

// Mock useAuth
vi.mock('@/services/auth.js', () => ({
    useAuth: () => ({
        user: { value: { username: 'test', email: 'test@test.com', role: 'user', birthDate: null, gender: null } },
        changePassword: vi.fn(),
        updateUserEmail: vi.fn(),
        updateUserProfile: vi.fn().mockResolvedValue()
    })
}))

// Mock dataService
vi.mock('@/services/dataService.js', () => ({
    getReminders: vi.fn().mockResolvedValue([]),
    upsertReminder: vi.fn(),
    deleteReminder: vi.fn(),
    getReadings: vi.fn().mockResolvedValue([]),
    exportCSV: vi.fn(),
    importCSV: vi.fn(),
    generateTestData: vi.fn(),
    refreshFromServer: vi.fn(),
    backupData: vi.fn(),
    restoreData: vi.fn(),
    deleteAllReadings: vi.fn(),
    getMedications: vi.fn().mockResolvedValue([]),
    upsertMedication: vi.fn(),
    deleteMedication: vi.fn(),
    stopMedication: vi.fn()
}))

// Mock other services
vi.mock('@/services/rbac.js', () => ({
    isAdmin: vi.fn(() => false)
}))
vi.mock('@/services/i18n.js', () => ({
    useI18n: () => ({
        t: (k) => k,
        setLang: vi.fn(),
        currentLang: { value: 'it' },
        availableLangs: ['it', 'en']
    })
}))
vi.mock('@/services/keepAlive.js', () => ({
    isKeepAliveEnabled: vi.fn().mockResolvedValue(false),
    startKeepAlive: vi.fn(),
    stopKeepAlive: vi.fn(),
    isKeepAliveActive: vi.fn(() => false),
    getStorageInfo: vi.fn().mockResolvedValue(null),
    formatBytes: vi.fn(v => v || 'N/D')
}))
vi.mock('@/services/pwaInstall.js', () => ({
    promptInstall: vi.fn(),
    isInstallPromptAvailable: vi.fn(() => false),
    isIOS: vi.fn(() => false),
    isStandalone: vi.fn(() => true)
}))
vi.mock('@/services/swUpdate.js', () => ({
    useSWUpdate: () => ({ forceClearCache: vi.fn() })
}))
vi.mock('@/services/version.js', () => ({
    APP_VERSION: '1.0.0',
    BUILD_NUMBER: 'abc1234',
    BUILD_TIME: '2026-01-01T00:00:00Z'
}))
vi.mock('@/services/timeBands.js', () => ({
    getUserBands: vi.fn().mockResolvedValue({
        morning: { key: 'morning', label: 'Mattina', icon: '🌅', start: 6, end: 12 },
        afternoon: { key: 'afternoon', label: 'Pomeriggio', icon: '☀️', start: 12, end: 18 },
        evening: { key: 'evening', label: 'Sera', icon: '🌆', start: 18, end: 22 },
        night: { key: 'night', label: 'Notte', icon: '🌙', start: 22, end: 6 }
    }),
    saveUserBands: vi.fn(),
    getDefaultBands: vi.fn(() => ({
        morning: { key: 'morning', label: 'Mattina', icon: '🌅', start: 6, end: 12 },
        afternoon: { key: 'afternoon', label: 'Pomeriggio', icon: '☀️', start: 12, end: 18 },
        evening: { key: 'evening', label: 'Sera', icon: '🌆', start: 18, end: 22 },
        night: { key: 'night', label: 'Notte', icon: '🌙', start: 22, end: 6 }
    }))
}))

describe('SettingsView — decomposition', () => {
    it('renders CollapsibleSection for Anagrafica, Password, Reminders, Time Bands, Advanced Tools', async () => {
        const { default: SettingsView } = await import('@/views/SettingsView.vue')
        const wrapper = mount(SettingsView, {
            global: { stubs: { 'router-link': true, 'AppIcon': true, 'TimeBandSlider': true } }
        })

        await wrapper.vm.$nextTick()

        const sections = wrapper.findAllComponents({ name: 'CollapsibleSection' })
        expect(sections.length).toBeGreaterThanOrEqual(5)

        const titles = sections.map(s => s.props('title'))
        expect(titles.some(t => t && t.includes('Anagrafica'))).toBe(true)
        expect(titles.some(t => t && t.includes('change_password'))).toBe(true)
        expect(titles.some(t => t && t.includes('reminders'))).toBe(true)
        expect(titles.some(t => t && t.includes('Fasce Orarie'))).toBe(true)
        expect(titles.some(t => t && t.includes('Strumenti avanzati'))).toBe(true)
    })

    it('Account section is a regular card (not collapsible)', async () => {
        const { default: SettingsView } = await import('@/views/SettingsView.vue')
        const wrapper = mount(SettingsView, {
            global: { stubs: { 'router-link': true, 'AppIcon': true, 'TimeBandSlider': true } }
        })

        await wrapper.vm.$nextTick()

        expect(wrapper.html()).toContain('account')
    })

    it('renders the settings page header', async () => {
        const { default: SettingsView } = await import('@/views/SettingsView.vue')
        const wrapper = mount(SettingsView, {
            global: { stubs: { 'router-link': true, 'AppIcon': true, 'TimeBandSlider': true } }
        })

        await wrapper.vm.$nextTick()

        expect(wrapper.find('h1').text()).toBe('settings')
    })
})
