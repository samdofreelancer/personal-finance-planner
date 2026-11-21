import { test, expect } from '../fixtures/test.js'

test('creates and persists a loan with a dated repayment', async ({ loansPage }) => {
  await loansPage.open()
  await loansPage.createLoanWithRepayment({
    name: 'E2E - Vay thử nghiệm',
    principal: '10000000',
    repaymentDate: '2026-12-15',
    repaymentPrincipal: '500000',
    repaymentInterest: '50000',
  })
  await loansPage.save()

  await expect(loansPage.saveStatus).toHaveText('Đã lưu trên máy chủ')
  await loansPage.reload()

  await expect(loansPage.savedLoanNameInput).toHaveValue('E2E - Vay thử nghiệm')
  await expect(loansPage.savedRepaymentDateInput).toHaveValue('2026-12-15')
})
