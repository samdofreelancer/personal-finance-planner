import { BasePage } from './BasePage.js'

export class AssumptionsPage extends BasePage {
  constructor(page) {
    super(page)
    this.saveStatus = page.locator('#save-status')
    this.monthlyTable = page.locator('#monthly-table')
  }

  async open() {
    await this.goto('/#assumptions')
  }

  async addExpenseSchedule({ name, initialMonth, initialAmount, nextMonth, nextAmount }) {
    const expenseIndex = (await this.page.locator('.expense-editor').count()) + 1
    await this.page.getByRole('button', { name: '+ Thêm khoản chi' }).click()
    const expense = this.page.locator('.expense-editor').last()
    await expense.getByLabel(`Tên khoản chi ${expenseIndex}`).fill(name)
    await expense.getByLabel(`Tháng hiệu lực khoản ${expenseIndex}, mốc 1`).fill(initialMonth)
    await expense.getByLabel(`Số tiền khoản ${name}, mốc 1`).fill(initialAmount)
    await expense.getByRole('button', { name: '+ Thêm mốc thay đổi' }).click()
    await expense.getByLabel(`Tháng hiệu lực khoản ${expenseIndex}, mốc 2`).fill(nextMonth)
    await expense.getByLabel(`Số tiền khoản ${name}, mốc 2`).fill(nextAmount)
  }

  get expenseNameInput() {
    return this.page.locator('.expense-editor').last().getByLabel(/^Tên khoản chi/)
  }

  async save() {
    await this.page.locator('#save-assumptions-button').click()
  }

  async reload() {
    await this.page.reload()
  }

  monthlyRow(monthLabel) {
    return this.monthlyTable.locator('tbody tr').filter({ hasText: monthLabel }).first()
  }
}
