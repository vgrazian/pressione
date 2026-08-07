import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import AppIcon from '@/components/AppIcon.vue'

describe('AppIcon', () => {
    it('renders heart icon', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'heart' } })
        expect(wrapper.find('svg').exists()).toBe(true)
        expect(wrapper.find('svg').attributes('viewBox')).toBe('0 0 24 24')
    })

    it('renders home icon', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'home' } })
        expect(wrapper.find('svg').exists()).toBe(true)
    })

    it('renders plus icon', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'plus' } })
        expect(wrapper.find('svg').exists()).toBe(true)
    })

    it('renders trash icon', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'trash' } })
        expect(wrapper.find('svg').exists()).toBe(true)
    })

    it('renders settings icon', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'settings' } })
        expect(wrapper.find('svg').exists()).toBe(true)
    })

    it('renders download icon (new)', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'download' } })
        expect(wrapper.find('svg').exists()).toBe(true)
    })

    it('renders upload icon (new)', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'upload' } })
        expect(wrapper.find('svg').exists()).toBe(true)
    })

    it('renders refresh icon (new)', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'refresh' } })
        expect(wrapper.find('svg').exists()).toBe(true)
    })

    it('renders x icon (cancel/close)', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'x' } })
        expect(wrapper.find('svg').exists()).toBe(true)
    })

    it('x icon has two lines', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'x' } })
        const lines = wrapper.findAll('line')
        expect(lines).toHaveLength(2)
    })

    it('renders fallback dot for unknown icon', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'nonexistent' } })
        expect(wrapper.find('.icon-fallback').exists()).toBe(true)
        expect(wrapper.find('.icon-fallback').text()).toBe('●')
    })

    it('applies size prop', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'heart', size: 32 } })
        expect(wrapper.find('svg').attributes('width')).toBe('32')
        expect(wrapper.find('svg').attributes('height')).toBe('32')
    })

    it('default size is 24', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'heart' } })
        expect(wrapper.find('svg').attributes('width')).toBe('24')
    })

    it('default color is currentColor', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'heart' } })
        expect(wrapper.find('svg').attributes('stroke')).toBe('currentColor')
    })

    it('applies custom color prop', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'heart', color: '#ff0000' } })
        expect(wrapper.find('svg').attributes('stroke')).toBe('#ff0000')
    })

    it('all icons have stroke-width 1.5', () => {
        const wrapper = shallowMount(AppIcon, { props: { name: 'heart' } })
        expect(wrapper.find('svg').attributes('stroke-width')).toBe('1.5')
    })
})
