import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createInvitation,
  changePassword,
  confirmPasswordReset,
  getProfile,
  getCurrentUser,
  listInvitations,
  login,
  logout,
  registerAccount,
  registerWithInvitation,
  requestPasswordReset,
  revokeInvitation,
  updateProfile,
} from '../src/services/auth.js'
import { apiRequest, requestJson } from '../src/services/api.js'

vi.mock('../src/services/api.js', () => ({
  apiRequest: vi.fn(),
  requestJson: vi.fn(),
}))

beforeEach(() => vi.clearAllMocks())
afterEach(() => vi.unstubAllGlobals())

describe('authentication service', () => {
  it('returns null for an unauthenticated session without raising a global auth event', async () => {
    apiRequest.mockResolvedValue({ status: 401, ok: false })

    await expect(getCurrentUser()).resolves.toBeNull()
    expect(apiRequest).toHaveBeenCalledWith('/api/auth/me', {}, { notifyUnauthorized: false })
  })

  it('returns the current authenticated user', async () => {
    const user = { userId: 1, email: 'owner@example.com', householdId: 1, role: 'OWNER' }
    apiRequest.mockResolvedValue({ status: 200, ok: true, json: async () => user })

    await expect(getCurrentUser()).resolves.toEqual(user)
  })

  it('registers a new independent household owner', async () => {
    const user = { userId: 1, role: 'OWNER' }
    requestJson.mockResolvedValue(user)

    await expect(registerAccount({ email: 'owner@example.com', password: 'correct-horse-battery-staple' }))
      .resolves.toEqual(user)
    expect(requestJson).toHaveBeenCalledWith('/api/auth/register', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'owner@example.com', password: 'correct-horse-battery-staple' }),
    }), 'Không đăng ký được tài khoản')
  })

  it('registers a household member using the one-time invitation token', async () => {
    requestJson.mockResolvedValue({ userId: 2, role: 'MEMBER' })

    await registerWithInvitation({
      email: 'member@example.com', password: 'correct-horse-battery-staple', invitationToken: 'raw-token',
    })
    expect(requestJson).toHaveBeenCalledWith('/api/auth/register', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        email: 'member@example.com', password: 'correct-horse-battery-staple', invitationToken: 'raw-token',
      }),
    }), 'Không tham gia được household')
  })

  it('logs in and logs out through the server session', async () => {
    requestJson.mockResolvedValueOnce({ userId: 1, role: 'OWNER' }).mockResolvedValueOnce(null)

    await login({ email: 'owner@example.com', password: 'correct-horse-battery-staple' })
    await logout()

    expect(requestJson).toHaveBeenNthCalledWith(1, '/api/auth/login', expect.objectContaining({ method: 'POST' }), 'Đăng nhập thất bại')
    expect(requestJson).toHaveBeenNthCalledWith(2, '/api/auth/logout', { method: 'POST' }, 'Đăng xuất thất bại')
  })

  it('requests and confirms a password reset through the public auth API', async () => {
    apiRequest.mockResolvedValueOnce({ ok: true, status: 202 })
    requestJson.mockResolvedValueOnce(null)

    await requestPasswordReset({ email: 'owner@example.com' })
    await confirmPasswordReset({ token: 'test-reset-value', newPassword: 'new-secure-password' })

    expect(apiRequest).toHaveBeenCalledWith('/api/auth/password-reset/request', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: 'owner@example.com' }),
    }))
    expect(requestJson).toHaveBeenCalledWith('/api/auth/password-reset/confirm', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ token: 'test-reset-value', newPassword: 'new-secure-password' }),
    }), 'Không đặt lại được mật khẩu')
  })

  it('lists, creates and revokes invitations without exposing token hashes', async () => {
    const invitations = [{ id: 5, expiresAt: '2026-11-28T00:00:00Z', consumedAt: null, revokedAt: null }]
    requestJson.mockResolvedValueOnce(invitations).mockResolvedValueOnce({ id: 6, token: 'one-time-token' })
      .mockResolvedValueOnce(null)

    await expect(listInvitations()).resolves.toEqual(invitations)
    await expect(createInvitation(7)).resolves.toEqual({ id: 6, token: 'one-time-token' })
    await revokeInvitation(6)

    expect(requestJson).toHaveBeenNthCalledWith(1, '/api/household/invitations', {}, 'Không tải được lời mời')
    expect(requestJson).toHaveBeenNthCalledWith(2, '/api/household/invitations', expect.objectContaining({
      method: 'POST', body: JSON.stringify({ validityDays: 7 }),
    }), 'Không tạo được lời mời')
    expect(requestJson).toHaveBeenNthCalledWith(3, '/api/household/invitations/6', { method: 'DELETE' }, 'Không thu hồi được lời mời')
  })

  it('reads and updates the personal profile and changes its password', async () => {
    requestJson.mockResolvedValueOnce({ email: 'user@example.com' }).mockResolvedValueOnce({ displayName: 'User' }).mockResolvedValueOnce(null)
    await getProfile()
    await updateProfile({ displayName: 'User', avatarUrl: null })
    await changePassword({ currentPassword: 'current-password', newPassword: 'new-password' })

    expect(requestJson).toHaveBeenNthCalledWith(1, '/api/profile', {}, 'Không tải được hồ sơ cá nhân')
    expect(requestJson).toHaveBeenNthCalledWith(2, '/api/profile', expect.objectContaining({
      method: 'PUT', body: JSON.stringify({ displayName: 'User', avatarUrl: null }),
    }), 'Không cập nhật được hồ sơ cá nhân')
    expect(requestJson).toHaveBeenNthCalledWith(3, '/api/profile/password', expect.objectContaining({
      method: 'POST', body: JSON.stringify({ currentPassword: 'current-password', newPassword: 'new-password' }),
    }), 'Không đổi được mật khẩu')
  })
})
