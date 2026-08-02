// Global setup that runs before tests are loaded
// Ensures localStorage is available before any module-level code executes

if (typeof localStorage === 'undefined') {
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
