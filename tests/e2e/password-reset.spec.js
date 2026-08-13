import { test, expect } from '@playwright/test'

/**
 * Password Reset Flow E2E Tests
 *
 * Tests the forgot-password UI on the login page and the reset-password view.
 * Run: npx playwright test tests/e2e/password-reset.spec.js --reporter=line
 */

test.describe('Password Reset — Login Page', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/#/login')
        await page.waitForTimeout(500)
    })

    test('PR-01: "Password dimenticata?" shows email recovery form', async ({ page }) => {
        // Click the forgot password link
        const forgotLink = page.locator('button:has-text("Password dimenticata")')
        await expect(forgotLink).toBeVisible({ timeout: 3000 })
        await forgotLink.click()
        await page.waitForTimeout(300)

        // Email input should be visible
        const emailInput = page.locator('#forgot-email')
        await expect(emailInput).toBeVisible({ timeout: 3000 })

        // Submit button should be visible
        const submitBtn = page.locator('button:has-text("Invia richiesta")')
        await expect(submitBtn).toBeVisible({ timeout: 3000 })
    })

    test('PR-02: Email recovery form validates email format', async ({ page }) => {
        // Open recovery form
        await page.locator('button:has-text("Password dimenticata")').click()
        await page.waitForTimeout(300)

        // Submit with invalid email (no @)
        await page.fill('#forgot-email', 'notanemail')
        await page.locator('button:has-text("Invia richiesta")').click()
        await page.waitForTimeout(500)

        // Should show error
        const errorMsg = page.locator('.form-error')
        const hasError = await errorMsg.isVisible({ timeout: 2000 }).catch(() => false)
        expect(hasError).toBe(true)
    })

    test('PR-03: "Torna al login" returns to login form', async ({ page }) => {
        // Open recovery form
        await page.locator('button:has-text("Password dimenticata")').click()
        await page.waitForTimeout(300)

        // Click back
        const backBtn = page.locator('button:has-text("Torna al login")')
        await expect(backBtn).toBeVisible({ timeout: 3000 })
        await backBtn.click()
        await page.waitForTimeout(300)

        // Login form should be visible again
        const loginBtn = page.locator('button:has-text("Accedi")')
        await expect(loginBtn).toBeVisible({ timeout: 3000 })

        // Username field should be visible
        const usernameInput = page.locator('#username')
        await expect(usernameInput).toBeVisible({ timeout: 3000 })
    })

    test('PR-04: Empty email disables the send request button', async ({ page }) => {
        // Open recovery form
        await page.locator('button:has-text("Password dimenticata")').click()
        await page.waitForTimeout(300)

        // With empty email the button is disabled (no request can be sent)
        const submitBtn = page.locator('button:has-text("Invia richiesta")')
        await expect(submitBtn).toBeDisabled({ timeout: 3000 })
    })
})

test.describe('Password Reset — Reset Page', () => {
    test('PR-05: /reset-password without token shows disabled form', async ({ page }) => {
        await page.goto('/#/reset-password')
        await page.waitForTimeout(500)

        // Should show heading
        const heading = page.locator('h1')
        await expect(heading).toBeVisible({ timeout: 3000 })

        // Submit button is disabled while fields are empty
        const submitBtn = page.locator('button:has-text("Aggiorna password")')
        await expect(submitBtn).toBeDisabled({ timeout: 3000 })
    })

    test('PR-06: /reset-password shows "Torna al login" link', async ({ page }) => {
        await page.goto('/#/reset-password')
        await page.waitForTimeout(500)

        const backLink = page.locator('a:has-text("Torna al login")')
        await expect(backLink).toBeVisible({ timeout: 3000 })
    })

    test('PR-07: /reset-password disables submit with empty fields', async ({ page }) => {
        await page.goto('/#/reset-password?token=test-token-123')
        await page.waitForTimeout(500)

        // Submit button is disabled while both password fields are empty
        const submitBtn = page.locator('button:has-text("Aggiorna password")')
        await expect(submitBtn).toBeDisabled({ timeout: 3000 })
    })
})
