import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AuthView from '../src/components/AuthView.vue'
import {
  confirmPasswordReset, login, registerAccount, registerWithInvitation, requestPasswordReset,
} from '../src/services/auth.js'

vi.mock('../src/services/auth.js', () => ({
  login: vi.fn(),
  registerAccount: vi.fn(),
  registerWithInvitation: vi.fn(),
  requestPasswordReset: vi.fn(),
  confirmPasswordReset: vi.fn(),
}))

beforeEach(() => {
  vi.clearAllMocks()
  login.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
  registerAccount.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
  registerWithInvitation.mockResolvedValue({ userId: 2, email: 'member@example.com', role: 'MEMBER' })
  requestPasswordReset.mockResolvedValue(null)
  confirmPasswordReset.mockResolvedValue(null)
})

describe('AuthView', () => {
  it('always offers self-registration from the login screen', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: null } })

    expect(wrapper.text()).toContain('Chưa có tài khoản? Tạo tài khoản')
    expect(wrapper.find('input[name="confirmPassword"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Đăng nhập')
    expect(wrapper.get('input[name="password"]').attributes('placeholder')).toBe('••••••••••••')
    expect(wrapper.text()).toContain('kế hoạch tài chính gia đình')
    expect(wrapper.get('button[type="submit"]').element.disabled).toBe(true)
    await wrapper.get('input[name="email"]').setValue('owner@example.com')
    await wrapper.get('input[name="password"]').setValue('password')
    expect(wrapper.get('button[type="submit"]').element.disabled).toBe(false)
    await wrapper.get('.auth-mode-toggle').trigger('click')
    expect(wrapper.text()).toContain('Tạo tài khoản')
    expect(wrapper.find('input[name="confirmPassword"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Mật khẩu cần có ít nhất 12 ký tự.')
    expect(wrapper.get('input[name="password"]').attributes('placeholder')).toBe('Tạo mật khẩu của bạn')
  })

  it('allows the user to reveal and hide the password accessibly', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: null } })
    const password = wrapper.get('input[name="password"]')

    expect(password.attributes('type')).toBe('password')
    await wrapper.get('button[aria-label="Hiện mật khẩu"]').trigger('click')
    expect(wrapper.get('input[name="password"]').attributes('type')).toBe('text')
    expect(wrapper.get('button[aria-label="Ẩn mật khẩu"]').attributes('aria-pressed')).toBe('true')
    await wrapper.get('button[aria-label="Ẩn mật khẩu"]').trigger('click')
    expect(wrapper.get('input[name="password"]').attributes('type')).toBe('password')
  })

  it('offers a generic password recovery confirmation without revealing account existence', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: null } })
    await wrapper.get('.auth-forgot-link').trigger('click')
    expect(wrapper.text()).toContain('Khôi phục mật khẩu')
    expect(wrapper.find('input[name="password"]').exists()).toBe(false)
    await wrapper.get('input[name="email"]').setValue('owner@example.com')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(requestPasswordReset).toHaveBeenCalledWith({ email: 'owner@example.com' })
    expect(wrapper.text()).toContain('Nếu email thuộc về một tài khoản')
  })

  it('submits a new password with the reset token and returns to login', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: null, resetToken: 'one-time-token' } })
    await wrapper.get('input[name="password"]').setValue('a-new-secure-password')
    await wrapper.get('input[name="confirmPassword"]').setValue('a-new-secure-password')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(confirmPasswordReset).toHaveBeenCalledWith({ token: 'one-time-token', newPassword: 'a-new-secure-password' })
    expect(wrapper.emitted('password-reset')).toHaveLength(1)
    expect(wrapper.text()).toContain('Mật khẩu đã được đặt lại')
    expect(wrapper.find('input[name="confirmPassword"]').exists()).toBe(false)
  })

  it('registers a new household owner and emits the authenticated user', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: null } })
    await wrapper.get('.auth-mode-toggle').trigger('click')
    await wrapper.get('input[name="email"]').setValue('owner@example.com')
    await wrapper.get('input[name="password"]').setValue('correct-horse-battery-staple')
    await wrapper.get('input[name="confirmPassword"]').setValue('correct-horse-battery-staple')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(registerAccount).toHaveBeenCalledWith({ email: 'owner@example.com', password: 'correct-horse-battery-staple' })
    expect(wrapper.emitted('authenticated')[0][0].role).toBe('OWNER')
  })

  it('uses the invitation token to register a member in the invited household', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: 'invite-secret' } })
    await wrapper.get('input[name="email"]').setValue('member@example.com')
    await wrapper.get('input[name="password"]').setValue('correct-horse-battery-staple')
    await wrapper.get('input[name="confirmPassword"]').setValue('correct-horse-battery-staple')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(registerWithInvitation).toHaveBeenCalledWith({
      email: 'member@example.com', password: 'correct-horse-battery-staple', invitationToken: 'invite-secret',
    })
    expect(wrapper.emitted('authenticated')[0][0].role).toBe('MEMBER')
  })

  it('keeps registration available when there is no invitation and accounts already exist', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: null } })

    expect(wrapper.text()).toContain('Đăng nhập')
    expect(wrapper.find('input[name="confirmPassword"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Chưa có tài khoản? Tạo tài khoản')
    await wrapper.get('.auth-mode-toggle').trigger('click')
    expect(wrapper.find('input[name="confirmPassword"]').exists()).toBe(true)
  })

  it('logs in and emits the authenticated user', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: null } })
    await wrapper.get('input[name="email"]').setValue('owner@example.com')
    await wrapper.get('input[name="password"]').setValue('correct-horse-battery-staple')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(login).toHaveBeenCalledWith({ email: 'owner@example.com', password: 'correct-horse-battery-staple' })
    expect(wrapper.emitted('authenticated')[0][0].role).toBe('OWNER')
  })

  it('validates password confirmation and displays server errors', async () => {
    const wrapper = mount(AuthView, { props: { invitationToken: null } })
    await wrapper.get('.auth-mode-toggle').trigger('click')
    await wrapper.get('input[name="email"]').setValue('owner@example.com')
    await wrapper.get('input[name="password"]').setValue('correct-horse-battery-staple')
    await wrapper.get('input[name="confirmPassword"]').setValue('different-password')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Mật khẩu xác nhận không khớp')
    expect(registerAccount).not.toHaveBeenCalled()

    registerAccount.mockRejectedValueOnce(new Error('Email đã tồn tại'))
    await wrapper.get('input[name="confirmPassword"]').setValue('correct-horse-battery-staple')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Email đã tồn tại')
  })
})
