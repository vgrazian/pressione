// Role-Based Access Control
export const ROLE_ADMIN = 'admin'
export const ROLE_USER = 'user'

const PERMISSION_MATRIX = {
    admin: new Set([
        'readings:read', 'readings:write', 'readings:delete',
        'settings:read', 'settings:write',
        'reminders:read', 'reminders:write',
        'users:read', 'users:write', 'users:delete',
        'export:all', 'import:all',
        'sync:run'
    ]),
    user: new Set([
        'readings:read', 'readings:write', 'readings:delete',
        'settings:read', 'settings:write',
        'reminders:read', 'reminders:write',
        'export:all', 'import:all',
        'sync:run'
    ])
}

export function hasPermission(user, permission) {
    if (!user) return false
    const permissions = PERMISSION_MATRIX[user.role]
    return permissions ? permissions.has(permission) : false
}

export function isAdmin(user) {
    return user?.role === ROLE_ADMIN
}
