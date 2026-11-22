import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ProfileView from '../src/components/ProfileView.vue'
import { changePassword, getProfile, updateProfile } from '../src/services/auth.js'

vi.mock('../src/services/auth.js', () => ({
  changePassword: vi.fn(),
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
}))

const user = { userId: 1, email: 'person@example.com', role: 'OWNER' }

beforeEach(() => {
  vi.clearAllMocks()
  getProfile.mockResolvedValue({
    userId: 1, email: user.email, displayName: 'Person', avatarUrl: null, createdAt: '2026-01-01T00:00:00Z',
  })
  updateProfile.mockImplementation(async (profile) => ({ ...profile, email: user.email, createdAt: '2026-01-01T00:00:00Z' }))
  changePassword.mockResolvedValue(null)
})

describe('ProfileView', () => {
  it('shows account metadata as read-only and saves editable profile fields', async () => {
    const wrapper = mount(ProfileView, { props: { user } })
    await flushPromises()

    expect(getProfile).toHaveBeenCalledOnce()
    expect(wrapper.get('#profile-email').element.value).toBe(user.email)
    expect(wrapper.get('#profile-email').attributes('readonly')).toBeDefined()
    expect(wrapper.get('#profile-created-at').attributes('readonly')).toBeDefined()
    await wrapper.get('#profile-display-name').setValue('  Updated Person  ')
    await wrapper.get('#profile-avatar-url').setValue('https://cdn.example.com/avatar.png')
    await wrapper.get('.profile-form').trigger('submit')
    await flushPromises()

    expect(updateProfile).toHaveBeenCalledWith({ displayName: 'Updated Person', avatarUrl: 'https://cdn.example.com/avatar.png' })
    expect(wrapper.text()).toContain('Đã cập nhật hồ sơ cá nhân')
  })

  it('validates password confirmation before submitting a password change', async () => {
    const wrapper = mount(ProfileView, { props: { user } })
    await flushPromises()
    await wrapper.get('#current-password').setValue('correct-current-password')
    await wrapper.get('#new-password').setValue('new-secure-password')
    await wrapper.get('#confirm-new-password').setValue('different-secure-password')
    await wrapper.get('.profile-password-form').trigger('submit')
    await flushPromises()

    expect(changePassword).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Mật khẩu xác nhận không khớp')
  })

  it('changes password after matching confirmation and clears password fields', async () => {
    const wrapper = mount(ProfileView, { props: { user } })
    await flushPromises()
    await wrapper.get('#current-password').setValue('correct-current-password')
    await wrapper.get('#new-password').setValue('new-secure-password')
    await wrapper.get('#confirm-new-password').setValue('new-secure-password')
    await wrapper.get('.profile-password-form').trigger('submit')
    await flushPromises()

    expect(changePassword).toHaveBeenCalledWith({ currentPassword: 'correct-current-password', newPassword: 'new-secure-password' })
    expect(wrapper.get('#current-password').element.value).toBe('')
    expect(wrapper.text()).toContain('Đã đổi mật khẩu')
  })
})
