import { describe, it, expect, vi, beforeEach } from 'vitest'
import { isIOS, isStandalone } from '@/services/pwaInstall.js'

describe('isIOS', () => {
    const originalUserAgent = navigator.userAgent

    afterEach(() => {
        Object.defineProperty(navigator, 'userAgent', {
            value: originalUserAgent,
            configurable: true
        })
    })

    function setUserAgent(ua) {
        Object.defineProperty(navigator, 'userAgent', {
            value: ua,
            configurable: true
        })
    }

    it('returns true for iPhone', () => {
        setUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15')
        expect(isIOS()).toBe(true)
    })

    it('returns true for iPad', () => {
        setUserAgent('Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15')
        expect(isIOS()).toBe(true)
    })

    it('returns true for iPod', () => {
        setUserAgent('Mozilla/5.0 (iPod touch; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15')
        expect(isIOS()).toBe(true)
    })

    it('returns false for Android', () => {
        setUserAgent('Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36')
        expect(isIOS()).toBe(false)
    })

    it('returns false for desktop Chrome', () => {
        setUserAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
        expect(isIOS()).toBe(false)
    })

    it('returns false for desktop Safari on Mac', () => {
        setUserAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15')
        expect(isIOS()).toBe(false)
    })
})

describe('isStandalone', () => {
    it('returns false in normal browser mode', () => {
        // By default in test environment, not standalone
        expect(isStandalone()).toBe(false)
    })

    it('returns true when display-mode is standalone', () => {
        // Mock matchMedia
        const originalMatchMedia = window.matchMedia
        window.matchMedia = vi.fn((query) => ({
            matches: query === '(display-mode: standalone)',
            media: query,
            onchange: null,
            addListener: vi.fn(),
            removeListener: vi.fn(),
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn()
        }))

        expect(isStandalone()).toBe(true)

        window.matchMedia = originalMatchMedia
    })
})
