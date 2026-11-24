import { test, expect } from '../fixtures/test.js'

test('changing the forecast horizon updates every dashboard section and persists after save', async ({ authenticatedPage: page }) => {
  await page.goto('/plan/assumptions')
  const requestedHorizons = []
  page.on('request', (request) => {
    if (request.url().endsWith('/api/forecast') && request.method() === 'POST') {
      requestedHorizons.push(JSON.parse(request.postData()).forecastYears)
    }
  })

  const horizon = page.locator('#forecastYears')
  await expect(horizon).toBeVisible()
  await expect(horizon.locator('option')).toHaveCount(6)
  await expect(horizon).toHaveValue('4')
  const selectFontSize = await horizon.evaluate((element) => getComputedStyle(element).fontSize)
  const inputFontSize = await page.getByLabel('Số tiền thu nhập, mốc 1').evaluate((element) => getComputedStyle(element).fontSize)
  expect(selectFontSize).toBe(inputFontSize)
  await horizon.selectOption('2')
  await expect.poll(() => requestedHorizons.at(-1)).toBe(2)

  await page.locator('#save-assumptions-button').click()
  await expect(page.locator('.app-toast')).toContainText('Đã lưu thay đổi thành công')
  await page.locator('.side-nav a[href="/forecast/monthly"]').click()
  await expect(page.locator('#monthly-table tbody tr')).toHaveCount(26)
  await page.locator('.side-nav a[href="/analytics/annual"]').click()
  await expect(page.locator('#annual-table tbody tr')).toHaveCount(3)
  await expect(page.locator('#annual-chart')).toContainText('2028')
  await page.locator('.side-nav .nav-link[href="/dashboard"]').click()
  await expect(page.locator('.outlook-period').first()).toContainText('11/2026 — 12/2028')
  await expect(page.locator('.outlook-result-note')).toContainText('12/2028')
  await page.reload()

  await page.locator('.side-nav a[href="/plan/assumptions"]').click()
  await expect(page.locator('#forecastYears')).toHaveValue('2')
  await page.locator('.side-nav a[href="/forecast/monthly"]').click()
  await expect(page.locator('#monthly-table tbody tr')).toHaveCount(26)
})
