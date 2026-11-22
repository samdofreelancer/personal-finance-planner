<template>
  <main class="household-settings-page">
    <header class="household-settings-header">
      <div>
        <p class="auth-eyebrow">HỘ GIA ĐÌNH</p>
        <h1>Hộ gia đình</h1>
        <p class="auth-description">{{ user.email }} · {{ user.role === 'OWNER' ? 'Chủ hộ' : 'Thành viên' }}</p>
      </div>
      <button class="button button-secondary" type="button" @click="$emit('close')">← Quay lại kế hoạch</button>
    </header>

    <section v-if="user.role === 'OWNER'" class="household-panel" aria-labelledby="invitation-heading">
      <div class="household-panel-heading">
        <div>
          <h2 id="invitation-heading">Quản lý lời mời</h2>
          <p>Tạo liên kết dùng một lần để thành viên tham gia cùng household.</p>
        </div>
        <button id="create-invitation" class="button button-primary" type="button" :disabled="isCreating" @click="create">
          {{ isCreating ? 'Đang tạo...' : '+ Tạo lời mời' }}
        </button>
      </div>

      <div v-if="createdLink" class="created-invitation" role="status">
        <strong>Liên kết lời mời đã được tạo</strong>
        <p>Chỉ hiển thị token này một lần. Hãy sao chép và gửi riêng cho thành viên.</p>
        <div class="invitation-link-row">
          <input id="invitation-link" :value="createdLink" readonly aria-label="Liên kết lời mời">
          <button id="copy-invitation" class="button button-secondary" type="button" @click="copyLink">Sao chép</button>
        </div>
        <small v-if="copyMessage">{{ copyMessage }}</small>
      </div>

      <p v-if="errorMessage" class="household-error" role="alert">{{ errorMessage }}</p>
      <p v-if="successMessage" class="household-success" role="status">{{ successMessage }}</p>

      <div class="invitation-list">
        <h3>Lời mời đã tạo</h3>
        <p v-if="invitations.length === 0" class="household-muted">Chưa có lời mời nào.</p>
        <div v-for="invitation in invitations" :key="invitation.id" class="invitation-row">
          <div>
            <strong>Mã lời mời #{{ invitation.id }}</strong>
            <span>{{ invitationStatus(invitation) }}</span>
            <small>Hết hạn: {{ formatDate(invitation.expiresAt) }}</small>
          </div>
          <button
            v-if="!invitation.revokedAt && !invitation.consumedAt && new Date(invitation.expiresAt) > new Date()"
            class="text-button danger-text"
            type="button"
            :data-invitation-id="invitation.id"
            @click="revoke(invitation.id)"
          >Thu hồi</button>
        </div>
      </div>
    </section>

    <section v-else class="household-panel member-notice">
      <h2>Household của bạn</h2>
      <p>Bạn là thành viên của household này. Chủ hộ quản lý việc mời thành viên mới.</p>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { createInvitation, listInvitations, revokeInvitation } from '../services/auth.js'

const props = defineProps({ user: { type: Object, required: true } })
defineEmits(['close'])

const invitations = ref([])
const createdLink = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const copyMessage = ref('')
const isCreating = ref(false)

async function refreshInvitations() {
  invitations.value = await listInvitations()
}

async function create() {
  errorMessage.value = ''
  successMessage.value = ''
  isCreating.value = true
  try {
    const invitation = await createInvitation(7)
    createdLink.value = `${window.location.origin}${window.location.pathname}?invite=${encodeURIComponent(invitation.token)}`
    await refreshInvitations()
  } catch (error) {
    errorMessage.value = error.message || 'Không tạo được lời mời.'
  } finally {
    isCreating.value = false
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(createdLink.value)
    copyMessage.value = 'Đã sao chép liên kết.'
  } catch {
    copyMessage.value = 'Không thể truy cập clipboard. Hãy sao chép liên kết thủ công.'
  }
}

async function revoke(id) {
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await revokeInvitation(id)
    await refreshInvitations()
    successMessage.value = 'Đã thu hồi lời mời.'
  } catch (error) {
    errorMessage.value = error.message || 'Không thu hồi được lời mời.'
  }
}

function invitationStatus(invitation) {
  if (invitation.revokedAt) return 'Đã thu hồi'
  if (invitation.consumedAt) return 'Đã sử dụng'
  if (new Date(invitation.expiresAt) <= new Date()) return 'Đã hết hạn'
  return 'Chưa sử dụng'
}

function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

onMounted(async () => {
  if (props.user.role === 'OWNER') {
    try {
      await refreshInvitations()
    } catch (error) {
      errorMessage.value = error.message || 'Không tải được lời mời.'
    }
  }
})
</script>
