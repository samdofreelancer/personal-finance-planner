export const MONEY_FIELDS = [
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
  const forecastYears = input.forecastYears
  if (!Number.isInteger(forecastYears) || forecastYears < 1 || forecastYears > 6) {
    errors.push('Thời gian dự phóng phải từ 1 đến 6 năm.')
  }
  if (!Array.isArray(input.incomeChanges) || input.incomeChanges.length === 0) {
    errors.push('Lịch thu nhập không được để trống.')
  } else {
    const months = new Set()
    let hasBaseline = false
    for (const change of input.incomeChanges) {
      const month = change?.effectiveMonth
      if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month || '')) {
        errors.push('Tháng hiệu lực thu nhập không hợp lệ.')
      } else if (month < '2026-11') {
        errors.push('Tháng hiệu lực thu nhập không được trước 11/2026.')
      } else if (months.has(month)) {
        errors.push('Lịch thu nhập bị trùng tháng hiệu lực.')
      } else {
        months.add(month)
        if (month === '2026-11') hasBaseline = true
      }
      if (!Number.isSafeInteger(change?.amount) || change.amount < 0) {
        errors.push('Số tiền thu nhập phải là VND nguyên không âm.')
      }
    }
    if (!hasBaseline) errors.push('Lịch thu nhập phải có mốc cơ sở 11/2026.')
  }
  const expenses = input.expenses
  if (!Array.isArray(expenses)) {
    errors.push('Danh sách khoản chi không hợp lệ.')
    return errors
  }
  const names = new Set()
  for (const expense of expenses) {
    const name = expense?.name?.trim()
    if (!name || name.length > 120) {
      errors.push('Tên khoản chi phải có từ 1 đến 120 ký tự.')
      continue
    }
    const normalizedName = name.toLocaleLowerCase('vi')
    if (names.has(normalizedName)) errors.push('Tên khoản chi không được trùng nhau.')
    names.add(normalizedName)
    if (!Array.isArray(expense.changes) || expense.changes.length === 0) {
      errors.push(`Khoản chi “${name}” cần ít nhất một mốc hiệu lực.`)
      continue
    }
    const months = new Set()
    for (const change of expense.changes) {
      if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(change?.effectiveMonth || '')) {
        errors.push(`Tháng hiệu lực của khoản chi “${name}” không hợp lệ.`)
      } else if (months.has(change.effectiveMonth)) {
        errors.push(`Khoản chi “${name}” bị trùng tháng hiệu lực.`)
      } else {
        months.add(change.effectiveMonth)
      }
      if (!Number.isSafeInteger(change?.amount) || change.amount < 0) {
        errors.push(`Số tiền của khoản chi “${name}” phải là VND nguyên không âm.`)
      }
    }
  }
  return errors
}
