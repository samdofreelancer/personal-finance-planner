import { describe, expect, it } from 'vitest'
import { MONEY_FIELDS, validateAssumptions } from '../src/utils/validation.js'

function validInput() {
  return {
    baseSalary: 42000000,
    januaryMultiplier: 2,
    startingSavings: 30000000,
    raises: { 2027: 0.08, 2028: 0.08, 2029: 0.08, 2030: 0.08 },
    expenses: [{ name: 'Sinh hoạt phí', changes: [{ effectiveMonth: '2026-11', amount: 4250000 }] }],
  }
}

describe('validateAssumptions', () => {
  it('accepts valid assumptions and zero-valued money inputs', () => {
    const input = validInput()
    for (const field of MONEY_FIELDS) input[field] = 0
    expect(validateAssumptions(input)).toEqual([])
  })

  it.each([['negative', -1], ['decimal', 1.5], ['unsafe integer', Number.MAX_SAFE_INTEGER + 1], ['string', '100']])(
    'rejects %s monetary values', (_label, value) => {
      const input = validInput()
      input.expenses[0].changes[0].amount = value
      expect(validateAssumptions(input)).toContain('Số tiền của khoản chi “Sinh hoạt phí” phải là VND nguyên không âm.')
    },
  )

  it.each([0, -1, 20.01, Number.NaN])('rejects invalid January multiplier %s', (value) => {
    const input = validInput()
    input.januaryMultiplier = value
    expect(validateAssumptions(input)).toContain('Hệ số lương tháng 1 phải lớn hơn 0 và không vượt quá 20.')
  })

  it('accepts January multiplier at the upper boundary', () => {
    const input = validInput()
    input.januaryMultiplier = 20
    expect(validateAssumptions(input)).toEqual([])
  })

  it.each([[2027, -0.01], [2028, 1.01], [2029, Number.NaN], [2030, undefined]])(
    'rejects an invalid raise for %i', (year, value) => {
      const input = validInput()
      input.raises[year] = value
      expect(validateAssumptions(input)).toContain(`Mức tăng lương năm ${year} phải từ 0% đến 100%.`)
    },
  )

  it('handles a missing raises map', () => {
    const missingRaises = validInput()
    missingRaises.raises = null
    expect(validateAssumptions(missingRaises)).toHaveLength(4)
  })

  it('rejects duplicate category names and duplicate effective months', () => {
    const duplicateNames = validInput()
    duplicateNames.expenses.push({ name: ' sinh hoạt phí ', changes: [{ effectiveMonth: '2026-11', amount: 1 }] })
    expect(validateAssumptions(duplicateNames)).toContain('Tên khoản chi không được trùng nhau.')

    const duplicateMonths = validInput()
    duplicateMonths.expenses[0].changes.push({ effectiveMonth: '2026-11', amount: 2 })
    expect(validateAssumptions(duplicateMonths)).toContain('Khoản chi “Sinh hoạt phí” bị trùng tháng hiệu lực.')
  })
})
