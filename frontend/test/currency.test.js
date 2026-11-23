import { describe, expect, it } from 'vitest'
import { formatCompactCurrency, formatCurrency, formatNumber, parseCurrencyInput } from '../src/utils/currency.js'

describe('currency formatting', () => {
  it('formats VND with Vietnamese thousands separators and currency symbol', () => {
    expect(formatCurrency(42000000)).toBe('42.000.000 ₫')
    expect(formatCurrency(-1250000)).toBe('-1.250.000 ₫')
  })

  it('formats editable amounts without adding a currency suffix', () => {
    expect(formatNumber(42000000)).toBe('42.000.000')
    expect(formatNumber(0)).toBe('0')
  })

  it('formats compact chart amounts with an explicit VND symbol', () => {
    expect(formatCompactCurrency(42500000)).toBe('42,5 tr ₫')
    expect(formatCompactCurrency(-125000000)).toBe('-125 tr ₫')
  })

  it('normalizes invalid, decimal and nullish values safely', () => {
    expect(formatNumber('not-a-number')).toBe('0')
    expect(formatCurrency(null)).toBe('0 ₫')
    expect(formatNumber(12.6)).toBe('13')
  })

  it('parses raw and grouped currency input into integer VND', () => {
    expect(parseCurrencyInput('42.000.000')).toBe(42000000)
    expect(parseCurrencyInput('42,000,000 ₫')).toBe(42000000)
    expect(parseCurrencyInput('')).toBe('')
    expect(parseCurrencyInput(null)).toBe('')
  })
})
