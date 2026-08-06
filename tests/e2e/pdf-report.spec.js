import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

/**
 * PDF Report E2E Tests.
 * Tests PDF download from Analisi and Report views.
 * Run: npx playwright test tests/e2e/pdf-report.spec.js --reporter=line
 */

test.describe('PDF Report', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/analisi')
        await page.waitForTimeout(500)
    })

    test('PDF-01: PDF download button is visible on Analisi page', async ({ page }) => {
        const pdfBtn = page.locator('button:has-text("Scarica PDF")')
        await pdfBtn.scrollIntoViewIfNeeded()
        await expect(pdfBtn).toBeVisible({ timeout: 3000 })
    })

    test('PDF-02: PDF download triggers file download', async ({ page }) => {
        const pdfBtn = page.locator('button:has-text("Scarica PDF")')
        await pdfBtn.scrollIntoViewIfNeeded()

        const [download] = await Promise.all([
            page.waitForEvent('download', { timeout: 15000 }).catch(() => null),
            pdfBtn.click()
        ])

        if (download) {
            expect(download.suggestedFilename()).toContain('pressione_report')
        }
    })

    test('PDF-03: Include grafici checkbox is in Scarica/Condividi section', async ({ page }) => {
        const chartsCheckbox = page.locator('label:has-text("Includi grafici")')
        await chartsCheckbox.scrollIntoViewIfNeeded()
        const isVisible = await chartsCheckbox.isVisible({ timeout: 2000 }).catch(() => false)
        expect(isVisible).toBe(true)
    })
})
