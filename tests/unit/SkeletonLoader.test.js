import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import SkeletonLoader from '@/components/SkeletonLoader.vue'

describe('SkeletonLoader', () => {
    it('renders text skeletons by default', () => {
        const wrapper = shallowMount(SkeletonLoader)
        expect(wrapper.find('.skeleton-list').exists()).toBe(true)
        expect(wrapper.findAll('.skeleton-text')).toHaveLength(3)
    })

    it('renders specified count of text skeletons', () => {
        const wrapper = shallowMount(SkeletonLoader, {
            props: { type: 'text', count: 5 }
        })
        expect(wrapper.findAll('.skeleton-text')).toHaveLength(5)
    })

    it('renders card skeletons', () => {
        const wrapper = shallowMount(SkeletonLoader, {
            props: { type: 'card', count: 2 }
        })
        expect(wrapper.find('.skeleton-grid').exists()).toBe(true)
        expect(wrapper.findAll('.skeleton-card')).toHaveLength(2)
    })

    it('renders chart skeleton', () => {
        const wrapper = shallowMount(SkeletonLoader, {
            props: { type: 'chart' }
        })
        expect(wrapper.find('.skeleton-chart').exists()).toBe(true)
    })

    it('renders stats skeletons', () => {
        const wrapper = shallowMount(SkeletonLoader, {
            props: { type: 'stats' }
        })
        expect(wrapper.find('.skeleton-stats').exists()).toBe(true)
        expect(wrapper.findAll('.skeleton-stat')).toHaveLength(4)
    })

    it('applies custom height to card skeleton', () => {
        const wrapper = shallowMount(SkeletonLoader, {
            props: { type: 'card', height: '200px' }
        })
        const card = wrapper.find('.skeleton-card')
        expect(card.attributes('style')).toContain('height: 200px')
    })
})
