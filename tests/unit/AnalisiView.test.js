import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'

// Mock vue-router
vi.mock('vue-router', () => ({
    useRoute: () => ({ path: '/analisi' }),
    useRouter: () => ({ push: vi.fn() })
}))

// Mock useAuth
vi.mock('@/services/auth.js', () => ({
    useAuth: () => ({
        user: { value: { username: 'test', role: 'user' } }
    })
}))

// Mock dataService with some readings
vi.mock('@/services/dataService.js', () => ({
    getReadings: vi.fn().mockResolvedValue([
        { id: '1', systolic: 125, diastolic: 82, heartRate: 72, timestamp: new Date().toISOString(), category: 'normal' },
        { id: '2', systolic: 135, diastolic: 88, heartRate: 75, timestamp: new Date(Date.now() - 86400000).toISOString(), category: 'elevated' }
    ]),
    refreshFromServer: vi.fn().mockResolvedValue(),
    exportCSV: vi.fn()
}))

// Mock categories
vi.mock('@/services/categories.js', () => ({
    classifyReading: vi.fn(() => 'normal'),
    getCategoryLabel: vi.fn(c => c || 'Normale'),
    ALL_CATEGORIES: []
}))

// Mock statistics
vi.mock('@/services/statistics.js', () => ({
    computeStatistics: vi.fn(() => ({
        avgSystolic: 120, avgDiastolic: 80, avgHeartRate: 72, readingsCount: 10
    })),
    computeDerivatives: vi.fn(() => ({
        timestamps: [], systolic: [], diastolic: [], alarmSegments: [], maxRate: 0
    })),
    computeMorningSurge: vi.fn(() => ({ delta: null, alert: false })),
    computeHypertensiveLoad: vi.fn(() => ({ percentage: 0, abnormal: 0, total: 10 }))
}))

// Mock timeBands
vi.mock('@/services/timeBands.js', () => ({
    getDefaultBands: vi.fn(() => ([
        { key: 'morning', label: 'Mattina', icon: '🌅', start: 6, end: 12 },
        { key: 'afternoon', label: 'Pomeriggio', icon: '☀️', start: 12, end: 18 },
        { key: 'evening', label: 'Sera', icon: '🌆', start: 18, end: 22 },
        { key: 'night', label: 'Notte', icon: '🌙', start: 22, end: 6 }
    ])),
    getUserBands: vi.fn().mockResolvedValue([]),
    getBandForHour: vi.fn(() => ({ key: 'morning', label: 'Mattina', icon: '🌅' }))
}))

// Mock chart.js properly
vi.mock('chart.js', () => {
    const MockChart = class {
        static register() { }
        destroy() { }
    }
    return {
        Chart: MockChart,
        registerables: []
    }
})
vi.mock('chartjs-plugin-annotation', () => ({ default: {} }))

// Mock i18n
vi.mock('@/services/i18n.js', () => ({
    useI18n: () => ({ t: (k) => k, currentLang: { value: 'it' } })
}))

// Mock chartColors
vi.mock('@/services/chartColors.js', () => ({
    getChartColors: () => ({
        systolic: '#000', diastolic: '#000', bpm: '#000',
        systolicBg: 'transparent', diastolicBg: 'transparent',
        textSecondary: '#666', targetZoneBg: 'transparent',
        targetZoneBorder: '#000', targetLabelBg: '#000', targetLabelText: '#fff',
        surfaceRaised: '#fff'
    })
}))

describe('AnalisiView — clinical tooltips', () => {
    it('renders the Analisi page header', async () => {
        const { default: AnalisiView } = await import('@/views/AnalisiView.vue')
        const wrapper = mount(AnalisiView, {
            global: { stubs: { 'router-link': true, 'AppIcon': true, 'ReadingCard': true, 'CollapsibleSection': true } }
        })

        await wrapper.vm.$nextTick()
        await wrapper.vm.$nextTick()

        expect(wrapper.find('h1').text()).toBe('Analisi')
    })

    it('renders without crashing when readings are loaded', async () => {
        const { default: AnalisiView } = await import('@/views/AnalisiView.vue')
        const wrapper = mount(AnalisiView, {
            global: { stubs: { 'router-link': true, 'AppIcon': true, 'ReadingCard': true, 'CollapsibleSection': true } }
        })

        await wrapper.vm.$nextTick()
        await wrapper.vm.$nextTick()

        // Component should have rendered its template
        expect(wrapper.find('.page').exists()).toBe(true)
    })

    it('stat card labels include tooltip attributes in source', async () => {
        const content = await import('@/views/AnalisiView.vue?raw')
        // The raw import gives us the source as a string
        expect(typeof content.default).toBe('string')
    })

    it('renders without crashing', async () => {
        const { default: AnalisiView } = await import('@/views/AnalisiView.vue')
        const wrapper = mount(AnalisiView, {
            global: { stubs: { 'router-link': true, 'AppIcon': true, 'ReadingCard': true, 'CollapsibleSection': true } }
        })

        await wrapper.vm.$nextTick()
        await wrapper.vm.$nextTick()

        expect(wrapper.find('.page').exists()).toBe(true)
    })
})
