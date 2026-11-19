export const MONEY_FIELDS = [
  'baseSalary', 'living', 'childThrough2026', 'childQ12027',
  'childFromApr2027', 'rentThroughJan2027', 'rentFromFeb2027',
  'startingSavings',
]

export function validateAssumptions(input) {
  const errors = []

  for (const field of MONEY_FIELDS) {
    if (!Number.isSafeInteger(input[field]) || input[field] < 0) {
      errors.push(`${field} phải là số VND nguyên không âm.`)
    }
  }
  if (!Number.isFinite(input.januaryMultiplier) ||
      input.januaryMultiplier <= 0 || input.januaryMultiplier > 20) {
    errors.push('Hệ số lương tháng 1 phải lớn hơn 0 và không vượt quá 20.')
  }
  for (const year of [2027, 2028, 2029, 2030]) {
    const value = input.raises?.[year]
    if (!Number.isFinite(value) || value < 0 || value > 1) {
      errors.push(`Mức tăng lương năm ${year} phải từ 0% đến 100%.`)
    }
  }
  return errors
}
