const numberFormatter = new Intl.NumberFormat('vi-VN', {
  maximumFractionDigits: 0,
})

const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

export function formatNumber(value) {
  return numberFormatter.format(Math.round(Number(value) || 0))
}

export function formatCurrency(value) {
  return currencyFormatter.format(Math.round(Number(value) || 0))
}

export function parseCurrencyInput(value) {
  const digits = String(value ?? '').replace(/\D/g, '')
  return digits === '' ? '' : Number(digits)
}
