import { test, expect } from '../fixtures/test.js'

test('uses single-purpose rows in the mobile drawer and keeps the header uncluttered', async ({ dashboardPage }) => {
  const { page } = dashboardPage
  await page.setViewportSize({ width: 390, height: 844 })
  await dashboardPage.open()
  await expect(page.locator('#annual-chart svg')).toHaveAttribute('viewBox', /^0 0 420 /)

  const layout = await page.evaluate(() => {
    return {
      householdAriaLabel: document.querySelector('#household-settings-button').getAttribute('aria-label'),
      householdTitle: document.querySelector('#household-settings-button').getAttribute('title'),
      topbarActionsDisplay: getComputedStyle(document.querySelector('.topbar-actions')).display,
      documentWidth: document.documentElement.scrollWidth,
      viewportWidth: window.innerWidth,
    }
  })

  expect(layout.householdAriaLabel).toBe('Không gian hộ gia đình')
  expect(layout.householdTitle).toBe('Không gian hộ gia đình')
  expect(layout.topbarActionsDisplay).toBe('none')
  expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth)
  await expect(page.getByRole('button', { name: 'Mở menu điều hướng' })).toBeVisible()
  await expect(page.locator('.mobile-brand')).toContainText('planwise')
  await expect(page.locator('.breadcrumb strong')).toBeVisible()
  await expect(page.locator('#annual-table thead th').first()).toHaveCSS('position', 'sticky')
  await expect(page.locator('#monthly-table thead th').first()).toHaveCSS('position', 'sticky')
  await expect(page.locator('#monthly-table tbody th[scope="row"]').first()).toHaveCSS('position', 'sticky')
  const mobileTables = await page.evaluate(() => [...document.querySelectorAll('.table-scroll')].map((element) => ({
    clientWidth: element.clientWidth,
    scrollWidth: element.scrollWidth,
  })))
  expect(mobileTables.some((table) => table.scrollWidth > table.clientWidth)).toBe(true)
  const stickyColumns = await page.evaluate(() => ['#annual-table', '#monthly-table'].map((selector) => {
    const table = document.querySelector(selector)
    const scroller = table.closest('.table-scroll')
    scroller.scrollLeft = 180
    const firstCell = table.querySelector('thead th:first-child')
    return {
      cellLeft: firstCell.getBoundingClientRect().left,
      containerLeft: scroller.getBoundingClientRect().left,
      scrollLeft: scroller.scrollLeft,
    }
  }))
  expect(stickyColumns.every((column) => column.scrollLeft > 0 && Math.abs(column.cellLeft - column.containerLeft) < 2)).toBe(true)
  const mobileHeader = await page.evaluate(() => ({ height: document.querySelector('.topbar').getBoundingClientRect().height }))
  expect(mobileHeader.height).toBeLessThan(100)
  await expect(page.locator('.sidebar')).toBeHidden()

  await page.getByRole('button', { name: 'Mở menu điều hướng' }).click()
  await expect(page.locator('.sidebar')).toBeVisible()
  await expect(page.locator('.sidebar .side-nav')).toHaveCSS('display', 'grid')
  await expect(page.locator('.sidebar .nav-link')).toHaveCount(5)
  await expect(page.locator('.sidebar .nav-link').nth(1)).toContainText('Giả định tài chính')
  const mobileNavigationRows = await page.locator('.sidebar .nav-link').evaluateAll((links) => links.map((link) => link.getBoundingClientRect().top))
  expect(mobileNavigationRows.every((top, index) => index === 0 || top > mobileNavigationRows[index - 1])).toBe(true)
  await expect(page.locator('.drawer-actions .drawer-action')).toHaveCount(3)
  const drawerActionRows = await page.locator('.drawer-actions .drawer-action').evaluateAll((buttons) => buttons.map((button) => ({
    height: button.getBoundingClientRect().height,
    width: button.getBoundingClientRect().width,
    whiteSpace: getComputedStyle(button).whiteSpace,
    display: getComputedStyle(button).display,
  })))
  expect(drawerActionRows.every((button) => button.height >= 48 && button.width > 200 && button.whiteSpace === 'nowrap' && button.display === 'flex')).toBe(true)
  await expect(page.locator('.drawer-mobile-heading')).toHaveCSS('display', 'flex')
  await expect(page.locator('.drawer-close')).toBeVisible()
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
  await expect(page.locator('.topbar-actions')).toBeHidden()
  const narrowLayout = await page.evaluate(() => ({
    width: document.documentElement.scrollWidth,
    viewport: window.innerWidth,
    buttons: [...document.querySelectorAll('.drawer-actions .drawer-action')].map((button) => ({
      text: button.innerText,
      height: button.getBoundingClientRect().height,
      whiteSpace: getComputedStyle(button).whiteSpace,
    })),
  }))
  expect(narrowLayout.width).toBeLessThanOrEqual(narrowLayout.viewport)
  expect(narrowLayout.buttons.every((button) => button.whiteSpace === 'nowrap' && button.height >= 48)).toBe(true)

  await page.setViewportSize({ width: 1280, height: 900 })
  await expect(page.locator('.sidebar')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Mở menu điều hướng' })).toBeHidden()
  await expect(page.locator('.topbar-actions .button')).toHaveCount(0)
  await expect(page.locator('.sidebar .drawer-actions')).toBeVisible()
  const desktopActionRows = await page.locator('.sidebar .drawer-action').evaluateAll((buttons) => buttons.map((button) => ({ top: button.getBoundingClientRect().top, width: button.getBoundingClientRect().width, height: button.getBoundingClientRect().height })))
  expect(desktopActionRows).toHaveLength(3)
  expect(desktopActionRows.every((button, index) => button.width > 150 && button.height >= 48 && (index === 0 || button.top > desktopActionRows[index - 1].top))).toBe(true)
  await expect(page.locator('#monthly-table tfoot tr')).toContainText('Tổng cộng kỳ')
  await expect(page.locator('#monthly-table tfoot tr th')).toHaveCSS('font-weight', '800')
  await expect(page.locator('#monthly-table thead th').first()).toHaveCSS('position', 'sticky')
  const desktopLayout = await page.evaluate(() => ({
    width: document.documentElement.scrollWidth,
    viewport: window.innerWidth,
    topbarHeight: document.querySelector('.topbar').getBoundingClientRect().height,
  }))
  expect(desktopLayout.width).toBeLessThanOrEqual(desktopLayout.viewport)
  expect(desktopLayout.topbarHeight).toBeLessThanOrEqual(80)

  await page.goto('/#assumption')
  await expect(page.locator('#assumptions-form')).toBeVisible()
  await expect(page.locator('#save-status')).toHaveText('Đã lưu trên máy chủ')
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
