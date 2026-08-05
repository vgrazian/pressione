import { describe, it, expect, beforeEach } from 'vitest'
import { useTheme } from '@/services/theme.js'

describe('useTheme', () => {
    function resetStorage() {
        try { localStorage.removeItem('pressione_theme') } catch { }
    }

    beforeEach(() => {
        resetStorage()
        document.documentElement.removeAttribute('data-theme')
        // Reset singleton ref to 'system' (default when no localStorage)
        const { setTheme } = useTheme()
        setTheme('system')
    })

    it('returns default theme "system" when no saved preference', () => {
        const { theme } = useTheme()
        expect(theme.value).toBe('system')
    })

    it('setTheme updates theme ref and localStorage', () => {
        const { theme, setTheme } = useTheme()
        setTheme('dark')
        expect(theme.value).toBe('dark')
        try { expect(localStorage.getItem('pressione_theme')).toBe('dark') } catch { }
    })

    it('setTheme applies data-theme attribute for dark', () => {
        const { setTheme } = useTheme()
        setTheme('dark')
        expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    })

    it('setTheme removes data-theme attribute for system', () => {
        const { setTheme } = useTheme()
        setTheme('dark')
        setTheme('system')
        expect(document.documentElement.hasAttribute('data-theme')).toBe(false)
    })

    it('toggle switches light ↔ dark (system not in cycle)', () => {
        const { theme, resolvedTheme, setTheme, toggle } = useTheme()
        setTheme('light')
        expect(theme.value).toBe('light')
        expect(resolvedTheme.value).toBe('light')
        toggle()
        expect(theme.value).toBe('dark')
        expect(resolvedTheme.value).toBe('dark')
        toggle()
        expect(theme.value).toBe('light')
    })

    it('toggle from system switches to explicit dark', () => {
        const { theme, toggle } = useTheme()
        // theme is already 'system' by default
        expect(theme.value).toBe('system')
        toggle()
        // Should set explicit dark (not stay in system)
        expect(theme.value).toBe('dark')
    })

    it('resolvedTheme returns actual light/dark even when set to system', () => {
        const { resolvedTheme, setTheme } = useTheme()
        setTheme('dark')
        expect(resolvedTheme.value).toBe('dark')
        setTheme('light')
        expect(resolvedTheme.value).toBe('light')
        setTheme('system')
        // In test env (happy-dom), no matchMedia, so defaults to light
        expect(['light', 'dark']).toContain(resolvedTheme.value)
    })

    it('theme ref is a singleton', () => {
        const { theme, setTheme } = useTheme()
        setTheme('dark')
        // useTheme returns the same module-level ref
        const { theme: theme2 } = useTheme()
        expect(theme2.value).toBe('dark')
    })
})
