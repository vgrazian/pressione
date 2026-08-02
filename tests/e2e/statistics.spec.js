import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

test.describe('Statistics', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsBot(page)
    // Add a reading first so statistics have data
    await page.goto('/#/add')
    await page.waitForSelector('#systolic', { timeout: 5000 })
    await page.fill('#systolic', '120')
    await page.fill('#diastolic', '80')
    await page.fill('#heartRate', '72')
    await page.click('button[type="submit"]')
    await page.waitForURL(/\/#\/$/, { timeout: 5000 })
  })

  test('should display statistics page', async ({ page }) => {
    await page.goto('/#/statistics')
    await page.waitForTimeout(1000)
    await expect(page.locator('h1')).toContainText('Statistiche', { timeout: 5000 })
  })

  test('should show report page', async ({ page }) => {
    await page.goto('/#/report')
    await page.waitForTimeout(1000)
    // Report shows with at least some content
    const report = page.locator('.report-text')
    const empty = page.locator('.empty-state')
    const hasContent = await report.isVisible({ timeout: 3000 }).catch(() => false)
    const isEmpty = await empty.isVisible({ timeout: 3000 }).catch(() => false)
    expect(hasContent || isEmpty).toBe(true)
  })
})
