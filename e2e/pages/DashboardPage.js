import { BasePage } from './BasePage.js'

export class DashboardPage extends BasePage {
  constructor(page) {
    super(page)
    this.heading = page.getByRole('heading', { name: /Kế hoạch tài chính/ })
    this.monthlyRows = page.locator('#monthly-table tbody tr')
    this.annualRows = page.locator('#annual-table tbody tr')
    this.annualTable = page.locator('#annual-table')
    this.exportButton = page.locator('#export-button')
  }

  async open() {
    await this.heading.waitFor({ state: 'visible' })
  }

  async exportCsv() {
    const downloadPromise = this.page.waitForEvent('download')
    await this.exportButton.click()
    return downloadPromise
  }
}
