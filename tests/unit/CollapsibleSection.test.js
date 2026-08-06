import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import CollapsibleSection from '@/components/CollapsibleSection.vue'

describe('CollapsibleSection', () => {
    it('renders title', () => {
        const wrapper = shallowMount(CollapsibleSection, {
            props: { title: 'Test Section' },
            slots: { default: '<p>Content</p>' }
        })
        expect(wrapper.find('.collapsible__title').text()).toBe('Test Section')
    })

    it('is closed by default', () => {
        const wrapper = shallowMount(CollapsibleSection, {
            props: { title: 'Test' }
        })
        expect(wrapper.find('.collapsible--open').exists()).toBe(false)
    })

    it('opens when open prop is true', () => {
        const wrapper = shallowMount(CollapsibleSection, {
            props: { title: 'Test', open: true }
        })
        expect(wrapper.find('.collapsible--open').exists()).toBe(true)
    })

    it('toggles open state on header click', async () => {
        const wrapper = shallowMount(CollapsibleSection, {
            props: { title: 'Test' }
        })
        expect(wrapper.find('.collapsible--open').exists()).toBe(false)
        await wrapper.find('.collapsible__header').trigger('click')
        expect(wrapper.find('.collapsible--open').exists()).toBe(true)
        await wrapper.find('.collapsible__header').trigger('click')
        expect(wrapper.find('.collapsible--open').exists()).toBe(false)
    })

    it('shows slot content when open', async () => {
        const wrapper = shallowMount(CollapsibleSection, {
            props: { title: 'Test' },
            slots: { default: '<p>Hidden content</p>' }
        })
        // Closed — slot not rendered (v-show hides it)
        await wrapper.find('.collapsible__header').trigger('click')
        expect(wrapper.find('.collapsible__body').text()).toContain('Hidden content')
    })

    it('has aria-expanded attribute', () => {
        const wrapper = shallowMount(CollapsibleSection, {
            props: { title: 'Test' }
        })
        const btn = wrapper.find('.collapsible__header')
        expect(btn.attributes('aria-expanded')).toBe('false')
        btn.trigger('click')
        // After click, aria-expanded should update
    })
})
