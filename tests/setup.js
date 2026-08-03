// Test setup: fake-indexeddb polyfill
import 'fake-indexeddb/auto'

// Ensure localStorage is available in happy-dom
if (typeof localStorage === 'undefined' || !localStorage) {
    const store = {}
    globalThis.localStorage = {
        getItem: (key) => store[key] || null,
        setItem: (key, value) => { store[key] = String(value) },
        removeItem: (key) => { delete store[key] },
        clear: () => { Object.keys(store).forEach(k => delete store[k]) },
        get length() { return Object.keys(store).length },
        key: (i) => Object.keys(store)[i] || null
    }
}

// Mock Vite env variables for unit tests
process.env.VITE_SUPABASE_URL = 'https://test.supabase.co'
process.env.VITE_SUPABASE_PUBLISHABLE_KEY = 'test-key'
