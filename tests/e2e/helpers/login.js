const BOT_USER = { username: 'bot', password: 'test1234', email: 'bot@pressione.test', role: 'user' }

export async function loginAsBot(page) {
  const expiresAt = new Date(Date.now() + 480 * 60 * 1000).toISOString()
  const session = {
    username: BOT_USER.username,
    email: BOT_USER.email,
    role: BOT_USER.role,
    birthDate: null,
    gender: null,
    profileCompleted: true,
    skipProfilePrompt: true,
    expiresAt
  }
  const sessionJson = JSON.stringify(session)

  // Navigate first, then inject session, then reload so initAuth picks it up
  await page.goto('/#/')
  await page.evaluate((json) => {
    localStorage.setItem('pressione_session', json)
  }, sessionJson)
  await page.reload()
  await page.waitForTimeout(2000)
  return page
}

export async function loginViaForm(page, username, password) {
  await page.goto('/#/login')
  await page.fill('#username', username)
  await page.fill('#password', password)
  await page.click('button[type="submit"]')
  try {
    await page.waitForURL(/\/#\/$/, { timeout: 10000 })
  } catch {
    const error = page.locator('.form-error')
    if (await error.isVisible({ timeout: 2000 }).catch(() => false)) {
      throw new Error('Login failed: ' + await error.textContent())
    }
    throw new Error('Login timed out')
  }
  return page
}

export async function confirmDialog(page) {
  const dialog = page.locator('.dialog-overlay')
  if (await dialog.isVisible({ timeout: 500 }).catch(() => false)) {
    await dialog.locator('.btn-primary').first().click()
    await page.waitForTimeout(300)
  }
}
