const BASE = '/api'

async function throwApiError(res, fallback) {
  let message = fallback
  try {
    const body = await res.json()
    if (body && typeof body.message === 'string' && body.message.trim()) {
      message = body.message
    }
  } catch {
    // The server did not return JSON; keep the fallback message.
  }
  throw new Error(message)
}

export async function getAssumptions() {
  const res = await fetch(`${BASE}/assumptions`)
  if (!res.ok) await throwApiError(res, 'Không tải được giả định')
  return res.json()
}

export async function saveAssumptions(dto) {
  const res = await fetch(`${BASE}/assumptions`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto)
  })
  if (!res.ok) await throwApiError(res, 'Không lưu được giả định')
  return res.json()
}

export async function seedDefaults() {
  const res = await fetch(`${BASE}/assumptions/seed`, { method: 'POST' })
  if (!res.ok) await throwApiError(res, 'Không khôi phục được dữ liệu mặc định')
  return res.json()
}

export async function getLoans() {
  const res = await fetch(`${BASE}/loans`)
  if (!res.ok) await throwApiError(res, 'Không tải được danh sách khoản vay')
  return res.json()
}

export async function saveLoans(loans) {
  const res = await fetch(`${BASE}/loans`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(loans),
  })
  if (!res.ok) await throwApiError(res, 'Không lưu được khoản vay và lịch trả')
  return res.json()
}

export async function getForecast(dto) {
  const res = await fetch(`${BASE}/forecast`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto),
  })
  if (!res.ok) await throwApiError(res, 'Không tính toán được dự báo')
  return res.json()
}

export async function exportCsv(dto) {
  const res = await fetch(`${BASE}/export/csv`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto),
  })
  if (!res.ok) await throwApiError(res, 'Không xuất được CSV')
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
