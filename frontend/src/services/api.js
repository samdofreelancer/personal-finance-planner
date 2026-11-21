const BASE = '/api'
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export async function throwApiError(res, fallback) {
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

async function getCsrfToken() {
  const response = await fetch(`${BASE}/csrf`, { credentials: 'same-origin' })
  if (!response.ok) await throwApiError(response, 'Không lấy được mã bảo vệ CSRF')
  const body = await response.json()
  if (!body?.token) throw new Error('Máy chủ không cấp mã bảo vệ CSRF')
  return body.token
}

export async function apiRequest(path, options = {}, { notifyUnauthorized = true } = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const requestOptions = { ...options, credentials: 'same-origin' }

  if (MUTATING_METHODS.has(method)) {
    const token = await getCsrfToken()
    requestOptions.headers = { ...(options.headers || {}), 'X-XSRF-TOKEN': token }
  } else if (options.headers) {
    requestOptions.headers = { ...options.headers }
  }

  const response = await fetch(path, requestOptions)
  if (response.status === 401 && notifyUnauthorized && !path.startsWith(`${BASE}/auth/`)) {
    if (typeof window !== 'undefined') window.dispatchEvent(new Event('auth:unauthorized'))
  }
  return response
}

export async function requestJson(path, options = {}, fallback = 'Yêu cầu không thành công', settings = {}) {
  const response = await apiRequest(path, options, settings)
  if (!response.ok) await throwApiError(response, fallback)
  if (response.status === 204) return null
  return response.json()
}

export async function getAssumptions() {
  return requestJson(`${BASE}/assumptions`, {}, 'Không tải được giả định')
}

export async function saveAssumptions(dto) {
  return requestJson(`${BASE}/assumptions`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto),
  }, 'Không lưu được giả định')
}

export async function seedDefaults() {
  return requestJson(`${BASE}/assumptions/seed`, { method: 'POST' }, 'Không khôi phục được dữ liệu mặc định')
}

export async function getLoans() {
  return requestJson(`${BASE}/loans`, {}, 'Không tải được danh sách khoản vay')
}

export async function saveLoans(loans) {
  return requestJson(`${BASE}/loans`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(loans),
  }, 'Không lưu được khoản vay và lịch trả')
}

export async function getForecast(dto) {
  return requestJson(`${BASE}/forecast`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto),
  }, 'Không tính toán được dự báo')
}

export async function exportCsv(dto) {
  const response = await apiRequest(`${BASE}/export/csv`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto),
  })
  if (!response.ok) await throwApiError(response, 'Không xuất được CSV')
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'ke-hoach-tai-chinh-2026-2030.csv'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
}
