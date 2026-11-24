import { test, expect } from '../fixtures/test.js'

test('creates and persists a user-defined expense schedule across reload', async ({ assumptionsPage }) => {
  await assumptionsPage.open()
  await expect(assumptionsPage.saveStatus).toHaveCount(0)

  await assumptionsPage.addExpenseSchedule({
    name: 'E2E - Học phí',
    initialMonth: '2026-11',
    initialAmount: '1500000',
    nextMonth: '2027-01',
    nextAmount: '2000000',
  })
  await assumptionsPage.save()
  await expect(assumptionsPage.saveToast).toHaveText('Đã lưu thay đổi thành công')
  await assumptionsPage.reload()

  await expect(assumptionsPage.saveStatus).toHaveCount(0)
  await expect(assumptionsPage.expenseNameInput).toHaveValue('E2E - Học phí')
  await assumptionsPage.openForecast()
  await expect(assumptionsPage.monthlyTable).toContainText('E2E - Học phí')
  await expect(assumptionsPage.monthlyRow('thg 11 2026')).toContainText('1.500.000')
  await expect(assumptionsPage.monthlyRow('thg 1 2027')).toContainText('2.000.000')
})
