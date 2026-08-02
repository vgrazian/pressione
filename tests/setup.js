// Test setup: fake-indexeddb polyfill
import 'fake-indexeddb/auto'

// Mock Vite env variables for tests
process.env.VITE_SUPABASE_URL = 'https://test.supabase.co'
process.env.VITE_SUPABASE_PUBLISHABLE_KEY = 'test-key'
