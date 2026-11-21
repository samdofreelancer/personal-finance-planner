import { afterEach, describe, expect, it, vi } from 'vitest'
import { exportCsv, getAssumptions, getLoans, getForecast, saveAssumptions, saveLoans, seedDefaults } from '../src/services/api.js'

const assumptions = {
  incomeChanges: [{ effectiveMonth: '2026-11', amount: 42000000 }],
  januaryMultiplier: 2,
  startingSavings: 30000000,
  expenses: [{ name: 'Học phí', changes: [{ effectiveMonth: '2026-11', amount: 4000000 }] }],
}

afterEach(() => vi.unstubAllGlobals())

describe('API service', () => {
  it('loads assumptions as JSON', async () => {
    const payload = { incomeChanges: [{ effectiveMonth: '2026-11', amount: 42000000 }] }
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => payload })
    vi.stubGlobal('fetch', fetch)

    await expect(getAssumptions()).resolves.toEqual(payload)
    expect(fetch).toHaveBeenCalledWith('/api/assumptions')
  })

  it('saves assumptions with a JSON PUT request', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => assumptions })
    vi.stubGlobal('fetch', fetch)

    await expect(saveAssumptions(assumptions)).resolves.toEqual(assumptions)
    expect(fetch).toHaveBeenCalledWith('/api/assumptions', expect.objectContaining({
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(assumptions),
    }))
  })

  it('seeds default assumptions', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => assumptions })
    vi.stubGlobal('fetch', fetch)

    await expect(seedDefaults()).resolves.toEqual(assumptions)
    expect(fetch).toHaveBeenCalledWith('/api/assumptions/seed', { method: 'POST' })
  })

  it('loads saved loans and their repayment schedules', async () => {
    const payload = [{ id: 1, name: 'Vay mua nhà', openingPrincipal: 100000000, repayments: [] }]
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => payload })
    vi.stubGlobal('fetch', fetch)

    await expect(getLoans()).resolves.toEqual(payload)
    expect(fetch).toHaveBeenCalledWith('/api/loans')
  })

  it('saves the complete loan schedule with an atomic JSON PUT', async () => {
    const loans = [{ id: 1, name: 'Vay mua nhà', openingPrincipal: 100000000, repayments: [
      { id: 2, dueDate: '2026-11-20', principalAmount: 5000000, interestAmount: 100000 },
    ] }]
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => loans })
    vi.stubGlobal('fetch', fetch)

    await expect(saveLoans(loans)).resolves.toEqual(loans)
    expect(fetch).toHaveBeenCalledWith('/api/loans', expect.objectContaining({
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(loans),
    }))
  })

  it('uses a server validation message for failed responses', async () => {
    const fetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ message: 'living không hợp lệ' }),
    })
    vi.stubGlobal('fetch', fetch)

    await expect(getAssumptions()).rejects.toThrow('living không hợp lệ')
  })

  it('falls back when an error response is not JSON or has a blank message', async () => {
    const invalidJson = vi.fn().mockResolvedValue({ ok: false, json: async () => { throw new Error('bad json') } })
    vi.stubGlobal('fetch', invalidJson)
    await expect(getAssumptions()).rejects.toThrow('Không tải được giả định')

    const blankMessage = vi.fn().mockResolvedValue({ ok: false, json: async () => ({ message: '  ' }) })
    vi.stubGlobal('fetch', blankMessage)
    await expect(saveAssumptions(assumptions)).rejects.toThrow('Không lưu được giả định')
  })

  it('sends forecast assumptions and dynamic expense schedules as a JSON POST', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ months: [] }) })
    vi.stubGlobal('fetch', fetch)

    await getForecast(assumptions)
    expect(fetch).toHaveBeenCalledWith('/api/forecast', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(assumptions),
    }))
  })

  it('exports CSV through a temporary download link and revokes its URL', async () => {
    const blob = new Blob(['csv'])
    const fetch = vi.fn().mockResolvedValue({ ok: true, blob: async () => blob })
    const createObjectURL = vi.fn().mockReturnValue('blob:forecast')
    const revokeObjectURL = vi.fn()
    const click = vi.fn()
    vi.stubGlobal('fetch', fetch)
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL })
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(click)

    await exportCsv(assumptions)

    expect(fetch).toHaveBeenCalledWith('/api/export/csv', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(assumptions),
    }))

    expect(createObjectURL).toHaveBeenCalledWith(blob)
    expect(click).toHaveBeenCalledOnce()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:forecast')
    expect(document.querySelector('a[download]')).toBeNull()
  })

  it('reports CSV download failures without creating a blob', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ message: 'CSV export failed' }),
    }))
    await expect(exportCsv(assumptions)).rejects.toThrow('CSV export failed')
  })
})
