<template>
  <div v-if="isCheckingSession" class="auth-loading" role="status">Đang kiểm tra phiên đăng nhập...</div>
  <AuthView
    v-else-if="resetToken"
    :reset-token="resetToken"
    @password-reset="handlePasswordReset"
  />
  <AuthView
    v-else-if="!user"
    :invitation-token="invitationToken"
    @authenticated="handleAuthenticated"
  />
  <HouseholdSettings
    v-else-if="showHouseholdSettings"
    :user="user"
    @close="showHouseholdSettings = false"
  />
  <ProfileView
    v-else-if="showProfile"
    :user="user"
    @close="showProfile = false"
  />
  <RouterView v-else v-slot="{ Component, route }">
    <component
      :is="Component"
      :user="user"
      :section="route.meta.section || 'overview'"
      @logout="handleLogout"
      @open-household-settings="showHouseholdSettings = true"
      @open-profile="showProfile = true"
    />
  </RouterView>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AuthView from './components/AuthView.vue'
import HouseholdSettings from './components/HouseholdSettings.vue'
import ProfileView from './components/ProfileView.vue'
import { getCurrentUser, logout } from './services/auth.js'

const user = ref(null)
const isCheckingSession = ref(true)
const showHouseholdSettings = ref(false)
const showProfile = ref(false)
const invitationToken = new URLSearchParams(window.location.search).get('invite') || null
const resetToken = new URLSearchParams(window.location.search).get('resetToken') || null

async function restoreSession() {
  isCheckingSession.value = true
  try {
    user.value = await getCurrentUser()
  } catch {
    user.value = null
  } finally {
    isCheckingSession.value = false
  }
}

function handleAuthenticated(authenticatedUser) {
  user.value = authenticatedUser
  showHouseholdSettings.value = false
  showProfile.value = false
  if (invitationToken) {
    const url = new URL(window.location.href)
    url.searchParams.delete('invite')
    window.history.replaceState({}, '', url)
  }
}

function handlePasswordReset() {
  user.value = null
  showHouseholdSettings.value = false
  showProfile.value = false
  const url = new URL(window.location.href)
  url.searchParams.delete('resetToken')
  window.history.replaceState({}, '', url)
}

async function handleLogout() {
  try {
    await logout()
  } finally {
    user.value = null
    showHouseholdSettings.value = false
    showProfile.value = false
  }
}

function handleUnauthorized() {
  user.value = null
  showHouseholdSettings.value = false
  showProfile.value = false
}

onMounted(() => {
  window.addEventListener('auth:unauthorized', handleUnauthorized)
  void restoreSession()
})

onBeforeUnmount(() => window.removeEventListener('auth:unauthorized', handleUnauthorized))
</script>
