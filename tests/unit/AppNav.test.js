import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { ref } from 'vue'

// Reactive mock user that can be changed per test
const mockUser = ref({ username: 'test', role: 'user' })

// Mock vue-router
let currentPath = '/'
vi.mock('vue-router', () => ({
    useRoute: () => ({ path: currentPath }),
    useRouter: () => ({ push: vi.fn() })
}))

// Mock useAuth — returns the reactive mockUser ref
vi.mock('@/services/auth.js', () => ({
    useAuth: () => ({
        user: mockUser,
        logout: vi.fn()
    })
}))

// Must import AppNav AFTER the mocks are set up
const { default: AppNav } = await import('@/components/AppNav.vue')

describe('AppNav', () => {
    beforeEach(() => {
        currentPath = '/'
        mockUser.value = { username: 'test', role: 'user' }
    })

    it('renders 4 navigation items for regular user', () => {
        const wrapper = shallowMount(AppNav)
        expect(wrapper.findAll('.nav-item')).toHaveLength(4)
    })

    it('renders 5 navigation items for admin user', () => {
        mockUser.value = { username: 'admin', role: 'admin' }
        const wrapper = shallowMount(AppNav)
        expect(wrapper.findAll('.nav-item')).toHaveLength(5)
    })

    it('nav items have correct labels for regular user', () => {
        const wrapper = shallowMount(AppNav)
        const labels = wrapper.findAll('.nav-label')
        expect(labels[0].text()).toBe('Home')
        expect(labels[1].text()).toBe('Lista')
        expect(labels[2].text()).toBe('Analisi')
        expect(labels[3].text()).toBe('Altro')
    })

    it('admin sees Gestione item between Analisi and Altro', () => {
        mockUser.value = { username: 'admin', role: 'admin' }
        const wrapper = shallowMount(AppNav)
        const labels = wrapper.findAll('.nav-label')
        expect(labels).toHaveLength(5)
        expect(labels[2].text()).toBe('Analisi')
        expect(labels[3].text()).toBe('Gestione')
        expect(labels[4].text()).toBe('Altro')
    })

    it('Gestione item is not visible when user is not admin', () => {
        mockUser.value = { username: 'user', role: 'user' }
        const wrapper = shallowMount(AppNav)
        const labels = wrapper.findAll('.nav-label')
        const gestioneLabel = labels.filter(l => l.text() === 'Gestione')
        expect(gestioneLabel).toHaveLength(0)
    })

    it('home item is active when on home route', () => {
        currentPath = '/'
        const wrapper = shallowMount(AppNav)
        const items = wrapper.findAll('.nav-item')
        expect(items[0].classes()).toContain('active')
    })

    it('list item is active when on list route', () => {
        currentPath = '/list'
        const wrapper = shallowMount(AppNav)
        const items = wrapper.findAll('.nav-item')
        expect(items[1].classes()).toContain('active')
    })

    it('operators item is active when on operators route (admin)', () => {
        mockUser.value = { username: 'admin', role: 'admin' }
        currentPath = '/operators'
        const wrapper = shallowMount(AppNav)
        const items = wrapper.findAll('.nav-item')
        expect(items[3].classes()).toContain('active')
    })

    it('renders all nav items with correct structure for admin', () => {
        mockUser.value = { username: 'admin', role: 'admin' }
        const wrapper = shallowMount(AppNav)
        const items = wrapper.findAll('.nav-item')
        expect(items).toHaveLength(5)
        items.forEach((item) => {
            expect(item.find('.nav-label').exists()).toBe(true)
            expect(item.classes()).toContain('nav-item')
        })
    })
})
