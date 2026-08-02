import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

test.describe('Home Screen', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsBot(page)
  })

  test('should show home page after login', async ({ page }) => {
    // Should show greeting with bot username
    await expect(page.locator('h1')).toContainText('Ciao', { timeout: 5000 })
  })

  test('should show either latest reading or empty state', async ({ page }) => {
    // Either latest-card or empty-state should be visible
    const hasLatest = await page.locator('.latest-card').isVisible({ timeout: 3000 }).catch(() => false)
    const hasEmpty = await page.locator('.empty-state').isVisible({ timeout: 3000 }).catch(() => false)
    expect(hasLatest || hasEmpty).toBe(true)
  })

  test('should navigate to add reading page', async ({ page }) => {
    // Click the "Nuova" button - could be in header or empty state
    const addBtn = page.locator('button:has-text("Nuova"), .empty-state button:has-text("Aggiungi")').first()
    await addBtn.click()
    await page.waitForURL(/\/#\/add/, { timeout: 5000 })
    await expect(page.locator('h1')).toContainText('Nuova')
  })
})
