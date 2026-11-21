<template>
  <main class="auth-shell">
    <section class="auth-card" aria-labelledby="auth-title">
      <a class="auth-brand" href="/" aria-label="Planwise">plan<span>wise</span></a>
      <p class="auth-eyebrow">PERSONAL FINANCE PLANNER</p>
      <h1 id="auth-title">{{ title }}</h1>
      <p class="auth-description">{{ description }}</p>

      <div v-if="invitationToken" class="auth-invitation-note" role="status">
        Bạn đang tham gia household bằng lời mời riêng. Tài khoản của bạn vẫn có mật khẩu và phiên đăng nhập riêng.
      </div>
      <div v-else-if="!registrationAvailable && mode === 'login'" class="auth-invitation-note">
        Đăng ký chỉ khả dụng qua lời mời
      </div>

      <form class="auth-form" novalidate @submit.prevent="submit">
        <label class="auth-field" for="auth-email">
          <span>Email</span>
          <input id="auth-email" v-model="email" name="email" type="email" autocomplete="email" maxlength="254" required placeholder="you@example.com">
        </label>
        <label class="auth-field" for="auth-password">
          <span>Mật khẩu</span>
          <input id="auth-password" v-model="password" name="password" type="password" :autocomplete="isRegistering ? 'new-password' : 'current-password'" minlength="12" maxlength="72" required placeholder="Ít nhất 12 ký tự">
        </label>
        <label v-if="isRegistering" class="auth-field" for="auth-confirm-password">
          <span>Xác nhận mật khẩu</span>
          <input id="auth-confirm-password" v-model="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" minlength="12" maxlength="72" required placeholder="Nhập lại mật khẩu">
        </label>

        <p v-if="errorMessage" class="auth-error" role="alert">{{ errorMessage }}</p>
        <button class="button button-primary auth-submit" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Đang xử lý...' : submitLabel }}
        </button>
      </form>

      <button v-if="registrationAllowed" class="auth-mode-toggle" type="button" :disabled="isSubmitting" @click="toggleMode">
        {{ isRegistering ? 'Đã có tài khoản? Đăng nhập' : 'Chưa có tài khoản? Tạo tài khoản' }}
      </button>
      <p class="auth-security-note"><span aria-hidden="true">●</span> Dữ liệu được lưu riêng trong household của bạn.</p>
    </section>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue'
import { login, registerInitial, registerWithInvitation } from '../services/auth.js'

const props = defineProps({
  registrationAvailable: { type: Boolean, default: false },
  invitationToken: { type: String, default: null },
})

const emit = defineEmits(['authenticated'])

const registrationAllowed = computed(() => props.registrationAvailable || Boolean(props.invitationToken))
const mode = ref(registrationAllowed.value ? 'register' : 'login')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)
const isRegistering = computed(() => mode.value === 'register')
const title = computed(() => {
  if (!isRegistering.value) return 'Chào mừng trở lại'
  return props.invitationToken ? 'Tham gia household' : 'Tạo tài khoản chủ hộ'
})
const description = computed(() => isRegistering.value
  ? 'Tạo thông tin đăng nhập riêng để quản lý kế hoạch tài chính gia đình.'
  : 'Đăng nhập để tiếp tục kế hoạch tài chính của household.')
const submitLabel = computed(() => isRegistering.value ? 'Tạo tài khoản' : 'Đăng nhập')

function toggleMode() {
  mode.value = isRegistering.value ? 'login' : 'register'
  errorMessage.value = ''
}

async function submit() {
  errorMessage.value = ''
  const credentials = { email: email.value.trim(), password: password.value }
  if (!credentials.email || !credentials.password) {
    errorMessage.value = 'Vui lòng nhập email và mật khẩu.'
    return
  }
  if (isRegistering.value && password.value !== confirmPassword.value) {
    errorMessage.value = 'Mật khẩu xác nhận không khớp.'
    return
  }

  isSubmitting.value = true
  try {
    let user
    if (isRegistering.value && props.invitationToken) {
      user = await registerWithInvitation({ ...credentials, invitationToken: props.invitationToken })
    } else if (isRegistering.value) {
      user = await registerInitial(credentials)
    } else {
      user = await login(credentials)
    }
    emit('authenticated', user)
  } catch (error) {
    errorMessage.value = error.message || 'Không thể xác thực tài khoản.'
  } finally {
    isSubmitting.value = false
  }
}
</script>
