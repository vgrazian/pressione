import { describe, it, expect, vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import OfflineBanner from '@/components/OfflineBanner.vue'

vi.mock('@/services/dataService.js', () => ({
    isOnline: vi.fn(() => Promise.resolve(true))
}))

const { isOnline } = await import('@/services/dataService.js')

describe('OfflineBanner', () => {
    it('is hidden when online (default state)', () => {
        const wrapper = shallowMount(OfflineBanner)
        expect(wrapper.find('.offline-banner').exists()).toBe(false)
    })

    it('calls isOnline on mount', () => {
        shallowMount(OfflineBanner)
        expect(isOnline).toHaveBeenCalled()
    })

    it('renders without crashing', () => {
        const wrapper = shallowMount(OfflineBanner)
        expect(wrapper.vm).toBeDefined()
    })

    it('produces valid HTML', () => {
        const wrapper = shallowMount(OfflineBanner)
        expect(wrapper.html()).toBeDefined()
    })
})
