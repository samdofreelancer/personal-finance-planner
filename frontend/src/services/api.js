const BASE = '/api'

export async function getAssumptions() {
  const res = await fetch(`${BASE}/assumptions`)
  if (!res.ok) throw new Error('Không tải được giả định')
  return res.json()
}

export async function saveAssumptions(dto) {
  const res = await fetch(`${BASE}/assumptions`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto)
  })
  if (!res.ok) throw new Error('Không lưu được giả định')
  return res.json()
}

export async function seedDefaults() {
  const res = await fetch(`${BASE}/assumptions/seed`, { method: 'POST' })
  if (!res.ok) throw new Error('Không seeding được dữ liệu mặc định')
  return res.json()
}

export async function getForecast(dto) {
  const params = new URLSearchParams()
  if (dto.baseSalary != null) params.set('baseSalary', dto.baseSalary)
  if (dto.januaryMultiplier != null) params.set('januaryMultiplier', dto.januaryMultiplier.toString())
  if (dto.living != null) params.set('living', dto.living)
  if (dto.childThrough2026 != null) params.set('childThrough2026', dto.childThrough2026)
  if (dto.childQ12027 != null) params.set('childQ12027', dto.childQ12027)
  if (dto.childFromApr2027 != null) params.set('childFromApr2027', dto.childFromApr2027)
  if (dto.rentThroughJan2027 != null) params.set('rentThroughJan2027', dto.rentThroughJan2027)
  if (dto.rentFromFeb2027 != null) params.set('rentFromFeb2027', dto.rentFromFeb2027)
  if (dto.loanPrincipal != null) params.set('loanPrincipal', dto.loanPrincipal)
  if (dto.fixedPayment != null) params.set('fixedPayment', dto.fixedPayment)
  if (dto.novInterest != null) params.set('novInterest', dto.novInterest)
  if (dto.decInterest != null) params.set('decInterest', dto.decInterest)
  if (dto.payoffInterest != null) params.set('payoffInterest', dto.payoffInterest)
  if (dto.payoffInterestConfirmed != null) params.set('payoffInterestConfirmed', dto.payoffInterestConfirmed)
  if (dto.startingSavings != null) params.set('startingSavings', dto.startingSavings)
  if (dto.raises) {
    for (const [y, v] of Object.entries(dto.raises)) {
      params.set(`raises[${y}]`, v.toString())
    }
  }
  const res = await fetch(`${BASE}/forecast?${params.toString()}`)
  if (!res.ok) throw new Error('Không tính toán được dự báo')
  return res.json()
}

export async function exportCsv(dto) {
  const params = new URLSearchParams()
  if (dto.baseSalary != null) params.set('baseSalary', dto.baseSalary)
  if (dto.januaryMultiplier != null) params.set('januaryMultiplier', dto.januaryMultiplier.toString())
  if (dto.living != null) params.set('living', dto.living)
  if (dto.childThrough2026 != null) params.set('childThrough2026', dto.childThrough2026)
  if (dto.childQ12027 != null) params.set('childQ12027', dto.childQ12027)
  if (dto.childFromApr2027 != null) params.set('childFromApr2027', dto.childFromApr2027)
  if (dto.rentThroughJan2027 != null) params.set('rentThroughJan2027', dto.rentThroughJan2027)
  if (dto.rentFromFeb2027 != null) params.set('rentFromFeb2027', dto.rentFromFeb2027)
  if (dto.loanPrincipal != null) params.set('loanPrincipal', dto.loanPrincipal)
  if (dto.fixedPayment != null) params.set('fixedPayment', dto.fixedPayment)
  if (dto.novInterest != null) params.set('novInterest', dto.novInterest)
  if (dto.decInterest != null) params.set('decInterest', dto.decInterest)
  if (dto.payoffInterest != null) params.set('payoffInterest', dto.payoffInterest)
  if (dto.payoffInterestConfirmed != null) params.set('payoffInterestConfirmed', dto.payoffInterestConfirmed)
  if (dto.startingSavings != null) params.set('startingSavings', dto.startingSavings)
  if (dto.raises) {
    for (const [y, v] of Object.entries(dto.raises)) {
      params.set(`raises[${y}]`, v.toString())
    }
  }
  const res = await fetch(`${BASE}/export/csv?${params.toString()}`)
  if (!res.ok) throw new Error('Không xuất được CSV')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'ke-hoach-tai-chinh-2026-2030.csv'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}