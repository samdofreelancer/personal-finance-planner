import { afterEach, describe, expect, it, vi } from 'vitest'
import { exportCsv, getAssumptions, getForecast, saveAssumptions, seedDefaults } from '../src/services/api.js'

const assumptions = {
  baseSalary: 42000000,
  januaryMultiplier: 2,
  living: 4000000,
  payoffInterest: null,
  payoffInterestConfirmed: false,
  raises: { 2027: 0.08, 2028: 0.09 },
}

afterEach(() => vi.unstubAllGlobals())

describe('API service', () => {
  it('loads assumptions as JSON', async () => {
    const payload = { baseSalary: 42000000 }
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

  it('serializes defined forecast assumptions and omits null values', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ months: [] }) })
    vi.stubGlobal('fetch', fetch)

    await getForecast(assumptions)
    const url = new URL(fetch.mock.calls[0][0], 'http://localhost')
    expect(url.pathname).toBe('/api/forecast')
    expect(url.searchParams.get('baseSalary')).toBe('42000000')
    expect(url.searchParams.get('januaryMultiplier')).toBe('2')
    expect(url.searchParams.get('raises[2027]')).toBe('0.08')
    expect(url.searchParams.has('payoffInterest')).toBe(false)
    expect(url.searchParams.get('payoffInterestConfirmed')).toBe('false')
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
