import { readFile } from 'node:fs/promises'
import { test, expect } from '../fixtures/test.js'

test('exports the displayed plan as a CSV download', async ({ dashboardPage }) => {
  await dashboardPage.open()
  const download = await dashboardPage.exportCsv()

  expect(download.suggestedFilename()).toBe('ke-hoach-tai-chinh-2026-2030.csv')
  const content = await readFile(await download.path(), 'utf8')
  expect(content).toContain('2026')
  expect(content).toContain('Sinh hoạt phí')
})
