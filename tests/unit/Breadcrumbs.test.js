import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import Breadcrumbs from '@/components/Breadcrumbs.vue'

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
    useRoute: () => ({ path: '/add' }),
    useRouter: () => ({ push: mockPush })
}))

describe('Breadcrumbs', () => {
    const items = [
        { label: 'Home', to: '/' },
        { label: 'Nuova Misurazione' }
    ]

    it('renders all breadcrumb items', () => {
        const wrapper = shallowMount(Breadcrumbs, { props: { items } })
        expect(wrapper.text()).toContain('Home')
        expect(wrapper.text()).toContain('Nuova Misurazione')
    })

    it('last item is not a link', () => {
        const wrapper = shallowMount(Breadcrumbs, { props: { items } })
        const spans = wrapper.findAll('.breadcrumbs__current')
        expect(spans).toHaveLength(1)
        expect(spans[0].text()).toBe('Nuova Misurazione')
    })

    it('first item is a clickable link', () => {
        const wrapper = shallowMount(Breadcrumbs, { props: { items } })
        const link = wrapper.find('.breadcrumbs__link')
        expect(link.exists()).toBe(true)
        expect(link.text()).toBe('Home')
    })

    it('clicking link navigates to path', async () => {
        const wrapper = shallowMount(Breadcrumbs, { props: { items } })
        await wrapper.find('.breadcrumbs__link').trigger('click')
        expect(mockPush).toHaveBeenCalledWith('/')
    })

    it('renders separator between items', () => {
        const wrapper = shallowMount(Breadcrumbs, { props: { items } })
        expect(wrapper.find('.breadcrumbs__sep').exists()).toBe(true)
    })

    it('handles single item (no link, no separator)', () => {
        const wrapper = shallowMount(Breadcrumbs, {
            props: { items: [{ label: 'Home' }] }
        })
        expect(wrapper.find('.breadcrumbs__current').text()).toBe('Home')
        expect(wrapper.find('.breadcrumbs__sep').exists()).toBe(false)
    })
})
