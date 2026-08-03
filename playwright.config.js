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
        command: 'VITE_SUPABASE_URL= VITE_SUPABASE_PUBLISHABLE_KEY= npx vite --port 5173',
        port: 5173,
        reuseExistingServer: true
    }
})
