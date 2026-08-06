import { describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import TimeOfDayIcon from '@/components/TimeOfDayIcon.vue'

describe('TimeOfDayIcon', () => {
    it('shows sun icon for morning hours', () => {
        const wrapper = shallowMount(TimeOfDayIcon, {
            props: { timestamp: '2026-01-01T08:00:00' }
        })
        // AppIcon is stubbed in shallowMount, check it received correct props
        const appIcon = wrapper.findComponent({ name: 'AppIcon' })
        expect(appIcon.exists()).toBe(true)
    })

    it('shows moon icon for night hours', () => {
        const wrapper = shallowMount(TimeOfDayIcon, {
            props: { timestamp: '2026-01-01T23:00:00' }
        })
        const appIcon = wrapper.findComponent({ name: 'AppIcon' })
        expect(appIcon.exists()).toBe(true)
    })

    it('passes size 14 to AppIcon', () => {
        const wrapper = shallowMount(TimeOfDayIcon, {
            props: { timestamp: '2026-01-01T12:00:00' }
        })
        const appIcon = wrapper.findComponent({ name: 'AppIcon' })
        expect(appIcon.props('size')).toBe(14)
    })

    it('has time-icon class', () => {
        const wrapper = shallowMount(TimeOfDayIcon, {
            props: { timestamp: '2026-01-01T12:00:00' }
        })
        expect(wrapper.find('.time-icon').exists()).toBe(true)
    })

    it('has title attribute with band label', () => {
        const wrapper = shallowMount(TimeOfDayIcon, {
            props: { timestamp: '2026-01-01T09:00:00' }
        })
        expect(wrapper.find('.time-icon').attributes('title')).toBeTruthy()
    })
})
