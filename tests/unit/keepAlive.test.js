import { describe, it, expect, vi } from 'vitest'

// Mock Supabase client to avoid import side-effects
vi.mock('@/services/supabaseClient.js', () => ({
    isSupabaseConfigured: true,
    supabase: {
        from: () => ({
            select: () => ({
                limit: () => Promise.resolve({ error: null })
            })
        })
    }
}))

// Mock IndexedDB
vi.mock('@/db/index.js', () => ({
    getSetting: vi.fn().mockResolvedValue(false),
    setSetting: vi.fn().mockResolvedValue(undefined)
}))

import { formatBytes } from '@/services/keepAlive.js'

describe('formatBytes', () => {
    it('returns "N/D" for null', () => {
        expect(formatBytes(null)).toBe('N/D')
    })

    it('returns "N/D" for undefined', () => {
        expect(formatBytes(undefined)).toBe('N/D')
    })

    it('formats bytes correctly', () => {
        expect(formatBytes(0)).toBe('0 B')
        expect(formatBytes(500)).toBe('500 B')
        expect(formatBytes(1023)).toBe('1023 B')
    })

    it('formats kilobytes correctly', () => {
        expect(formatBytes(1024)).toBe('1.0 KB')
        expect(formatBytes(1536)).toBe('1.5 KB')
        expect(formatBytes(10240)).toBe('10.0 KB')
    })

    it('formats megabytes correctly', () => {
        expect(formatBytes(1048576)).toBe('1.0 MB')
        expect(formatBytes(5242880)).toBe('5.0 MB')
        expect(formatBytes(104857600)).toBe('100.0 MB')
    })

    it('handles edge cases at KB/MB boundary', () => {
        expect(formatBytes(1024 * 1024 - 1)).toBe('1024.0 KB')
        expect(formatBytes(1024 * 1024)).toBe('1.0 MB')
    })

    it('handles large values', () => {
        expect(formatBytes(1073741824)).toBe('1024.0 MB')
    })
})
