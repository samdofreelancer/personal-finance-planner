import { test as base, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { AssumptionsPage } from '../pages/AssumptionsPage.js'
import { DashboardPage } from '../pages/DashboardPage.js'
import { LoansPage } from '../pages/LoansPage.js'

export const test = base.extend({
  // Authentication is a fixture dependency, not a beforeEach hook. This guarantees
  // every page object is constructed around the same authenticated Page instance.
  authenticatedPage: async ({ page }, use, testInfo) => {
    const ownerCredentials = {
      email: `e2e-${randomUUID()}-${testInfo.testId.slice(0, 140)}@planner.test`,
      password: 'e2e-owner-password-2026',
    }
    page.on('pageerror', (error) => console.error(`[browser pageerror] ${error.stack || error.message}`))
    await page.goto('/')
    await page.getByRole('button', { name: 'Chưa có tài khoản? Tạo tài khoản' }).click()
    await page.getByLabel('Email').fill(ownerCredentials.email)
    await page.getByLabel('Mật khẩu', { exact: true }).fill(ownerCredentials.password)
    await page.getByLabel('Xác nhận mật khẩu').fill(ownerCredentials.password)
    await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click()

    await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible()
    await expect(page.getByRole('heading', { name: /Kế hoạch tài chính/ })).toBeVisible()
    await expect(page.locator('#export-button')).toBeVisible()
    await expect(page.locator('#save-status')).toHaveText('Đã lưu trên máy chủ')
    await expect(page.locator('#annual-chart .chart-bar')).toHaveCount(5)
    await expect(page.locator('#annual-table tbody tr')).toHaveCount(5)
    await expect(page.locator('#monthly-table tbody tr')).toHaveCount(50)

    const initialProfile = await page.evaluate(async () => {
      const [assumptions, loans] = await Promise.all([
        fetch('/api/assumptions').then((response) => response.json()),
        fetch('/api/loans').then((response) => response.json()),
      ])
      return { income: assumptions.incomeChanges[0]?.amount, savings: assumptions.startingSavings,
        expenses: assumptions.expenses.length, loans: loans.length }
    })
    expect(initialProfile).toEqual({ income: 0, savings: 0, expenses: 0, loans: 0 })

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
