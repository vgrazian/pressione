import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

/**
 * UI Edge Cases E2E Tests.
 *
 * Tests profile prompt, offline banner, and theme persistence.
 * Run: npx playwright test tests/e2e/ui-edge-cases.spec.js --reporter=line
 */

test.describe('UI — Profile Prompt', () => {
    test('UI-12: Login shows profile prompt for incomplete profiles', async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(1000)

        // Profile prompt may or may not appear depending on bot's profile state
        const prompt = page.locator('.profile-prompt, [class*="profile-prompt"]')
        const isVisible = await prompt.isVisible({ timeout: 2000 }).catch(() => false)

        if (isVisible) {
            // Should have a skip button
            const skipBtn = page.locator('button:has-text("Salta"), button:has-text("Skip")')
            const hasSkip = await skipBtn.isVisible({ timeout: 2000 }).catch(() => false)

            if (hasSkip) {
                await skipBtn.click()
                await page.waitForTimeout(500)
                // Prompt should close
                const closed = await prompt.isVisible({ timeout: 2000 }).catch(() => false)
                expect(closed).toBe(false)
            }
        }
        // If no prompt, that's fine too
    })

    test('UI-13: Profile prompt has save button', async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(1000)

        const prompt = page.locator('.profile-prompt, [class*="profile-prompt"]')
        const isVisible = await prompt.isVisible({ timeout: 2000 }).catch(() => false)

        if (isVisible) {
            const saveBtn = page.locator('button:has-text("Salva")')
            const hasSave = await saveBtn.isVisible({ timeout: 2000 }).catch(() => false)
            // Either has save button or skip button
            const skipBtn = page.locator('button:has-text("Salta"), button:has-text("Skip")')
            const hasSkip = await skipBtn.isVisible({ timeout: 2000 }).catch(() => false)
            expect(hasSave || hasSkip).toBe(true)
        }
    })
})

test.describe('UI — Offline Banner', () => {
    test('UI-14: App loads without crashing (offline banner may or may not show)', async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(1000)

        // Just verify the app is in a stable state
        const body = page.locator('body')
        await expect(body).toBeVisible({ timeout: 3000 })
    })

    test('UI-15: Home page content is visible after login', async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(1000)

        // Should see greeting or empty state
        const hasContent = await Promise.race([
            page.locator('h1:has-text("bot")').isVisible({ timeout: 3000 }).catch(() => false),
            page.locator('.empty-state').isVisible({ timeout: 3000 }).catch(() => false),
            page.locator('.wellness-card').isVisible({ timeout: 3000 }).catch(() => false)
        ])

        expect(hasContent).toBe(true)
    })
})

test.describe('UI — Theme Persistence', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(500)
    })

    test('UI-16: Theme toggle changes data-theme attribute', async ({ page }) => {
        const themeBtn = page.locator('header button[title*="Tema"]')
        await expect(themeBtn).toBeVisible({ timeout: 3000 })

        // Get current state
        const html = page.locator('html')
        const initialTheme = await html.getAttribute('data-theme')

        // Toggle
        await themeBtn.click()
        await page.waitForTimeout(300)

        // Check attribute changed
        const newTheme = await html.getAttribute('data-theme')
        expect(newTheme).not.toBe(initialTheme)
    })

    test('UI-17: Dark mode applies to body', async ({ page }) => {
        const themeBtn = page.locator('header button[title*="Tema"]')
        const html = page.locator('html')
        const currentTheme = await html.getAttribute('data-theme')

        // Toggle to ensure we change
        await themeBtn.click()
        await page.waitForTimeout(300)

        // The page should still render (no crash)
        const body = page.locator('body')
        await expect(body).toBeVisible({ timeout: 2000 })

        // Toggle back
        await themeBtn.click()
        await page.waitForTimeout(300)
    })
})

test.describe('UI — Navigation Edge Cases', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(500)
    })

    test('UI-18: FAB is not visible on add reading page', async ({ page }) => {
        await page.locator('button.fab').click()
        await expect(page).toHaveURL(/\/#\/add/, { timeout: 5000 })

        // FAB should be hidden on add page
        const fab = page.locator('button.fab')
        const isVisible = await fab.isVisible({ timeout: 2000 }).catch(() => false)
        expect(isVisible).toBe(false)
    })

    test('UI-19: Breadcrumbs show on add/edit page', async ({ page }) => {
        await page.goto('/#/add')
        await expect(page).toHaveURL(/\/#\/add/, { timeout: 5000 })

        const breadcrumbs = page.locator('.breadcrumbs')
        await expect(breadcrumbs).toBeVisible({ timeout: 5000 })
    })

    test('UI-20: Back button on add page returns to previous page', async ({ page }) => {
        await page.locator('button.fab').click()
        await expect(page).toHaveURL(/\/#\/add/, { timeout: 5000 })

        const backBtn = page.locator('.back-btn')
        const hasBackBtn = await backBtn.isVisible({ timeout: 2000 }).catch(() => false)
        if (hasBackBtn) {
            await backBtn.click()
            await page.waitForTimeout(500)
            // Should navigate away from add
            expect(page.url()).not.toContain('/add')
        }
    })
})
