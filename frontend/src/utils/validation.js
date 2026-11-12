export const MONEY_FIELDS = [
  'baseSalary', 'living', 'childThrough2026', 'childQ12027',
  'childFromApr2027', 'rentThroughJan2027', 'rentFromFeb2027',
  'loanPrincipal', 'fixedPayment', 'novInterest', 'decInterest',
  'startingSavings',
]

export function validateAssumptions(input) {
  const errors = []

  for (const field of MONEY_FIELDS) {
    if (!Number.isSafeInteger(input[field]) || input[field] < 0) {
      errors.push(`${field} phải là số VND nguyên không âm.`)
    }
  }
  if (input.payoffInterest !== null && input.payoffInterest !== undefined &&
      (!Number.isSafeInteger(input.payoffInterest) || input.payoffInterest < 0)) {
    errors.push('Lãi tất toán phải là số VND nguyên không âm hoặc để trống nếu chưa xác nhận.')
  }
  if (input.payoffInterestConfirmed && (input.payoffInterest === null || input.payoffInterest === undefined)) {
    errors.push('Nhập số lãi tất toán trước khi đánh dấu đã xác nhận.')
  }
  if (typeof input.payoffInterestConfirmed !== 'boolean') {
    errors.push('Trạng thái xác nhận lãi tất toán không hợp lệ.')
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
  if (input.novInterest > input.fixedPayment || input.decInterest > input.fixedPayment) {
    errors.push('Lãi từng kỳ không thể lớn hơn khoản trả cố định của kỳ đó.')
  }
  return errors
}
