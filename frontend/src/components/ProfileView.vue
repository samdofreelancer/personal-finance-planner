<template>
  <main class="profile-page">
    <header class="profile-header">
      <div>
        <p class="auth-eyebrow">TÀI KHOẢN</p>
        <h1>Hồ sơ cá nhân</h1>
        <p class="auth-description">Thông tin nhận diện và bảo mật cho tài khoản Planwise của bạn.</p>
      </div>
      <button class="button button-secondary" type="button" @click="$emit('close')">← Quay lại kế hoạch</button>
    </header>

    <p v-if="isLoading" class="profile-notice" role="status">Đang tải hồ sơ...</p>
    <p v-if="errorMessage" class="profile-error" role="alert">{{ errorMessage }}</p>
    <p v-if="successMessage" class="profile-success" role="status">{{ successMessage }}</p>

    <section v-if="!isLoading" class="profile-panel" aria-labelledby="profile-info-heading">
      <div class="profile-panel-heading">
        <div class="profile-avatar" aria-label="Ảnh đại diện">
          <img v-if="avatarPreview" :src="avatarPreview" alt="Ảnh đại diện" @error="avatarLoadFailed = true">
          <span v-else>{{ initials }}</span>
        </div>
        <div><h2 id="profile-info-heading">Thông tin cá nhân</h2><p>Cập nhật cách tên và ảnh của bạn hiển thị trong ứng dụng.</p></div>
      </div>

      <form class="profile-form" @submit.prevent="saveProfile">
        <label class="profile-field" for="profile-display-name">
          <span>Tên hiển thị</span>
          <input id="profile-display-name" v-model.trim="displayName" maxlength="80" required autocomplete="name" placeholder="Tên bạn muốn sử dụng">
        </label>
        <label class="profile-field" for="profile-avatar-url">
          <span>Đường dẫn ảnh đại diện <small>(không bắt buộc)</small></span>
          <input id="profile-avatar-url" v-model.trim="avatarUrl" type="url" maxlength="2048" placeholder="https://example.com/avatar.jpg">
          <small>Chỉ hỗ trợ liên kết HTTPS công khai. Để trống nếu muốn dùng chữ cái đại diện.</small>
        </label>
        <label class="profile-field" for="profile-email">
          <span>Email đăng nhập</span>
          <input id="profile-email" :value="profile.email || user.email" readonly>
        </label>
        <label class="profile-field" for="profile-created-at">
          <span>Ngày tạo tài khoản</span>
          <input id="profile-created-at" :value="createdAtLabel" readonly>
        </label>
        <div class="profile-actions"><span class="profile-muted">Email và ngày tạo tài khoản không thể chỉnh sửa.</span><button class="button button-primary" type="submit" :disabled="isSaving">{{ isSaving ? 'Đang lưu...' : 'Lưu hồ sơ' }}</button></div>
      </form>
    </section>

    <section v-if="!isLoading" class="profile-panel" aria-labelledby="profile-password-heading">
      <div class="profile-panel-heading"><div><h2 id="profile-password-heading">Bảo mật tài khoản</h2><p>Đổi mật khẩu để bảo vệ quyền truy cập vào kế hoạch tài chính.</p></div></div>
      <form class="profile-form profile-password-form" @submit.prevent="submitPasswordChange">
        <label class="profile-field" for="current-password"><span>Mật khẩu hiện tại</span><input id="current-password" v-model="currentPassword" type="password" autocomplete="current-password" required></label>
        <label class="profile-field" for="new-password"><span>Mật khẩu mới</span><input id="new-password" v-model="newPassword" type="password" autocomplete="new-password" minlength="12" maxlength="72" required><small>Ít nhất 12 ký tự, tối đa 72 byte UTF-8.</small></label>
        <label class="profile-field" for="confirm-new-password"><span>Xác nhận mật khẩu mới</span><input id="confirm-new-password" v-model="confirmNewPassword" type="password" autocomplete="new-password" minlength="12" maxlength="72" required></label>
        <div class="profile-actions"><span class="profile-muted">Phiên đăng nhập hiện tại vẫn được giữ sau khi đổi mật khẩu.</span><button class="button button-primary" type="submit" :disabled="isChangingPassword">{{ isChangingPassword ? 'Đang cập nhật...' : 'Đổi mật khẩu' }}</button></div>
      </form>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { changePassword, getProfile, updateProfile } from '../services/auth.js'

const props = defineProps({ user: { type: Object, required: true } })
defineEmits(['close'])

const profile = ref({})
const displayName = ref('')
const avatarUrl = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmNewPassword = ref('')
const isLoading = ref(true)
const isSaving = ref(false)
const isChangingPassword = ref(false)
const avatarLoadFailed = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const initials = computed(() => (displayName.value || profile.value.email || props.user.email || 'P').trim().slice(0, 1).toUpperCase())
const avatarPreview = computed(() => avatarLoadFailed.value ? '' : avatarUrl.value)
const createdAtLabel = computed(() => profile.value.createdAt
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'long' }).format(new Date(profile.value.createdAt))
  : '—')

onMounted(async () => {
  try {
    profile.value = await getProfile()
    displayName.value = profile.value.displayName || ''
    avatarUrl.value = profile.value.avatarUrl || ''
  } catch (error) {
    errorMessage.value = error.message || 'Không tải được hồ sơ cá nhân.'
  } finally {
    isLoading.value = false
  }
})

async function saveProfile() {
  errorMessage.value = ''
  successMessage.value = ''
  isSaving.value = true
  try {
    profile.value = await updateProfile({ displayName: displayName.value, avatarUrl: avatarUrl.value || null })
    avatarLoadFailed.value = false
    successMessage.value = 'Đã cập nhật hồ sơ cá nhân.'
  } catch (error) {
    errorMessage.value = error.message || 'Không cập nhật được hồ sơ cá nhân.'
  } finally {
    isSaving.value = false
  }
}

async function submitPasswordChange() {
  errorMessage.value = ''
  successMessage.value = ''
  if (newPassword.value !== confirmNewPassword.value) {
    errorMessage.value = 'Mật khẩu xác nhận không khớp.'
    return
  }
  isChangingPassword.value = true
  try {
    await changePassword({ currentPassword: currentPassword.value, newPassword: newPassword.value })
    currentPassword.value = ''
    newPassword.value = ''
    confirmNewPassword.value = ''
    successMessage.value = 'Đã đổi mật khẩu. Hãy sử dụng mật khẩu mới cho lần đăng nhập tiếp theo.'
  } catch (error) {
    errorMessage.value = error.message || 'Không đổi được mật khẩu.'
  } finally {
    isChangingPassword.value = false
  }
}
</script>
