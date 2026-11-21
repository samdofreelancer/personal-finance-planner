import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import HouseholdSettings from '../src/components/HouseholdSettings.vue'
import { createInvitation, listInvitations, revokeInvitation } from '../src/services/auth.js'

vi.mock('../src/services/auth.js', () => ({
  createInvitation: vi.fn(),
  listInvitations: vi.fn(),
  revokeInvitation: vi.fn(),
}))

const owner = { userId: 1, email: 'owner@example.com', householdId: 1, role: 'OWNER' }
const member = { userId: 2, email: 'member@example.com', householdId: 1, role: 'MEMBER' }

beforeEach(() => {
  vi.clearAllMocks()
  listInvitations.mockResolvedValue([])
  createInvitation.mockResolvedValue({ id: 10, token: 'one-time-secret', expiresAt: '2026-11-28T00:00:00Z' })
  revokeInvitation.mockResolvedValue(null)
})

afterEach(() => vi.unstubAllGlobals())

describe('HouseholdSettings', () => {
  it('loads owner invitation history and creates a one-time share link', async () => {
    listInvitations.mockResolvedValueOnce([
      { id: 5, createdAt: '2026-11-20T00:00:00Z', expiresAt: '2026-11-27T00:00:00Z', consumedAt: null, revokedAt: null },
    ])
    const wrapper = mount(HouseholdSettings, { props: { user: owner } })
    await flushPromises()

    expect(listInvitations).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Quản lý lời mời')
    expect(wrapper.text()).toContain('Chưa sử dụng')

    await wrapper.get('#create-invitation').trigger('click')
    await flushPromises()

    expect(createInvitation).toHaveBeenCalledWith(7)
    expect(wrapper.get('#invitation-link').element.value).toContain('?invite=one-time-secret')
    expect(wrapper.text()).toContain('Chỉ hiển thị token này một lần')
  })

  it('copies the invitation URL and lets the owner revoke an invitation', async () => {
    listInvitations.mockResolvedValue([{
      id: 10, createdAt: '2026-11-21T00:00:00Z', expiresAt: '2026-11-28T00:00:00Z', consumedAt: null, revokedAt: null,
    }])
    const wrapper = mount(HouseholdSettings, { props: { user: owner } })
    await flushPromises()
    await wrapper.get('#create-invitation').trigger('click')
    await flushPromises()

    const writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal('navigator', { ...navigator, clipboard: { writeText } })
    await wrapper.get('#copy-invitation').trigger('click')
    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('?invite=one-time-secret'))

    await wrapper.get('[data-invitation-id="10"]').trigger('click')
    await flushPromises()
    expect(revokeInvitation).toHaveBeenCalledWith(10)
    expect(wrapper.text()).toContain('Đã thu hồi lời mời')
  })

  it('does not expose invitation management to household members', async () => {
    const wrapper = mount(HouseholdSettings, { props: { user: member } })
    await flushPromises()

    expect(wrapper.text()).toContain('Bạn là thành viên của household')
    expect(wrapper.find('#create-invitation').exists()).toBe(false)
    expect(listInvitations).not.toHaveBeenCalled()
  })
})
