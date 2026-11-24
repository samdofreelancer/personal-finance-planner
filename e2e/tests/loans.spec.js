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

  await expect(loansPage.saveToast).toHaveText('Đã lưu lịch trả nợ thành công')
  await loansPage.reload()

  await expect(loansPage.savedLoanNameInput).toHaveValue('E2E - Vay thử nghiệm')
  await expect(loansPage.savedLoanPrincipalInput).toHaveValue('10.000.000')
  await expect(loansPage.savedRepaymentDateInput).toHaveValue('2026-12-15')
  await expect(loansPage.savedRepaymentPrincipalInput).toHaveValue('500.000')
  await expect(loansPage.savedRepaymentInterestInput).toHaveValue('50.000')
})
