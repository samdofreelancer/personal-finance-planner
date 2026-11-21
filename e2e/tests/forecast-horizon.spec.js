import { test, expect } from '../fixtures/test.js'

test('changing the forecast horizon updates every dashboard section and persists after save', async ({ page }) => {
  await page.goto('/#assumptions')
  const requestedHorizons = []
  page.on('request', (request) => {
    if (request.url().endsWith('/api/forecast') && request.method() === 'POST') {
      requestedHorizons.push(JSON.parse(request.postData()).forecastYears)
    }
  })

  const horizon = page.getByLabel('Thời gian dự phóng')
  await expect(horizon).toBeVisible()
  await expect(horizon.locator('option')).toHaveCount(6)
  await expect(horizon).toHaveValue('4')
  const selectFontSize = await horizon.evaluate((element) => getComputedStyle(element).fontSize)
  const inputFontSize = await page.locator('#baseSalary').evaluate((element) => getComputedStyle(element).fontSize)
  expect(selectFontSize).toBe(inputFontSize)
  await horizon.selectOption('2')
  await expect.poll(() => requestedHorizons.at(-1)).toBe(2)

  await expect(page.locator('#monthly-table tbody tr')).toHaveCount(26)
  await expect(page.locator('#annual-table tbody tr')).toHaveCount(3)
  await expect(page.locator('.outlook-period').first()).toContainText('11/2026 — 12/2028')
  await expect(page.locator('.outlook-result-note')).toContainText('12/2028')
  await expect(page.locator('.section-meta').first()).toContainText('2028')
  await expect(page.locator('#annual-chart')).toContainText('2028')

  await page.locator('#save-assumptions-button').click()
  await expect(page.locator('#save-status')).toContainText('Đã lưu trên máy chủ')
  await page.reload()

  await expect(page.getByLabel('Thời gian dự phóng')).toHaveValue('2')
  await expect(page.locator('#monthly-table tbody tr')).toHaveCount(26)
  await expect(page.locator('#annual-table tbody tr')).toHaveCount(3)
  await expect(page.locator('.outlook-period').first()).toContainText('11/2026 — 12/2028')
})
