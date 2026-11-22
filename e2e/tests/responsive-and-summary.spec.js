import { test, expect } from '../fixtures/test.js'

test('uses a slide-out navigation drawer on mobile and keeps header actions on one line', async ({ dashboardPage }) => {
  const { page } = dashboardPage
  await page.setViewportSize({ width: 390, height: 844 })
  await dashboardPage.open()

  const layout = await page.evaluate(() => {
    const actions = [...document.querySelectorAll('.topbar-actions .button')]
    return {
      actionWhiteSpaces: actions.map((button) => getComputedStyle(button).whiteSpace),
      householdAriaLabel: document.querySelector('#household-settings-button').getAttribute('aria-label'),
      householdTitle: document.querySelector('#household-settings-button').getAttribute('title'),
      documentWidth: document.documentElement.scrollWidth,
      viewportWidth: window.innerWidth,
    }
  })

  expect(layout.actionWhiteSpaces.every((value) => value === 'nowrap')).toBe(true)
  expect(layout.householdAriaLabel).toBe('Không gian hộ gia đình')
  expect(layout.householdTitle).toBe('Không gian hộ gia đình')
  expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth)
  await expect(page.getByRole('button', { name: 'Mở menu điều hướng' })).toBeVisible()
  await expect(page.locator('.mobile-brand')).toContainText('planwise')
  await expect(page.locator('.breadcrumb strong')).toBeVisible()
  await expect(page.locator('.sidebar')).toBeHidden()

  await page.getByRole('button', { name: 'Mở menu điều hướng' }).click()
  await expect(page.locator('.sidebar')).toBeVisible()
  await expect(page.locator('.sidebar .side-nav')).toHaveCSS('display', 'grid')
  await expect(page.locator('.sidebar .nav-link')).toHaveCount(5)
  await expect(page.locator('.sidebar .nav-link').nth(1)).toContainText('Giả định tài chính')
  await expect(page.locator('.drawer-period strong')).toContainText('11/2026')
  await page.keyboard.press('Escape')
  await expect(page.locator('.sidebar')).toBeHidden()

  await page.getByRole('button', { name: 'Mở menu điều hướng' }).click()
  await page.locator('.drawer-backdrop').click({ position: { x: 380, y: 400 } })
  await expect(page.locator('.sidebar')).toBeHidden()

  await page.getByRole('button', { name: 'Mở menu điều hướng' }).click()
  await page.locator('.sidebar .nav-link[href="#assumptions"]').click()
  await expect(page.locator('.sidebar')).toBeHidden()
  await expect(page).toHaveURL(/#assumptions$/)
  await page.getByRole('button', { name: 'Mở menu điều hướng' }).click()
  await expect(page.locator('.sidebar .nav-link[href="#assumptions"]')).toHaveClass(/active/)
  await page.locator('.sidebar .drawer-close').click()
  await expect(page.locator('.sidebar')).toBeHidden()

  await page.setViewportSize({ width: 320, height: 740 })
  await expect(page.locator('.topbar-actions .button')).toHaveCount(3)
  const narrowLayout = await page.evaluate(() => ({
    width: document.documentElement.scrollWidth,
    viewport: window.innerWidth,
    buttons: [...document.querySelectorAll('.topbar-actions .button')].map((button) => ({
      text: button.innerText,
      height: button.getBoundingClientRect().height,
      whiteSpace: getComputedStyle(button).whiteSpace,
    })),
  }))
  expect(narrowLayout.width).toBeLessThanOrEqual(narrowLayout.viewport)
  expect(narrowLayout.buttons.every((button) => button.whiteSpace === 'nowrap' && button.height < 45)).toBe(true)

  await page.setViewportSize({ width: 1280, height: 900 })
  await expect(page.locator('.sidebar')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Mở menu điều hướng' })).toBeHidden()
  await expect(page.locator('#monthly-table tfoot tr')).toContainText('Tổng cộng kỳ')
  await expect(page.locator('#monthly-table thead th').first()).toHaveCSS('position', 'sticky')
})

test('loads sample assumptions into the preview without overwriting server data', async ({ assumptionsPage }) => {
  const { page } = assumptionsPage
  await assumptionsPage.open()
  const persistedIncome = await page.evaluate(async () => (await fetch('/api/assumptions')).json()).then((data) => data.incomeChanges[0].amount)

  page.once('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: 'Nạp dữ liệu mẫu' }).click()

  await expect(page.getByLabel('Số tiền thu nhập, mốc 1')).toHaveValue('25.000.000')
  await expect(page.getByLabel('Tiền tiết kiệm ban đầu')).toHaveValue('100.000.000')
  await expect(page.getByLabel('Số tiền khoản Chi phí gia đình, mốc 1')).toHaveValue('15.000.000')
  await expect(assumptionsPage.saveStatus).toContainText('Đã cập nhật mô phỏng · Chưa lưu')

  const stillPersistedIncome = await page.evaluate(async () => (await fetch('/api/assumptions')).json()).then((data) => data.incomeChanges[0].amount)
  expect(stillPersistedIncome).toBe(persistedIncome)
})
