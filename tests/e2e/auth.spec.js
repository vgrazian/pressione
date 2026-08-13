import { test, expect } from '@playwright/test'
import { loginAsBot, loginViaForm } from './helpers/login.js'

test.describe('Auth Flow', () => {
    test('should redirect to login when not authenticated', async ({ page }) => {
        await page.goto('/#/')
        await page.waitForURL(/\/#\/login/, { timeout: 10000 })
        await expect(page.locator('h1')).toHaveText('IperTeso')
    })

    // Skipped: users table not exposed via Supabase REST API
    test.skip('should login with bot credentials via form', async ({ page }) => {
        await loginViaForm(page, 'bot', 'test1234')
    })

    test('should reject invalid credentials', async ({ page }) => {
        await page.goto('/#/login')
        await page.fill('#username', 'nonexistent')
        await page.fill('#password', 'wrong')
        await page.click('button[type="submit"]')

        await expect(page.locator('.form-error')).toBeVisible({ timeout: 5000 })
    })
})
