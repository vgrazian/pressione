import { describe, it, expect } from 'vitest'
import { generateId } from '@/services/ids.js'

describe('generateId', () => {
    it('generates a valid UUID', () => {
        const id = generateId()
        expect(id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
    })

    it('generates unique IDs', () => {
        const ids = new Set()
        for (let i = 0; i < 100; i++) {
            ids.add(generateId())
        }
        expect(ids.size).toBe(100)
    })
})
