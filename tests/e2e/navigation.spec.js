import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsBot(page)
  })

  test('should show bottom navigation', async ({ page }) => {
    await expect(page.locator('.app-nav')).toBeVisible({ timeout: 3000 })
    const items = page.locator('.nav-item')
    expect(await items.count()).toBeGreaterThanOrEqual(3)
  })

  test('should navigate to statistics', async ({ page }) => {
    await page.click('.nav-item:has-text("Analisi")')
    await page.waitForURL(/\/#\/analisi/, { timeout: 5000 })
    await expect(page.locator('h1')).toContainText('Analisi')
  })

  test('should navigate to settings', async ({ page }) => {
    await page.click('.nav-item:has-text("Altro")')
    await page.waitForURL(/\/#\/settings/, { timeout: 5000 })
    await expect(page.locator('h1')).toContainText('Impostazioni')
  })

  test('should show user info on settings', async ({ page }) => {
    await page.goto('/#/settings')
    await page.waitForTimeout(1000)
    await expect(page.locator('.card').first()).toContainText('bot', { timeout: 5000 })
  })

  test('should have a visible logout button', async ({ page }) => {
    // Logout button is in the top bar with title="Logout"
    const count = await page.locator('.topbar-btn[title="Logout"]').count()
    expect(count).toBeGreaterThanOrEqual(1)
  })
})
