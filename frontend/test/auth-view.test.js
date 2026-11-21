import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AuthView from '../src/components/AuthView.vue'
import { login, registerInitial, registerWithInvitation } from '../src/services/auth.js'

vi.mock('../src/services/auth.js', () => ({
  login: vi.fn(),
  registerInitial: vi.fn(),
  registerWithInvitation: vi.fn(),
}))

beforeEach(() => {
  vi.clearAllMocks()
  login.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
  registerInitial.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
  registerWithInvitation.mockResolvedValue({ userId: 2, email: 'member@example.com', role: 'MEMBER' })
})

describe('AuthView', () => {
  it('offers initial household registration before the first account exists', async () => {
    const wrapper = mount(AuthView, { props: { registrationAvailable: true, invitationToken: null } })

    expect(wrapper.text()).toContain('Tạo tài khoản chủ hộ')
    expect(wrapper.find('input[name="confirmPassword"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Đăng nhập')
  })

  it('registers the initial owner and emits the authenticated user', async () => {
    const wrapper = mount(AuthView, { props: { registrationAvailable: true, invitationToken: null } })
    await wrapper.get('input[name="email"]').setValue('owner@example.com')
    await wrapper.get('input[name="password"]').setValue('correct-horse-battery-staple')
    await wrapper.get('input[name="confirmPassword"]').setValue('correct-horse-battery-staple')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(registerInitial).toHaveBeenCalledWith({ email: 'owner@example.com', password: 'correct-horse-battery-staple' })
    expect(wrapper.emitted('authenticated')[0][0].role).toBe('OWNER')
  })

  it('uses the invitation token to register a member when bootstrap is closed', async () => {
    const wrapper = mount(AuthView, { props: { registrationAvailable: false, invitationToken: 'invite-secret' } })
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

  it('shows only login when bootstrap is closed and no invitation was supplied', async () => {
    const wrapper = mount(AuthView, { props: { registrationAvailable: false, invitationToken: null } })

    expect(wrapper.text()).toContain('Đăng nhập')
    expect(wrapper.find('input[name="confirmPassword"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Đăng ký chỉ khả dụng qua lời mời')
  })

  it('logs in and emits the authenticated user', async () => {
    const wrapper = mount(AuthView, { props: { registrationAvailable: false, invitationToken: null } })
    await wrapper.get('input[name="email"]').setValue('owner@example.com')
    await wrapper.get('input[name="password"]').setValue('correct-horse-battery-staple')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(login).toHaveBeenCalledWith({ email: 'owner@example.com', password: 'correct-horse-battery-staple' })
    expect(wrapper.emitted('authenticated')[0][0].role).toBe('OWNER')
  })

  it('validates password confirmation and displays server errors', async () => {
    const wrapper = mount(AuthView, { props: { registrationAvailable: true, invitationToken: null } })
    await wrapper.get('input[name="email"]').setValue('owner@example.com')
    await wrapper.get('input[name="password"]').setValue('correct-horse-battery-staple')
    await wrapper.get('input[name="confirmPassword"]').setValue('different-password')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Mật khẩu xác nhận không khớp')
    expect(registerInitial).not.toHaveBeenCalled()

    registerInitial.mockRejectedValueOnce(new Error('Email đã tồn tại'))
    await wrapper.get('input[name="confirmPassword"]').setValue('correct-horse-battery-staple')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Email đã tồn tại')
  })
})
