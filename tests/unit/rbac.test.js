import { describe, it, expect } from 'vitest'
import { hasPermission, isAdmin, ROLE_ADMIN, ROLE_USER } from '@/services/rbac.js'

describe('hasPermission', () => {
    it('admin has all permissions', () => {
        const admin = { role: ROLE_ADMIN }
        expect(hasPermission(admin, 'readings:read')).toBe(true)
        expect(hasPermission(admin, 'users:write')).toBe(true)
        expect(hasPermission(admin, 'sync:run')).toBe(true)
    })

    it('user has limited permissions', () => {
        const user = { role: ROLE_USER }
        expect(hasPermission(user, 'readings:read')).toBe(true)
        expect(hasPermission(user, 'readings:write')).toBe(true)
        expect(hasPermission(user, 'users:write')).toBe(false)
    })

    it('returns false for null user', () => {
        expect(hasPermission(null, 'readings:read')).toBe(false)
        expect(hasPermission(undefined, 'readings:read')).toBe(false)
    })
})

describe('isAdmin', () => {
    it('detects admin', () => {
        expect(isAdmin({ role: ROLE_ADMIN })).toBe(true)
        expect(isAdmin({ role: ROLE_USER })).toBe(false)
        expect(isAdmin(null)).toBe(false)
    })
})
