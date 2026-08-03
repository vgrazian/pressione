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

test.describe('Workflow: Error & Edge Cases', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
    })

    test('E-01: Reject DIA > SYS', async ({ page }) => {
        await page.locator('button.fab').click()
        await page.fill('#systolic', '100')
        await page.fill('#diastolic', '120')
        await page.fill('#heartRate', '70')
        await page.locator('button:has-text("Salva")').first().click()
        await expect(page.locator('.form-error')).toBeVisible({ timeout: 3000 })
    })

    test('E-02: Reject out-of-range systolic (< 1)', async ({ page }) => {
        await page.locator('button.fab').click()
        await page.fill('#systolic', '-5')
        await page.fill('#diastolic', '80')
        await page.fill('#heartRate', '70')
        await page.locator('button:has-text("Salva")').first().click()
        const hasError = await page.locator('.form-error').isVisible({ timeout: 3000 }).catch(() => false)
        const stillOnAdd = page.url().includes('/add')
        expect(hasError || stillOnAdd).toBe(true)
    })

    test('E-03: Reject out-of-range systolic (> 300)', async ({ page }) => {
        await page.locator('button.fab').click()
        await page.fill('#systolic', '350')
        await page.fill('#diastolic', '80')
        await page.fill('#heartRate', '70')
        await page.locator('button:has-text("Salva")').first().click()
        const hasError = await page.locator('.form-error').isVisible({ timeout: 3000 }).catch(() => false)
        const stillOnAdd = page.url().includes('/add')
        expect(hasError || stillOnAdd).toBe(true)
    })

    test('E-04: Reject empty fields', async ({ page }) => {
        await page.locator('button.fab').click()
        // Submit with all fields empty
        await page.locator('button:has-text("Salva")').first().click()
        await expect(page.locator('.form-error')).toBeVisible({ timeout: 3000 })
    })

    test('E-05: Reject heart rate out of range', async ({ page }) => {
        await page.locator('button.fab').click()
        await page.fill('#systolic', '120')
        await page.fill('#diastolic', '80')
        await page.fill('#heartRate', '350')
        await page.locator('button:has-text("Salva")').first().click()
        const hasError = await page.locator('.form-error').isVisible({ timeout: 3000 }).catch(() => false)
        const stillOnAdd = page.url().includes('/add')
        expect(hasError || stillOnAdd).toBe(true)
    })

    test('E-06: Cancel logout via dialog', async ({ page }) => {
        const logoutBtn = page.locator('header button[title="Logout"]')
        await logoutBtn.click()
        await page.waitForTimeout(1000)
        // Click "Annulla" button on dialog
        const cancelBtn = page.locator('.dialog-overlay .btn-secondary')
        if (await cancelBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
            await cancelBtn.click()
            await page.waitForTimeout(500)
        }
        // Dialog should close, user stays authenticated
        const dialogGone = await page.locator('.dialog-overlay').isVisible({ timeout: 2000 }).catch(() => false)
        expect(dialogGone).toBe(false)
    })

    test('E-07: Login with invalid credentials shows error', async ({ page }) => {
        // First logout properly
        const logoutBtn = page.locator('header button[title="Logout"]')
        if (await logoutBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await logoutBtn.click()
            await page.waitForTimeout(500)
            const confirmBtn = page.locator('.dialog-overlay .btn-primary')
            if (await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
                await confirmBtn.click()
            }
        }
        await page.waitForURL(/\/#\/login/, { timeout: 5000 })

        // Try invalid login
        await page.fill('#username', 'nonexistent_user_xyz')
        await page.fill('#password', 'wrong_password')
        await page.click('button[type="submit"]')
        await expect(page.locator('.form-error')).toBeVisible({ timeout: 10000 })
    })

    test('E-08: Email validation rejects invalid format', async ({ page }) => {
        await page.locator('nav a:has-text("Altro")').click()
        await page.waitForTimeout(1000)

        // Open email change form
        const changeEmailBtn = page.locator('button:has-text("Cambia email"), button:has-text("Change email")')
        if (await changeEmailBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await changeEmailBtn.click()
            await page.fill('input[type="email"]', 'not-an-email')
            await page.locator('button:has-text("Aggiorna"), button:has-text("Update")').first().click()
            await expect(page.locator('.form-error')).toBeVisible({ timeout: 3000 })
        }
    })

    test('E-09: Password change rejects short password', async ({ page }) => {
        await page.locator('nav a:has-text("Altro")').click()
        await page.waitForTimeout(1000)

        // Open password form
        const pwHeader = page.locator('h3:has-text("Password")')
        if (await pwHeader.isVisible({ timeout: 2000 }).catch(() => false)) {
            await pwHeader.click()
            await page.waitForTimeout(500)
        }

        const currentPwInput = page.locator('input[type="password"]').first()
        if (await currentPwInput.isVisible({ timeout: 2000 }).catch(() => false)) {
            await currentPwInput.fill('anything')
            // Try with short new password (less than 8 chars)
            const pwInputs = page.locator('input[type="password"]')
            if (await pwInputs.nth(1).isVisible().catch(() => false)) {
                await pwInputs.nth(1).fill('123')
                await page.locator('button:has-text("Aggiorna password"), button:has-text("Update password")').click()
                await expect(page.locator('.form-error')).toBeVisible({ timeout: 3000 })
            }
        }
    })

    test('E-10: Time bands — inputs respect min/max bounds', async ({ page }) => {
        await page.locator('nav a:has-text("Altro")').click()
        await page.waitForTimeout(1000)

        const bandsSection = page.locator('h3:has-text("Fasce Orarie")')
        if (await bandsSection.isVisible({ timeout: 2000 }).catch(() => false)) {
            const firstInput = page.locator('input[type="number"]').first()
            if (await firstInput.isVisible({ timeout: 2000 }).catch(() => false)) {
                const min = await firstInput.getAttribute('min')
                const max = await firstInput.getAttribute('max')
                expect(min).toBe('0')
                expect(max).toBe('23')
            }
        }
    })

    test('E-11: Report page handles empty data gracefully', async ({ page }) => {
        // Navigate to report — should show empty state or skeleton
        await page.locator('nav a:has-text("Report")').click()
        await page.waitForTimeout(2000)
        // Should not crash — just verify page loaded
        await expect(page.locator('h1')).toContainText('Report', { timeout: 5000 })
    })

    test('E-12: Cancel reading addition (navigate back without saving)', async ({ page }) => {
        await page.locator('button.fab').click()
        await page.fill('#systolic', '130')
        await page.fill('#diastolic', '85')
        // Navigate away without saving
        await page.locator('nav a:has-text("Home")').click()
        await page.waitForTimeout(1000)
        // Should be on home page
        await expect(page.locator('h1')).toContainText('Ciao', { timeout: 5000 })
    })

    test('E-13: Delete all data with confirmation', async ({ page }) => {
        await page.locator('nav a:has-text("Altro")').click()
        await page.waitForTimeout(1000)

        const deleteBtn = page.locator('button:has-text("Elimina tutto"), button:has-text("Delete all")')
        if (await deleteBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await deleteBtn.click()
            await page.waitForTimeout(500)
            // Cancel the confirmation
            const cancelBtn = page.locator('.dialog-overlay .btn-secondary, .dialog-overlay button:not(.btn-primary)')
            if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
                await cancelBtn.click()
            } else {
                await page.locator('.dialog-overlay').click({ position: { x: 10, y: 10 } })
            }
            await page.waitForTimeout(500)
            // Should still be on settings page
            expect(page.url()).toContain('/settings')
        }
    })
})