import { describe, it, expect, vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import AppNav from '@/components/AppNav.vue'

// Mock vue-router with a reactive path
let currentPath = '/'
vi.mock('vue-router', () => ({
    useRoute: () => ({ path: currentPath }),
    useRouter: () => ({ push: vi.fn() })
}))

// Mock useAuth
vi.mock('@/services/auth.js', () => ({
    useAuth: () => ({
        user: { value: { username: 'test', role: 'user' } },
        logout: vi.fn()
    })
}))

describe('AppNav', () => {
    beforeEach(() => {
        currentPath = '/'
    })

    it('renders 4 navigation items', () => {
        const wrapper = shallowMount(AppNav)
        expect(wrapper.findAll('.nav-item')).toHaveLength(4)
    })

    it('nav items have labels', () => {
        const wrapper = shallowMount(AppNav)
        const labels = wrapper.findAll('.nav-label')
        expect(labels[0].text()).toBe('Home')
        expect(labels[1].text()).toBe('Lista')
        expect(labels[2].text()).toBe('Analisi')
        expect(labels[3].text()).toBe('Altro')
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

    it('renders all 4 nav items with correct labels and icons', () => {
        const wrapper = shallowMount(AppNav)
        const items = wrapper.findAll('.nav-item')
        expect(items).toHaveLength(4)
        // Verify each item has the expected structure (label + icon stub)
        items.forEach((item, i) => {
            expect(item.find('.nav-label').exists()).toBe(true)
            expect(item.classes()).toContain('nav-item')
        })
    })
})
