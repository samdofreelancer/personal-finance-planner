export const MONEY_FIELDS = [
  'baseSalary', 'startingSavings',
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
