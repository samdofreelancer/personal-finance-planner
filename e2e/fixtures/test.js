import { test as base, expect } from '@playwright/test'
import { AssumptionsPage } from '../pages/AssumptionsPage.js'
import { DashboardPage } from '../pages/DashboardPage.js'
import { LoansPage } from '../pages/LoansPage.js'

export const test = base.extend({
  assumptionsPage: async ({ page }, use) => {
    await use(new AssumptionsPage(page))
  },
  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page))
  },
  loansPage: async ({ page }, use) => {
    await use(new LoansPage(page))
  },
})

test.beforeEach(async ({ request }) => {
  const response = await request.post('/api/assumptions/seed')
  expect(response.ok(), 'E2E database should accept synthetic seed data').toBeTruthy()
})

export { expect }
