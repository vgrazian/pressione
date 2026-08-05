import { ref, computed, watch } from 'vue'

const THEME_KEY = 'pressione_theme'

// Safe localStorage access for SSR/test environments
function getStoredTheme() {
    try { return localStorage.getItem(THEME_KEY) }
    catch { return null }
}

const theme = ref(getStoredTheme() || 'light')

// Resolved theme: maps 'system' → actual 'light' or 'dark' based on OS pref
const resolvedTheme = computed(() => {
    if (theme.value === 'system') {
        if (typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            return 'dark'
        }
        return 'light'
    }
    return theme.value
})

export function useTheme() {
    function apply(mode) {
        const root = document.documentElement
        if (mode === 'dark') {
            root.setAttribute('data-theme', 'dark')
        } else if (mode === 'light') {
            root.setAttribute('data-theme', 'light')
        } else {
            root.removeAttribute('data-theme')
        }
    }

    function setTheme(mode) {
        theme.value = mode
        try { localStorage.setItem(THEME_KEY, mode) } catch { }
        apply(mode)
    }

    function toggle() {
        // Direct light ↔ dark toggle. 'system' is settable only from Settings.
        setTheme(resolvedTheme.value === 'dark' ? 'light' : 'dark')
    }

    // Apply on init
    apply(theme.value)

    // Re-apply when OS preference changes (only when in 'system' mode)
    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
            if (theme.value === 'system') apply('system')
        })
    }

    return { theme, resolvedTheme, setTheme, toggle }
}
