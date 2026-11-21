import { describe, expect, it } from 'vitest'
import { MONEY_FIELDS, validateAssumptions } from '../src/utils/validation.js'

function validInput() {
  return {
    januaryMultiplier: 2,
    startingSavings: 30000000,
    forecastYears: 4,
    incomeChanges: [
      { effectiveMonth: '2026-11', amount: 42000000 },
      { effectiveMonth: '2027-04', amount: 50000000 },
    ],
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

  it.each([0, 7, 1.5, undefined])('rejects invalid forecast horizon %s', (years) => {
    const input = validInput()
    input.forecastYears = years
    expect(validateAssumptions(input)).toContain('Thời gian dự phóng phải từ 1 đến 6 năm.')
  })

  it('requires a baseline income change and accepts changes beyond the selected horizon', () => {
    const input = validInput()
    input.forecastYears = 6
    expect(validateAssumptions(input)).toEqual([])
    input.incomeChanges = [{ effectiveMonth: '2026-12', amount: 42000000 }]
    expect(validateAssumptions(input)).toContain('Lịch thu nhập phải có mốc cơ sở 11/2026.')
  })

  it('rejects duplicate, invalid and pre-baseline income months', () => {
    const duplicate = validInput()
    duplicate.incomeChanges.push({ effectiveMonth: '2026-11', amount: 50000000 })
    expect(validateAssumptions(duplicate)).toContain('Lịch thu nhập bị trùng tháng hiệu lực.')

    const invalidMonth = validInput()
    invalidMonth.incomeChanges[1].effectiveMonth = '2026-13'
    expect(validateAssumptions(invalidMonth)).toContain('Tháng hiệu lực thu nhập không hợp lệ.')

    const beforeBaseline = validInput()
    beforeBaseline.incomeChanges[1].effectiveMonth = '2026-10'
    expect(validateAssumptions(beforeBaseline)).toContain('Tháng hiệu lực thu nhập không được trước 11/2026.')
  })

  it.each([[-1], [1.5], [Number.MAX_SAFE_INTEGER + 1], [null]])(
    'rejects an invalid scheduled income amount %s', (value) => {
      const input = validInput()
      input.incomeChanges[0].amount = value
      expect(validateAssumptions(input)).toContain('Số tiền thu nhập phải là VND nguyên không âm.')
    },
  )

  it('handles a missing income schedule', () => {
    const missingSchedule = validInput()
    missingSchedule.incomeChanges = null
    expect(validateAssumptions(missingSchedule)).toContain('Lịch thu nhập không được để trống.')
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
