import { readFile, writeFile } from 'node:fs/promises'

const reportPath = new URL('../coverage/lcov.info', import.meta.url)
const report = await readFile(reportPath, 'utf8')
const normalized = report
  .replace(/\\/g, '/')
  .replace(/^SF:src\//gm, 'SF:frontend/src/')
await writeFile(reportPath, normalized)
