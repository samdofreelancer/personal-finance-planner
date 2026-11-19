import { describe, expect, it } from 'vitest'
import { MONEY_FIELDS, validateAssumptions } from '../src/utils/validation.js'

function validInput() {
  return {
    baseSalary: 42000000,
    januaryMultiplier: 2,
    living: 4250000,
    childThrough2026: 2500000,
    childQ12027: 2750000,
    childFromApr2027: 3250000,
    rentThroughJan2027: 3750000,
    rentFromFeb2027: 5750000,
    startingSavings: 30000000,
    raises: { 2027: 0.08, 2028: 0.08, 2029: 0.08, 2030: 0.08 },
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
      input.living = value
      expect(validateAssumptions(input)).toContain('living phải là số VND nguyên không âm.')
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
})
