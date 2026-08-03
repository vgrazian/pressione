import { test, expect } from '@playwright/test'
import { loginAsBot } from './helpers/login.js'

/**
 * End-to-End Workflow Test — Bot User
 *
 * Simulates a complete user session: login → add reading → list → stats → report → share → settings → logout.
 * Run: npx playwright test tests/e2e/workflow.spec.js --reporter=line
 */

test.describe('Workflow: Complete User Journey', () => {
    test('W-01: Full workflow — add reading, check stats, generate report, share link, settings', async ({ page }) => {
        await loginAsBot(page)

        // ── 1. Home ────────────────────────────────────────────────
        await expect(page.locator('h1')).toContainText('Ciao', { timeout: 5000 })

        // ── 2. Add a reading ───────────────────────────────────────
        await page.locator('button.fab').click()
        await expect(page).toHaveURL(/\/#\/add/, { timeout: 5000 })
        await page.fill('#systolic', '128')
        await page.fill('#diastolic', '82')
        await page.fill('#heartRate', '72')
        await page.locator('button:has-text("Salva")').first().click()
        // Wait for redirect after save
        await page.waitForTimeout(3000)

        // ── 3. Add second reading (different time of day) ──────────
        await page.locator('button.fab').click()
        await expect(page).toHaveURL(/\/#\/add/, { timeout: 5000 })
        await page.fill('#systolic', '145')
        await page.fill('#diastolic', '95')
        await page.fill('#heartRate', '80')
        await page.locator('button:has-text("Salva")').first().click()
        await page.waitForTimeout(3000)

        // ── 4. Reading List ────────────────────────────────────────
        await page.locator('nav a:has-text("Lista")').click()
        await expect(page).toHaveURL(/\/#\/list/, { timeout: 5000 })
        await page.waitForTimeout(2000)
        const cards = page.locator('.reading-card, .swipe-container, .readings-list > *')
        const hasContent = await cards.first().isVisible({ timeout: 5000 }).catch(() => false)
        expect(hasContent).toBe(true)
        await page.locator('nav a:has-text("Report")').click()
        await expect(page).toHaveURL(/\/#\/report/, { timeout: 5000 })
        await page.waitForTimeout(2000)

        // Check that stats summary is visible
        const comparisonTable = page.locator('.comparison-table, h3:has-text("Confronto")')
        await expect(comparisonTable.first()).toBeVisible({ timeout: 5000 })

        // Toggle to grouped view
        const groupedBtn = page.locator('button:has-text("Per fascia")')
        if (await groupedBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await groupedBtn.click()
            await page.waitForTimeout(500)
        }

        // ── 7. Download PDF ────────────────────────────────────────
        const pdfBtn = page.locator('button:has-text("Scarica PDF")')
        if (await pdfBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            const [download] = await Promise.all([
                page.waitForEvent('download', { timeout: 15000 }).catch(() => null),
                pdfBtn.click()
            ])
            if (download) {
                expect(download.suggestedFilename()).toContain('pressione_report')
            }
        }

        // ── 8. Generate share link (best-effort: needs real Supabase) ──
        const genLinkBtn = page.locator('button:has-text("Genera Link")')
        if (await genLinkBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await genLinkBtn.click()
            await page.waitForTimeout(2000)
            // May show error (no Supabase in test) or success — either is fine
        }

        // ── 9. Settings ────────────────────────────────────────────
        await page.locator('nav a:has-text("Altro")').click()
        await expect(page).toHaveURL(/\/#\/settings/, { timeout: 5000 })
        await page.waitForTimeout(1000)

        // Account section visible
        await expect(page.locator('h3:has-text("Account")').first()).toBeVisible({ timeout: 3000 })

        // Time bands section
        await expect(page.locator('h3:has-text("Fasce Orarie")').first()).toBeVisible({ timeout: 3000 })

        // Keep-Alive section
        await expect(page.locator('h3:has-text("Keep-Alive")').first()).toBeVisible({ timeout: 3000 })

        // Version info at bottom
        await expect(page.locator('text=/Pressione v\\d/')).toBeVisible({ timeout: 3000 })

        // ── 10. Change language to English and back ─────────────────
        const enBtn = page.locator('button:has-text("English")')
        if (await enBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await enBtn.click()
            await page.waitForTimeout(500)
            await expect(page.locator('h1')).toContainText('Settings', { timeout: 3000 })
            // Switch back
            const itBtn = page.locator('button:has-text("Italiano")')
            if (await itBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
                await itBtn.click()
                await page.waitForTimeout(500)
            }
        }

        // ── 11. Logout ─────────────────────────────────────────────
        const logoutBtn = page.locator('header button[title="Logout"]')
        await logoutBtn.click()
        await page.waitForTimeout(500)

        // Handle confirm dialog
        const confirmBtn = page.locator('.dialog-overlay .btn-primary')
        if (await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await confirmBtn.click()
        }

        // Should redirect to login
        await expect(page).toHaveURL(/\/#\/login/, { timeout: 5000 })
        await expect(page.locator('h1')).toContainText('Pressione', { timeout: 3000 })
    })
})
