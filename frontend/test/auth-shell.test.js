import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import App from '../src/App.vue'
import { getCurrentUser, logout } from '../src/services/auth.js'

vi.mock('../src/services/auth.js', () => ({
  getCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

const AuthViewStub = defineComponent({
  name: 'AuthView',
  props: ['invitationToken'],
  emits: ['authenticated'],
  template: '<div data-testid="auth-view"><button id="authenticate" @click="$emit(\'authenticated\', { userId: 1, email: \'owner@example.com\', role: \'OWNER\' })">Authenticate</button></div>',
})

const PlannerViewStub = defineComponent({
  name: 'PlannerView',
  props: ['user'],
  emits: ['logout', 'open-household-settings', 'open-profile'],
  template: '<div data-testid="planner-view"><button id="logout" @click="$emit(\'logout\')">Logout</button><button id="settings" @click="$emit(\'open-household-settings\')">Settings</button><button id="profile" @click="$emit(\'open-profile\')">Profile</button></div>',
})

const HouseholdSettingsStub = defineComponent({
  name: 'HouseholdSettings',
  props: ['user'],
  emits: ['close'],
  template: '<div data-testid="household-settings">Household settings</div>',
})

const ProfileViewStub = defineComponent({
  name: 'ProfileView',
  props: ['user'],
  emits: ['close'],
  template: '<div data-testid="profile-view">Profile</div>',
})

const wrappers = []

function mountApp() {
  const wrapper = mount(App, {
    global: { stubs: { AuthView: AuthViewStub, PlannerView: PlannerViewStub, HouseholdSettings: HouseholdSettingsStub, ProfileView: ProfileViewStub } },
  })
  wrappers.push(wrapper)
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  getCurrentUser.mockResolvedValue(null)
  logout.mockResolvedValue(null)
})

afterEach(() => {
  wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  vi.restoreAllMocks()
})

describe('App authentication shell', () => {
  it('shows the login/register screen when no server session exists', async () => {
    const wrapper = mountApp()
    await flushPromises()

    expect(getCurrentUser).toHaveBeenCalledOnce()
    expect(wrapper.find('[data-testid="auth-view"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="planner-view"]').exists()).toBe(false)
  })

  it('restores the authenticated planner shell from the server session', async () => {
    getCurrentUser.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
    const wrapper = mountApp()
    await flushPromises()

    expect(wrapper.find('[data-testid="planner-view"]').exists()).toBe(true)
  })

  it('switches to the planner after authentication succeeds', async () => {
    const wrapper = mountApp()
    await flushPromises()
    await wrapper.get('#authenticate').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="planner-view"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="auth-view"]').exists()).toBe(false)
  })

  it('logs out, clears the user and returns to the auth screen', async () => {
    getCurrentUser.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
    const wrapper = mountApp()
    await flushPromises()
    await wrapper.get('#logout').trigger('click')
    await flushPromises()

    expect(logout).toHaveBeenCalledOnce()
    expect(wrapper.find('[data-testid="auth-view"]').exists()).toBe(true)
  })

  it('opens household settings and returns to the planner when closed', async () => {
    getCurrentUser.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
    const wrapper = mountApp()
    await flushPromises()
    await wrapper.get('#settings').trigger('click')
    expect(wrapper.find('[data-testid="household-settings"]').exists()).toBe(true)
  })

  it('opens the personal profile independently from household settings', async () => {
    getCurrentUser.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
    const wrapper = mountApp()
    await flushPromises()
    await wrapper.get('#profile').trigger('click')
    expect(wrapper.find('[data-testid="profile-view"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="household-settings"]').exists()).toBe(false)
  })

  it('returns to login when an authenticated API request reports an expired session', async () => {
    getCurrentUser.mockResolvedValue({ userId: 1, email: 'owner@example.com', role: 'OWNER' })
    const wrapper = mountApp()
    await flushPromises()
    window.dispatchEvent(new Event('auth:unauthorized'))
    await flushPromises()

    expect(wrapper.find('[data-testid="auth-view"]').exists()).toBe(true)
  })
})
