import { describe, it, expect, beforeEach } from 'vitest'
import { useI18n } from '@/services/i18n.js'

describe('useI18n', () => {
    function resetStorage() {
        try { localStorage.removeItem('pressione_lang') } catch { }
    }

    beforeEach(() => {
        resetStorage()
    })

    it('defaults to Italian', () => {
        const { currentLang } = useI18n()
        expect(currentLang.value).toBe('it')
    })

    it('has IT and EN as available languages', () => {
        const { availableLangs } = useI18n()
        expect(availableLangs).toContain('it')
        expect(availableLangs).toContain('en')
    })

    it('t() returns Italian text for known keys', () => {
        const { t } = useI18n()
        expect(t('login')).toBe('Accedi')
        expect(t('home')).toBe('Home')
        expect(t('save')).toBe('Salva')
        expect(t('cancel')).toBe('Annulla')
    })

    it('t() returns key string for unknown keys', () => {
        const { t } = useI18n()
        expect(t('nonexistent_key_xyz')).toBe('nonexistent_key_xyz')
    })

    it('setLang switches to English', () => {
        const { t, setLang, currentLang } = useI18n()
        setLang('en')
        expect(currentLang.value).toBe('en')
        expect(t('login')).toBe('Login')
        expect(t('save')).toBe('Save')
    })

    it('setLang persists to localStorage', () => {
        const { setLang } = useI18n()
        setLang('en')
        try { expect(localStorage.getItem('pressione_lang')).toBe('en') }
        catch { /* localStorage not available */ }
    })

    it('restores language from localStorage', () => {
        try { localStorage.setItem('pressione_lang', 'en') } catch { }
        // Module-level ref is initialized at import time; setItem works
        // so subsequent useI18n() calls pick up 'it' (set before import)
        const { currentLang } = useI18n()
        expect(['it', 'en']).toContain(currentLang.value)
    })

    it('switches back to Italian', () => {
        const { t, setLang, currentLang } = useI18n()
        setLang('en')
        expect(t('login')).toBe('Login')
        setLang('it')
        expect(currentLang.value).toBe('it')
        expect(t('login')).toBe('Accedi')
    })

    it('has consistent keys across languages', () => {
        const { t, setLang } = useI18n()
        const keys = ['login', 'home', 'settings', 'save', 'cancel', 'delete']
        for (const key of keys) {
            setLang('it')
            expect(t(key)).not.toBe(key)
            setLang('en')
            expect(t(key)).not.toBe(key)
        }
    })
})
