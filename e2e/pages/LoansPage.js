import { BasePage } from './BasePage.js'

export class LoansPage extends BasePage {
  constructor(page) {
    super(page)
    this.panel = page.locator('.loan-schedule-panel')
    this.saveStatus = this.panel.getByRole('status')
    this.savedLoan = this.panel.locator('.loan-card').last()
    this.savedLoanNameInput = this.savedLoan.getByLabel('Tên khoản vay')
    this.savedRepaymentDateInput = this.savedLoan.locator('input[type="date"]')
  }

  async open() {
    await this.goto('/#loans')
  }

  async createLoanWithRepayment({ name, principal, repaymentDate, repaymentPrincipal, repaymentInterest }) {
    await this.panel.getByRole('button', { name: '＋ Thêm khoản vay' }).click()
    const loan = this.panel.locator('.loan-card').last()
    await loan.getByLabel('Tên khoản vay').fill(name)
    await loan.getByLabel('Dư nợ gốc ban đầu (VND)').fill(principal)
    await loan.getByRole('button', { name: '＋ Thêm kỳ trả' }).click()
    await loan.getByLabel('Ngày trả').fill(repaymentDate)
    await loan.getByLabel('Tiền gốc (VND)').fill(repaymentPrincipal)
    await loan.getByLabel('Tiền lãi (VND)').fill(repaymentInterest)
  }

  async save() {
    await this.panel.getByRole('button', { name: /Lưu/ }).click()
  }

  async reload() {
    await this.page.reload()
  }
}
