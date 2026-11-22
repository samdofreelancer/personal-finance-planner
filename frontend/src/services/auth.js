import { apiRequest, requestJson, throwApiError } from './api.js'

const JSON_HEADERS = { 'Content-Type': 'application/json' }

export async function getCurrentUser() {
  const response = await apiRequest('/api/auth/me', {}, { notifyUnauthorized: false })
  if (response.status === 401) return null
  if (!response.ok) await throwApiError(response, 'Không tải được phiên đăng nhập')
  return response.json()
}

export async function registerAccount({ email, password }) {
  return requestJson('/api/auth/register', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ email, password }),
  }, 'Không đăng ký được tài khoản')
}

export async function registerWithInvitation({ email, password, invitationToken }) {
  return requestJson('/api/auth/register', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ email, password, invitationToken }),
  }, 'Không tham gia được household')
}

export async function login({ email, password }) {
  return requestJson('/api/auth/login', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ email, password }),
  }, 'Đăng nhập thất bại')
}

export async function logout() {
  return requestJson('/api/auth/logout', { method: 'POST' }, 'Đăng xuất thất bại')
}

export async function listInvitations() {
  return requestJson('/api/household/invitations', {}, 'Không tải được lời mời')
}

export async function createInvitation(validityDays = 7) {
  return requestJson('/api/household/invitations', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ validityDays }),
  }, 'Không tạo được lời mời')
}

export async function revokeInvitation(invitationId) {
  return requestJson(`/api/household/invitations/${invitationId}`, { method: 'DELETE' }, 'Không thu hồi được lời mời')
}
