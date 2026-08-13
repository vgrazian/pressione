import { test, expect } from '@playwright/test'
import { loginAsBot, expandAllSections } from './helpers/login.js'

test.describe('Settings — Profile', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/settings')
        await page.waitForTimeout(500)
        await expandAllSections(page)
    })

    test('S-01: Profile section has date of birth and gender fields', async ({ page }) => {
        const birthDateInput = page.locator('input[type="date"]').first()
        await expect(birthDateInput).toBeVisible({ timeout: 3000 })

        const genderSelect = page.locator('select.form-input').first()
        await expect(genderSelect).toBeVisible({ timeout: 3000 })
    })

    test('S-02: Profile section has anagrafica fields (nome, cognome, CF, telefono)', async ({ page }) => {
        await expect(page.locator('input[placeholder="Mario"]')).toBeVisible({ timeout: 3000 })
        await expect(page.locator('input[placeholder="Rossi"]')).toBeVisible({ timeout: 3000 })
        await expect(page.locator('input[placeholder*="RSSMRA"]')).toBeVisible({ timeout: 3000 })
        await expect(page.locator('input[placeholder*="333"]')).toBeVisible({ timeout: 3000 })
    })

    test('S-03: Save profile button is disabled when no changes', async ({ page }) => {
        const saveBtn = page.locator('button:has-text("Salva profilo")')
        await expect(saveBtn).toBeDisabled({ timeout: 3000 })
    })
})

test.describe('Settings — Password', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/settings')
        await page.waitForTimeout(500)
    })

    test('S-04: Password section is collapsible', async ({ page }) => {
        const pwHeader = page.locator('.collapsible__header:has-text("Cambio Password")')
        await expect(pwHeader).toBeVisible({ timeout: 3000 })
        await pwHeader.click()
        await page.waitForTimeout(300)
        // Fields should appear
        const currentPwInput = page.locator('input[type="password"]').first()
        await expect(currentPwInput).toBeVisible({ timeout: 3000 })
    })
})

test.describe('Settings — Reminders', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/settings')
        await page.waitForTimeout(500)
        await expandAllSections(page)
    })

    test('S-05: Reminders section is visible', async ({ page }) => {
        const remindersHeader = page.locator('.collapsible__header:has-text("Promemoria")')
        await expect(remindersHeader).toBeVisible({ timeout: 3000 })
    })

    test('S-06: Add reminder button is visible', async ({ page }) => {
        const addBtn = page.getByRole('button', { name: '+ Aggiungi', exact: true })
        // Scroll down to find it
        await addBtn.scrollIntoViewIfNeeded()
        await expect(addBtn).toBeVisible({ timeout: 3000 })
    })

    test('S-07: Adding a reminder shows time input and day chips', async ({ page }) => {
        const addBtn = page.getByRole('button', { name: '+ Aggiungi', exact: true })
        await addBtn.scrollIntoViewIfNeeded()
        await addBtn.click()
        await page.waitForTimeout(300)

        // Time input should appear
        const timeInput = page.locator('input[type="time"]')
        const hasTimeInput = await timeInput.isVisible({ timeout: 2000 }).catch(() => false)
        expect(hasTimeInput).toBe(true)

        // Day chips should appear
        const dayChips = page.locator('.day-chip')
        const chipCount = await dayChips.count()
        expect(chipCount).toBeGreaterThanOrEqual(7)
    })
})

test.describe('Settings — Data Management', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/settings')
        await page.waitForTimeout(500)
        await expandAllSections(page)
    })

    test('S-08: Export CSV button is visible', async ({ page }) => {
        const exportBtn = page.locator('button:has-text("Esporta CSV")')
        await exportBtn.scrollIntoViewIfNeeded()
        await expect(exportBtn).toBeVisible({ timeout: 3000 })
    })

    test('S-09: Backup JSON button is visible', async ({ page }) => {
        const backupBtn = page.locator('button:has-text("Backup (JSON)")')
        await backupBtn.scrollIntoViewIfNeeded()
        await expect(backupBtn).toBeVisible({ timeout: 3000 })
    })

    test('S-10: Generate test data button is visible', async ({ page }) => {
        const genBtn = page.locator('button:has-text("Genera Dati di Test")')
        await genBtn.scrollIntoViewIfNeeded()
        await expect(genBtn).toBeVisible({ timeout: 3000 })
    })
})

test.describe('Settings — Keep-Alive', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/settings')
        await page.waitForTimeout(500)
        await expandAllSections(page)
    })

    // Keep-Alive UI was removed from Settings; feature kept only as dead code.
    test.skip('S-11: Keep-Alive section is visible with toggle', async ({ page }) => {
        const kaHeader = page.locator('h3:has-text("Keep-Alive")')
        await kaHeader.scrollIntoViewIfNeeded()
        await expect(kaHeader).toBeVisible({ timeout: 3000 })

        const toggle = page.locator('.toggle-switch')
        const hasToggle = await toggle.isVisible({ timeout: 2000 }).catch(() => false)
        expect(hasToggle).toBe(true)
    })
})

test.describe('Settings — Danger Zone', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/settings')
        await page.waitForTimeout(500)
        await expandAllSections(page)
    })

    test('S-12: Danger zone has delete button', async ({ page }) => {
        const dangerHeader = page.locator('h4:has-text("Zona Pericolosa")')
        await dangerHeader.scrollIntoViewIfNeeded()
        await expect(dangerHeader).toBeVisible({ timeout: 3000 })

        const deleteBtn = page.locator('button:has-text("Elimina Tutte le Misurazioni")')
        await expect(deleteBtn).toBeVisible({ timeout: 3000 })
    })

    test('S-13: Delete all shows confirm dialog, cancel does not delete', async ({ page }) => {
        const deleteBtn = page.locator('button:has-text("Elimina Tutte le Misurazioni")')
        await deleteBtn.scrollIntoViewIfNeeded()
        await deleteBtn.click()
        await page.waitForTimeout(500)

        // Dialog should appear
        const dialog = page.locator('.dialog-overlay')
        const isVisible = await dialog.isVisible({ timeout: 3000 }).catch(() => false)
        expect(isVisible).toBe(true)

        if (isVisible) {
            // Click cancel
            const cancelBtn = page.locator('.dialog-overlay .btn-secondary')
            await cancelBtn.click()
            await page.waitForTimeout(500)

            // Dialog should close
            const dialogGone = await page.locator('.dialog-overlay').isVisible({ timeout: 2000 }).catch(() => false)
            expect(dialogGone).toBe(false)

            // Should still be on settings page
            const settingsHeading = page.locator('h1')
            await expect(settingsHeading).toContainText('Impostazioni', { timeout: 3000 })
        }
    })
})

test.describe('Settings — Language', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/settings')
        await page.waitForTimeout(500)
    })

    test('S-14: Language toggle switches between IT and EN', async ({ page }) => {
        const enBtn = page.locator('button:has-text("English")')
        await enBtn.scrollIntoViewIfNeeded()
        await expect(enBtn).toBeVisible({ timeout: 3000 })
        await enBtn.click()
        await page.waitForTimeout(500)

        // Heading should change to English
        const heading = page.locator('h1')
        await expect(heading).toContainText('Settings', { timeout: 3000 })

        // Switch back
        const itBtn = page.locator('button:has-text("Italiano")')
        await itBtn.scrollIntoViewIfNeeded()
        await itBtn.click()
        await page.waitForTimeout(500)
        await expect(heading).toContainText('Impostazioni', { timeout: 3000 })
    })
})

test.describe('Settings — Cache & Diagnostics', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsBot(page)
        await page.goto('/#/settings')
        await page.waitForTimeout(500)
        await expandAllSections(page)
    })

    test('S-15: Force update button is visible', async ({ page }) => {
        const updateBtn = page.locator('button:has-text("Forza aggiornamento")')
        await updateBtn.scrollIntoViewIfNeeded()
        await expect(updateBtn).toBeVisible({ timeout: 3000 })
    })

    test('S-16: Share diagnostics button is visible', async ({ page }) => {
        const diagBtn = page.locator('button:has-text("Condividi diagnostica")')
        await diagBtn.scrollIntoViewIfNeeded()
        await expect(diagBtn).toBeVisible({ timeout: 3000 })
    })
})
