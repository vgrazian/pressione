import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import CategoryBadge from '@/components/CategoryBadge.vue'

describe('CategoryBadge', () => {
    it('renders category label', () => {
        const wrapper = shallowMount(CategoryBadge, {
            props: { category: 'NORMAL' }
        })
        expect(wrapper.text()).toContain('Normale')
    })

    it('renders hypertension stage 1 label', () => {
        const wrapper = shallowMount(CategoryBadge, {
            props: { category: 'HYPERTENSION_STAGE_1' }
        })
        expect(wrapper.text()).toContain('Ipertensione Stadio 1')
    })

    it('applies inline color style', () => {
        const wrapper = shallowMount(CategoryBadge, {
            props: { category: 'NORMAL' }
        })
        const span = wrapper.find('.badge-category')
        expect(span.attributes('style')).toContain('color')
        expect(span.attributes('style')).toContain('background-color')
    })

    it('has base class badge-category', () => {
        const wrapper = shallowMount(CategoryBadge, {
            props: { category: 'HYPERTENSIVE_CRISIS' }
        })
        expect(wrapper.find('.badge-category').exists()).toBe(true)
    })

    it('applies small class when small prop is true', () => {
        const wrapper = shallowMount(CategoryBadge, {
            props: { category: 'NORMAL', small: true }
        })
        expect(wrapper.find('.badge-category--small').exists()).toBe(true)
    })

    it('does not apply small class by default', () => {
        const wrapper = shallowMount(CategoryBadge, {
            props: { category: 'NORMAL' }
        })
        expect(wrapper.find('.badge-category--small').exists()).toBe(false)
    })
})
