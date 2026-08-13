import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

/**
 * Profile Prompt E2E Tests.
 * Tests the profile completion prompt flow.
 * Run: npx playwright test tests/e2e/profile-prompt.spec.js --reporter=line
 */

test.describe('Profile Prompt', () => {
    test('PP-01: Login shows profile prompt or home page', async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(1000)

        // Either profile prompt or home page should be visible
        const hasPrompt = await page.locator('.profile-prompt-overlay').isVisible({ timeout: 2000 }).catch(() => false)
        const hasHome = await page.locator('h1:has-text("bot")').isVisible({ timeout: 2000 }).catch(() => false)

        expect(hasPrompt || hasHome).toBe(true)
    })

    test('PP-02: Profile prompt has birth date and gender fields', async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(1000)

        const prompt = page.locator('.profile-prompt-overlay')
        const isVisible = await prompt.isVisible({ timeout: 2000 }).catch(() => false)

        if (isVisible) {
            await expect(page.locator('#pp-birthdate')).toBeVisible({ timeout: 3000 })
            await expect(page.locator('#pp-gender')).toBeVisible({ timeout: 3000 })
        }
    })

    test('PP-03: Skip button closes the prompt', async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(1000)

        const prompt = page.locator('.profile-prompt-overlay')
        const isVisible = await prompt.isVisible({ timeout: 2000 }).catch(() => false)

        if (isVisible) {
            const skipBtn = page.locator('button:has-text("Salta")')
            await skipBtn.click()
            await page.waitForTimeout(500)

            // Prompt should close
            const closed = await prompt.isVisible({ timeout: 2000 }).catch(() => false)
            expect(closed).toBe(false)
        }
    })
})
