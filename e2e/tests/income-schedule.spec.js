import { test, expect } from '../fixtures/test.js'

test('stretches the income assumptions group across the available width on mobile', async ({ assumptionsPage }) => {
  await assumptionsPage.page.setViewportSize({ width: 393, height: 852 })
  await assumptionsPage.open()

  const layout = await assumptionsPage.page.evaluate(() => {
    const grid = document.querySelector('.input-groups').getBoundingClientRect()
    const income = document.querySelector('.input-group').getBoundingClientRect()
    return { gridWidth: grid.width, incomeWidth: income.width }
  })

  expect(layout.incomeWidth).toBeGreaterThanOrEqual(layout.gridWidth - 2)
})

test('creates and persists effective-month income changes reflected in monthly forecast', async ({ assumptionsPage }) => {
  await assumptionsPage.open()
  await expect(assumptionsPage.saveStatus).toHaveCount(0)

  await assumptionsPage.setIncomeSchedule([
    { effectiveMonth: '2026-11', amount: 50000000 },
    { effectiveMonth: '2027-04', amount: 60000000 },
    { effectiveMonth: '2028-01', amount: 65000000 },
  ])
  await assumptionsPage.save()
  await expect(assumptionsPage.saveToast).toHaveText('Đã lưu thay đổi thành công')

  await assumptionsPage.openForecast()
  await expect(assumptionsPage.monthlyRow('thg 3 2027').locator('td').nth(0)).toContainText('50.000.000')
  await expect(assumptionsPage.monthlyRow('thg 4 2027').locator('td').nth(0)).toContainText('60.000.000')
  await expect(assumptionsPage.monthlyRow('thg 12 2027').locator('td').nth(0)).toContainText('60.000.000')
  await expect(assumptionsPage.monthlyRow('thg 1 2028').locator('td').nth(0)).toContainText('130.000.000')

  await assumptionsPage.reload()
  await assumptionsPage.open()
  await expect(assumptionsPage.saveStatus).toHaveCount(0)
  await expect(assumptionsPage.page.locator('.income-change-row')).toHaveCount(7)
  await expect(assumptionsPage.incomeChangeRow(0).getByLabel('Số tiền thu nhập, mốc 1')).toHaveValue('50.000.000')
  await expect(assumptionsPage.incomeChangeRow(1).getByLabel('Tháng hiệu lực thu nhập, mốc 2')).toHaveValue('2027-04')
  await expect(assumptionsPage.incomeChangeRow(2).getByLabel('Tháng hiệu lực thu nhập, mốc 3')).toHaveValue('2028-01')
  await assumptionsPage.openForecast()
  await expect(assumptionsPage.monthlyRow('thg 4 2027').locator('td').nth(0)).toContainText('60.000.000')
})
