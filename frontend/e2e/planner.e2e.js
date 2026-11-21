import { test, expect } from '@playwright/test'
import { readFile } from 'node:fs/promises'

test.beforeEach(async ({ request }) => {
  const response = await request.post('/api/assumptions/seed')
  expect(response.ok(), 'E2E database should accept synthetic seed data').toBeTruthy()
})

test('shows the dashboard and complete forecast horizon', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: /Kế hoạch tài chính/ })).toBeVisible()
  await expect(page.locator('#monthly-table tbody tr')).toHaveCount(50)
  await expect(page.locator('#annual-table tbody tr')).toHaveCount(5)
  await expect(page.locator('#annual-table')).toContainText('2030')
})

test('creates and persists a user-defined expense schedule across reload', async ({ page }) => {
  await page.goto('/#assumptions')
  await expect(page.locator('#save-status')).toHaveText('Đã lưu trên máy chủ')

  await page.getByRole('button', { name: '+ Thêm khoản chi' }).click()
  const expense = page.locator('.expense-editor').last()
  await expense.getByLabel('Tên khoản chi 4').fill('E2E - Học phí')
  await expense.getByLabel('Tháng hiệu lực khoản 4, mốc 1').fill('2026-11')
  await expense.getByLabel('Số tiền khoản E2E - Học phí, mốc 1').fill('1500000')
  await expense.getByRole('button', { name: '+ Thêm mốc thay đổi' }).click()
  await expense.getByLabel('Tháng hiệu lực khoản 4, mốc 2').fill('2027-01')
  await expense.getByLabel('Số tiền khoản E2E - Học phí, mốc 2').fill('2000000')

  await page.locator('#save-assumptions-button').click()
  await expect(page.locator('#save-status')).toHaveText('Đã lưu trên máy chủ')
  await page.reload()

  await expect(page.locator('#save-status')).toHaveText('Đã lưu trên máy chủ')
  await expect(page.getByLabel('Tên khoản chi 4')).toHaveValue('E2E - Học phí')
  await expect(page.locator('#monthly-table')).toContainText('E2E - Học phí')
  await expect(page.locator('#monthly-table tbody tr').filter({ hasText: 'thg 11 2026' }).first())
    .toContainText('1.500.000')
  await expect(page.locator('#monthly-table tbody tr').filter({ hasText: 'thg 1 2027' }).first())
    .toContainText('2.000.000')
})

test('creates and persists a loan with a dated repayment', async ({ page }) => {
  await page.goto('/#loans')
  const panel = page.locator('.loan-schedule-panel')
  await panel.getByRole('button', { name: '＋ Thêm khoản vay' }).click()
  const loan = panel.locator('.loan-card').last()
  await loan.getByLabel('Tên khoản vay').fill('E2E - Vay thử nghiệm')
  await loan.getByLabel('Dư nợ gốc ban đầu (VND)').fill('10000000')
  await loan.getByRole('button', { name: '＋ Thêm kỳ trả' }).click()
  await loan.getByLabel('Ngày trả').fill('2026-12-15')
  await loan.getByLabel('Tiền gốc (VND)').fill('500000')
  await loan.getByLabel('Tiền lãi (VND)').fill('50000')
  await panel.getByRole('button', { name: /Lưu/ }).click()

  await expect(panel.getByRole('status')).toHaveText('Đã lưu trên máy chủ')
  await page.reload()

  const savedLoan = page.locator('.loan-schedule-panel .loan-card').last()
  await expect(savedLoan.getByLabel('Tên khoản vay')).toHaveValue('E2E - Vay thử nghiệm')
  await expect(savedLoan.locator('input[type="date"]')).toHaveValue('2026-12-15')
})

test('exports the displayed plan as a CSV download', async ({ page }) => {
  await page.goto('/')
  const downloadPromise = page.waitForEvent('download')
  await page.locator('#export-button').click()
  const download = await downloadPromise

  expect(download.suggestedFilename()).toBe('ke-hoach-tai-chinh-2026-2030.csv')
  const content = await readFile(await download.path(), 'utf8')
  expect(content).toContain('2026')
  expect(content).toContain('Sinh hoạt phí')
})
