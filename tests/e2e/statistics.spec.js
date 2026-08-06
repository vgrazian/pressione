import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

test.describe('Statistics', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsBot(page)
    // Add a reading first so statistics have data (best effort — may fail gracefully)
    await page.goto('/#/add')
    await page.waitForSelector('#systolic', { timeout: 5000 })
    await page.fill('#systolic', '120')
    await page.fill('#diastolic', '80')
    await page.fill('#heartRate', '72')
    await page.click('button[type="submit"]')
    // Wait but don't fail if redirect doesn't happen (Supabase may be unavailable)
    await page.waitForTimeout(2000)
  })

  test('should display statistics page', async ({ page }) => {
    await page.goto('/#/analisi')
    await page.waitForTimeout(1000)
    await expect(page.locator('h1')).toContainText('Analisi', { timeout: 5000 })
  })

  test('should show report page', async ({ page }) => {
    await page.goto('/#/analisi')
    await page.waitForTimeout(1000)
    // Analisi page shows with at least some content
    const heading = page.locator('h1')
    const hasHeading = await heading.isVisible({ timeout: 3000 }).catch(() => false)
    expect(hasHeading).toBe(true)
  })
})
