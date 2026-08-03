import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
    testDir: './tests/e2e',
    timeout: 30000,
    retries: 0,
    use: {
        baseURL: 'http://localhost:5173/pressione',
        trace: 'on-first-retry',
        screenshot: 'only-on-failure'
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] }
        }
    ],
    webServer: {
        command: 'VITE_SUPABASE_URL=https://pvmlphhzqevmktrknipo.supabase.co VITE_SUPABASE_PUBLISHABLE_KEY=sb_publishable_PfNRklkcQY5lyhyhrDH3Ug_PG268nto npx vite --port 5173',
        port: 5173,
        reuseExistingServer: false
    }
})
