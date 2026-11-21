import { test as base, expect } from '@playwright/test'
import { AssumptionsPage } from '../pages/AssumptionsPage.js'
import { DashboardPage } from '../pages/DashboardPage.js'
import { LoansPage } from '../pages/LoansPage.js'

const ownerCredentials = { email: 'e2e-owner@planner.test', password: 'e2e-owner-password-2026' }

export const test = base.extend({
  // Authentication is a fixture dependency, not a beforeEach hook. This guarantees
  // every page object is constructed around the same authenticated Page instance.
  authenticatedPage: async ({ page }, use) => {
    page.on('pageerror', (error) => console.error(`[browser pageerror] ${error.stack || error.message}`))
    await page.goto('/')
    const bootstrapResponse = await page.request.get('/api/auth/bootstrap')
    expect(bootstrapResponse.ok(), 'E2E backend should expose auth bootstrap status').toBeTruthy()
    const bootstrap = await bootstrapResponse.json()

    if (bootstrap.registrationAvailable) {
      await page.getByLabel('Email').fill(ownerCredentials.email)
      await page.getByLabel('Mật khẩu', { exact: true }).fill(ownerCredentials.password)
      await page.getByLabel('Xác nhận mật khẩu').fill(ownerCredentials.password)
      await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click()
    } else {
      await page.getByLabel('Email').fill(ownerCredentials.email)
      await page.getByLabel('Mật khẩu', { exact: true }).fill(ownerCredentials.password)
      await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    }

    await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible()
    await expect(page.getByRole('heading', { name: /Kế hoạch tài chính/ })).toBeVisible()
    await expect(page.locator('#export-button')).toBeVisible()
    await expect(page.locator('#save-status')).toHaveText('Đã lưu trên máy chủ')

    const seedResult = await page.evaluate(async () => {
      const csrfResponse = await fetch('/api/csrf', { credentials: 'same-origin' })
      const csrf = await csrfResponse.json()
      const response = await fetch('/api/assumptions/seed', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'X-XSRF-TOKEN': csrf.token },
      })
      const sessionResponse = await fetch('/api/auth/me', { credentials: 'same-origin' })
      return { seedStatus: response.status, sessionStatus: sessionResponse.status }
    })
    expect(seedResult.seedStatus, 'E2E database should accept synthetic seed data').toBe(200)
    expect(seedResult.sessionStatus, 'The authenticated browser session should survive seed/reset').toBe(200)

    await page.reload()
    const reloadSessionStatus = await page.evaluate(async () => (await fetch('/api/auth/me')).status)
    expect(reloadSessionStatus, 'Session cookie must survive a full browser reload').toBe(200)
    await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible()
    await expect(page.locator('#save-status')).toHaveText('Đã lưu trên máy chủ')

    await use(page)
  },
  assumptionsPage: async ({ authenticatedPage }, use) => {
    await use(new AssumptionsPage(authenticatedPage))
  },
  dashboardPage: async ({ authenticatedPage }, use) => {
    await use(new DashboardPage(authenticatedPage))
  },
  loansPage: async ({ authenticatedPage }, use) => {
    await use(new LoansPage(authenticatedPage))
  },
})

export { expect }
