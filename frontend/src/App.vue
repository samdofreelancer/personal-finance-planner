<template>
  <div v-if="isCheckingSession" class="auth-loading" role="status">Đang kiểm tra phiên đăng nhập...</div>
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
  <PlannerView
    v-else
    :user="user"
    @logout="handleLogout"
    @open-household-settings="showHouseholdSettings = true"
  />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AuthView from './components/AuthView.vue'
import HouseholdSettings from './components/HouseholdSettings.vue'
import PlannerView from './PlannerView.vue'
import { getCurrentUser, logout } from './services/auth.js'

const user = ref(null)
const isCheckingSession = ref(true)
const showHouseholdSettings = ref(false)
const invitationToken = new URLSearchParams(window.location.search).get('invite') || null

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
  if (invitationToken) {
    const url = new URL(window.location.href)
    url.searchParams.delete('invite')
    window.history.replaceState({}, '', url)
  }
}

async function handleLogout() {
  try {
    await logout()
  } finally {
    user.value = null
    showHouseholdSettings.value = false
  }
}

function handleUnauthorized() {
  user.value = null
  showHouseholdSettings.value = false
}

onMounted(() => {
  window.addEventListener('auth:unauthorized', handleUnauthorized)
  void restoreSession()
})

onBeforeUnmount(() => window.removeEventListener('auth:unauthorized', handleUnauthorized))
</script>
