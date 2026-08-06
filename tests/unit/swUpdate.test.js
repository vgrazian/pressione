import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * Service Worker update tests.
 *
 * Tests the forceClearCache logic by mocking browser APIs at the module level.
 */

const mockUnregister = vi.fn(() => Promise.resolve(true))
const mockKeys = vi.fn(() => Promise.resolve([]))
const mockDelete = vi.fn(() => Promise.resolve(true))
const mockReload = vi.fn()
const mockGetRegistrations = vi.fn(() => Promise.resolve([{ unregister: mockUnregister }]))

// Mock browser globals using vi.stubGlobal before imports
vi.stubGlobal('navigator', {
    serviceWorker: {
        ready: Promise.resolve({ waiting: null, addEventListener: vi.fn() }),
        getRegistrations: mockGetRegistrations,
        addEventListener: vi.fn()
    }
})
vi.stubGlobal('caches', {
    keys: mockKeys,
    delete: mockDelete
})
vi.stubGlobal('location', { reload: mockReload })

const { useSWUpdate } = await import('@/services/swUpdate.js')

beforeEach(() => {
    mockUnregister.mockClear()
    mockKeys.mockClear().mockResolvedValue([])
    mockDelete.mockClear()
    mockReload.mockClear()
    mockGetRegistrations.mockClear().mockResolvedValue([{ unregister: mockUnregister }])
    vi.clearAllMocks()
})

describe('useSWUpdate', () => {
    it('initial state: updateAvailable is false', () => {
        const { updateAvailable } = useSWUpdate()
        expect(updateAvailable.value).toBe(false)
    })

    it('initial state: updateFailed is false', () => {
        const { updateFailed } = useSWUpdate()
        expect(updateFailed.value).toBe(false)
    })

    it('forceClearCache calls getRegistrations', async () => {
        const { forceClearCache } = useSWUpdate()
        try { await forceClearCache() } catch { /* reload */ }

        expect(mockGetRegistrations).toHaveBeenCalled()
    })

    it('forceClearCache queries cache keys', async () => {
        mockKeys.mockResolvedValue(['v1', 'v2'])
        const { forceClearCache } = useSWUpdate()
        try { await forceClearCache() } catch { /* reload */ }

        expect(mockKeys).toHaveBeenCalled()
    })

    it('forceClearCache deletes each cache entry', async () => {
        mockKeys.mockResolvedValue(['v1', 'v2'])
        const { forceClearCache } = useSWUpdate()
        try { await forceClearCache() } catch { /* reload */ }

        expect(mockDelete).toHaveBeenCalledWith('v1')
        expect(mockDelete).toHaveBeenCalledWith('v2')
    })

    it('forceClearCache calls location.reload(true)', async () => {
        const { forceClearCache } = useSWUpdate()
        try { await forceClearCache() } catch { /* reload */ }

        expect(mockReload).toHaveBeenCalledWith(true)
    })
})
