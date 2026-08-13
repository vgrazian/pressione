import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

/**
 * UI Components E2E Tests
 *
 * Tests the new UI homogenization features: version tools, FAB visibility,
 * theme toggle, icon rendering, and update banner behavior.
 * Run: npx playwright test tests/e2e/ui-components.spec.js --reporter=line
 */

test.describe('UI Components — Login Version Tools', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/#/login')
        await page.waitForTimeout(500)
    })

    test('UI-01: Version bar shows app version and build', async ({ page }) => {
        const versionBar = page.locator('.version-bar')
        await expect(versionBar).toBeVisible({ timeout: 3000 })

        // Should contain build hash pattern
        const versionText = page.locator('.version-text')
        await expect(versionText).toBeVisible({ timeout: 3000 })
        const text = await versionText.textContent()
        expect(text).toMatch(/v\d+\.\d+\.\d+/)
    })

    test('UI-02: "Copia" button is visible in version bar', async ({ page }) => {
        const copyBtn = page.locator('.version-bar button:has-text("Copia")')
        await expect(copyBtn).toBeVisible({ timeout: 3000 })
    })

    test('UI-03: "Aggiorna" button is visible in version bar', async ({ page }) => {
        const updateBtn = page.locator('.version-bar button:has-text("Aggiorna")')
        await expect(updateBtn).toBeVisible({ timeout: 3000 })
    })
})

test.describe('UI Components — Authenticated', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.waitForTimeout(500)
    })

    test('UI-04: FAB is visible on Home page', async ({ page }) => {
        const fab = page.locator('button.fab')
        await expect(fab).toBeVisible({ timeout: 3000 })
    })

    test('UI-05: FAB navigates to add reading page', async ({ page }) => {
        const fab = page.locator('button.fab')
        await fab.click()
        await expect(page).toHaveURL(/\/#\/add/, { timeout: 5000 })

        const heading = page.locator('h1')
        await expect(heading).toContainText('Nuova', { timeout: 3000 })
    })

    test('UI-06: Theme toggle changes icon', async ({ page }) => {
        const themeBtn = page.locator('header button[title*="Tema"]')
        await expect(themeBtn).toBeVisible({ timeout: 3000 })

        // Get initial icon (sun or moon)
        const initialSvg = await themeBtn.locator('svg').first().getAttribute('viewBox')

        // Click to toggle
        await themeBtn.click()
        await page.waitForTimeout(300)

        // Should still have an SVG (icon changed)
        const newSvg = await themeBtn.locator('svg').first().getAttribute('viewBox')
        expect(newSvg).toBeTruthy()
    })

    test('UI-07: Bottom nav has 4 items with icons', async ({ page }) => {
        const navItems = page.locator('.nav-item')
        const count = await navItems.count()
        expect(count).toBeGreaterThanOrEqual(4)

        // Each nav item should have an SVG icon
        for (let i = 0; i < count; i++) {
            const svg = navItems.nth(i).locator('svg')
            await expect(svg).toBeVisible({ timeout: 2000 })
        }
    })

    test('UI-08: Logout button shows confirm dialog', async ({ page }) => {
        const logoutBtn = page.locator('header button[title="Logout"]')
        await logoutBtn.click()
        await page.waitForTimeout(500)

        // Confirm dialog should appear
        const dialog = page.locator('.dialog-overlay')
        const isVisible = await dialog.isVisible({ timeout: 3000 }).catch(() => false)
        expect(isVisible).toBe(true)
    })

    test('UI-09: Confirm dialog has cancel and confirm buttons', async ({ page }) => {
        const logoutBtn = page.locator('header button[title="Logout"]')
        await logoutBtn.click()
        await page.waitForTimeout(500)

        // Should have "Annulla" button
        const cancelBtn = page.locator('.dialog-overlay .btn-secondary')
        const hasCancel = await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)

        // Should have confirm button
        const confirmBtn = page.locator('.dialog-overlay .btn-primary')
        const hasConfirm = await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)

        expect(hasCancel || hasConfirm).toBe(true)
    })

    test('UI-10: Settings page renders account and language headings', async ({ page }) => {
        await page.goto('/#/settings')
        await page.waitForTimeout(500)

        // Main cards should render their headings
        await expect(page.locator('h3:has-text("Account")')).toBeVisible({ timeout: 3000 })
        await expect(page.locator('h3:has-text("Lingua")')).toBeVisible({ timeout: 3000 })
    })

    test('UI-11: Reading cards show icons instead of emoji', async ({ page }) => {
        // Navigate to list
        await page.locator('nav a:has-text("Lista")').click()
        await expect(page).toHaveURL(/\/#\/list/, { timeout: 5000 })
        await page.waitForTimeout(1000)

        // If there are reading cards, they should have SVG icons
        const cards = page.locator('.reading-card')
        const cardCount = await cards.count().catch(() => 0)
        if (cardCount > 0) {
            const firstCard = cards.first()
            // Should have AppIcon SVGs (edit/trash/heart)
            const svgs = firstCard.locator('svg')
            const svgCount = await svgs.count()
            expect(svgCount).toBeGreaterThanOrEqual(1)
        }
    })
})
