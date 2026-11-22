import { test, expect } from '../fixtures/test.js'

test('keeps mobile navigation and header actions compact without wrapping', async ({ dashboardPage }) => {
  const { page } = dashboardPage
  await page.setViewportSize({ width: 390, height: 844 })
  await dashboardPage.open()

  const layout = await page.evaluate(() => {
    const nav = document.querySelector('.side-nav')
    const actions = [...document.querySelectorAll('.topbar-actions .button')]
    const breadcrumbPrefix = document.querySelector('.breadcrumb-prefix')
    return {
      navOverflow: nav.scrollWidth > nav.clientWidth,
      navWhiteSpace: getComputedStyle(nav.querySelector('.nav-link')).whiteSpace,
      actionWhiteSpaces: actions.map((button) => getComputedStyle(button).whiteSpace),
      breadcrumbPrefixHidden: getComputedStyle(breadcrumbPrefix).display === 'none',
      mobileNavLabels: [...document.querySelectorAll('.nav-label-mobile')].map((node) => getComputedStyle(node).display !== 'none' ? node.textContent.trim() : ''),
      householdAriaLabel: document.querySelector('#household-settings-button').getAttribute('aria-label'),
      householdTitle: document.querySelector('#household-settings-button').getAttribute('title'),
      documentWidth: document.documentElement.scrollWidth,
      viewportWidth: window.innerWidth,
    }
  })

  expect(layout.navOverflow).toBe(true)
  expect(layout.navWhiteSpace).toBe('nowrap')
  expect(layout.actionWhiteSpaces.every((value) => value === 'nowrap')).toBe(true)
  expect(layout.breadcrumbPrefixHidden).toBe(true)
  expect(layout.mobileNavLabels).toEqual(['Tổng quan', 'Giả định', 'Khoản vay', 'Theo năm', 'Theo tháng'])
  expect(layout.householdAriaLabel).toBe('Không gian hộ gia đình')
  expect(layout.householdTitle).toBe('Không gian hộ gia đình')
  expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth)
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
