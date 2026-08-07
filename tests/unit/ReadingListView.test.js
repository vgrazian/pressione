import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'

// Mock vue-router
vi.mock('vue-router', () => ({
    useRoute: () => ({ path: '/list' }),
    useRouter: () => ({ push: vi.fn() })
}))

// Mock useAuth
vi.mock('@/services/auth.js', () => ({
    useAuth: () => ({
        user: { value: { username: 'testuser', role: 'user' } }
    })
}))

// Mock dataService
const mockDeleteReading = vi.fn().mockResolvedValue()
vi.mock('@/services/dataService.js', () => ({
    getReadings: vi.fn().mockResolvedValue([]),
    deleteReading: (...args) => mockDeleteReading(...args),
    refreshFromServer: vi.fn().mockResolvedValue()
}))

// Mock categories
vi.mock('@/services/categories.js', () => ({
    ALL_CATEGORIES: ['normal', 'elevated'],
    getCategoryLabel: vi.fn(c => c)
}))

describe('ReadingListView — undo deletion', () => {
    beforeEach(() => {
        vi.useFakeTimers()
        mockDeleteReading.mockClear()
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    it('deleteReading is not called immediately after handleDelete', async () => {
        const { default: ReadingListView } = await import('@/views/ReadingListView.vue')
        const wrapper = mount(ReadingListView, {
            global: { stubs: { 'router-link': true, 'ReadingCard': true, 'SkeletonLoader': true, 'CollapsibleSection': true, 'AppIcon': true } }
        })

        // Expose the handleDelete via vm
        const vm = wrapper.vm
        // Set up readings
        vm.allReadings = [
            { id: '1', systolic: 120, diastolic: 80, heartRate: 72, timestamp: new Date().toISOString(), category: 'normal' }
        ]

        // Mock window.confirm to return true (skip confirm dialog)
        const originalConfirm = window.confirm
        window.confirm = vi.fn(() => true)

        // Trigger delete on the first reading
        await vm.handleDelete(vm.allReadings[0])

        // Reading should be removed from list immediately
        expect(vm.allReadings).toHaveLength(0)
        // But deleteReading should NOT have been called yet (waiting for undo timeout)
        expect(mockDeleteReading).not.toHaveBeenCalled()

        // Undo should be visible
        expect(vm.undoReading).not.toBeNull()
        expect(vm.undoReading.id).toBe('1')

        window.confirm = originalConfirm
    })

    it('undo restores the reading and cancels the deletion', async () => {
        const { default: ReadingListView } = await import('@/views/ReadingListView.vue')
        const wrapper = mount(ReadingListView, {
            global: { stubs: { 'router-link': true, 'ReadingCard': true, 'SkeletonLoader': true, 'CollapsibleSection': true, 'AppIcon': true } }
        })

        const vm = wrapper.vm
        const reading = { id: '1', systolic: 120, diastolic: 80, heartRate: 72, timestamp: new Date().toISOString(), category: 'normal' }
        vm.allReadings = [{ ...reading }]

        const originalConfirm = window.confirm
        window.confirm = vi.fn(() => true)

        await vm.handleDelete(vm.allReadings[0])

        // Reading removed
        expect(vm.allReadings).toHaveLength(0)

        // Now undo
        vm.undoDelete()

        // Reading restored
        expect(vm.allReadings).toHaveLength(1)
        expect(vm.allReadings[0].id).toBe('1')
        expect(vm.undoReading).toBeNull()
        // Delete should never have been called
        expect(mockDeleteReading).not.toHaveBeenCalled()

        window.confirm = originalConfirm
    })

    it('deleteReading is called after undo timeout expires', async () => {
        const { default: ReadingListView } = await import('@/views/ReadingListView.vue')
        const wrapper = mount(ReadingListView, {
            global: { stubs: { 'router-link': true, 'ReadingCard': true, 'SkeletonLoader': true, 'CollapsibleSection': true, 'AppIcon': true } }
        })

        const vm = wrapper.vm
        const reading = { id: '1', systolic: 120, diastolic: 80, heartRate: 72, timestamp: new Date().toISOString(), category: 'normal' }
        vm.allReadings = [{ ...reading }]

        const originalConfirm = window.confirm
        window.confirm = vi.fn(() => true)

        await vm.handleDelete(vm.allReadings[0])

        // Reading removed from list
        expect(vm.allReadings).toHaveLength(0)

        // Fast-forward past the 5-second undo timeout
        vi.advanceTimersByTime(5000)

        // Now deleteReading should have been called
        expect(mockDeleteReading).toHaveBeenCalledWith('1', 'testuser')

        window.confirm = originalConfirm
    })

    it('undo toast is not shown when no reading is pending undo', async () => {
        const { default: ReadingListView } = await import('@/views/ReadingListView.vue')
        const wrapper = mount(ReadingListView, {
            global: { stubs: { 'router-link': true, 'ReadingCard': true, 'SkeletonLoader': true, 'CollapsibleSection': true, 'AppIcon': true } }
        })

        expect(wrapper.find('.undo-toast').exists()).toBe(false)
    })
})
