import { describe, it, expect, vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import ReadingCard from '@/components/ReadingCard.vue'

// Mock vue-router
vi.mock('vue-router', () => ({
    useRoute: () => ({ path: '/' }),
    useRouter: () => ({ push: vi.fn() })
}))

describe('ReadingCard', () => {
    const reading = {
        id: '1',
        systolic: 128,
        diastolic: 82,
        heartRate: 72,
        timestamp: '2026-01-01T08:00:00',
        category: 'NORMAL',
        notes: 'Test note'
    }

    it('renders systolic value', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading }
        })
        expect(wrapper.text()).toContain('128')
    })

    it('renders diastolic value', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading }
        })
        expect(wrapper.text()).toContain('82')
    })

    it('renders heart rate value', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading }
        })
        expect(wrapper.text()).toContain('72')
    })

    it('renders BPM label', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading }
        })
        expect(wrapper.text()).toContain('BPM')
    })

    it('renders mmHg unit', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading }
        })
        expect(wrapper.text()).toContain('mmHg')
    })

    it('renders notes when not compact', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading }
        })
        expect(wrapper.text()).toContain('Test note')
    })

    it('hides notes in compact mode', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading, compact: true }
        })
        expect(wrapper.text()).not.toContain('Test note')
    })

    it('has compact class when compact prop is true', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading, compact: true }
        })
        expect(wrapper.find('.reading-card--compact').exists()).toBe(true)
    })

    it('emits edit event when edit button clicked', async () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading }
        })
        const buttons = wrapper.findAll('button')
        const editBtn = buttons.find(b => b.text().includes('Modifica'))
        await editBtn.trigger('click')
        expect(wrapper.emitted('edit')).toBeTruthy()
        expect(wrapper.emitted('edit')[0]).toEqual([reading])
    })

    it('emits delete event when delete button clicked', async () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading }
        })
        await wrapper.findAll('button').find(b => b.text().includes('Elimina'))?.trigger('click')
        expect(wrapper.emitted('delete')).toBeTruthy()
        expect(wrapper.emitted('delete')[0]).toEqual([reading])
    })

    it('hides action buttons in compact mode', () => {
        const wrapper = shallowMount(ReadingCard, {
            props: { reading, compact: true }
        })
        expect(wrapper.find('.reading-card__actions').exists()).toBe(false)
    })
})
