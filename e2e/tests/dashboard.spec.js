import { test, expect } from '../fixtures/test.js'

test('shows the dashboard and complete forecast horizon', async ({ dashboardPage }) => {
  await dashboardPage.open()

  await expect(dashboardPage.heading).toBeVisible()
  await dashboardPage.openMonthlyForecast()
  await expect(dashboardPage.monthlyRows).toHaveCount(50)
  await dashboardPage.openAnnualAnalysis()
  await expect(dashboardPage.annualRows).toHaveCount(5)
  await expect(dashboardPage.annualTable).toContainText('2030')
})
