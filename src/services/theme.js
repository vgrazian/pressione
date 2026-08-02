import { ref, watch } from 'vue'

const THEME_KEY = 'pressione_theme'

// Safe localStorage access for SSR/test environments
function getStoredTheme() {
    try { return localStorage.getItem(THEME_KEY) }
    catch { return null }
}

const theme = ref(getStoredTheme() || 'system')

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
        const cycle = { light: 'dark', dark: 'system', system: 'light' }
        setTheme(cycle[theme.value] || 'light')
    }

    // Apply on init
    apply(theme.value)

    return { theme, setTheme, toggle }
}
