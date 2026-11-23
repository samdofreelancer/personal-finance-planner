<template>
  <main class="auth-shell">
    <section class="auth-card" aria-labelledby="auth-title">
      <a class="auth-brand" href="/" aria-label="Planwise">plan<span>wise</span></a>
      <p class="auth-eyebrow">QUẢN LÝ TÀI CHÍNH CÁ NHÂN</p>
      <h1 id="auth-title">{{ title }}</h1>
      <p class="auth-description">{{ description }}</p>

      <div v-if="invitationToken" class="auth-invitation-note" role="status">
        Bạn đang tham gia hộ gia đình bằng lời mời riêng. Tài khoản của bạn vẫn có mật khẩu và phiên đăng nhập riêng.
      </div>
      <form class="auth-form" novalidate @submit.prevent="submit">
        <label v-if="!isResetting" class="auth-field" for="auth-email">
          <span>Email</span>
          <input id="auth-email" v-model="email" name="email" type="email" autocomplete="email" maxlength="254" :required="!isResetting" placeholder="you@example.com">
        </label>
        <div v-if="!isForgot" class="auth-field">
          <div class="auth-field-heading">
            <label for="auth-password">{{ isResetting ? 'Mật khẩu mới' : 'Mật khẩu' }}</label>
            <button v-if="mode === 'login'" class="auth-forgot-link" type="button" @click="switchMode('forgot')">Quên mật khẩu?</button>
          </div>
          <div class="auth-password-wrap">
            <input id="auth-password" v-model="password" name="password" :type="showPassword ? 'text' : 'password'" :autocomplete="isRegistering || isResetting ? 'new-password' : 'current-password'" :minlength="isRegistering || isResetting ? 12 : undefined" maxlength="72" required :placeholder="isRegistering || isResetting ? 'Tạo mật khẩu của bạn' : '••••••••••••'">
            <button class="auth-password-toggle" type="button" :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'" :aria-pressed="showPassword" @click="showPassword = !showPassword">
              <svg v-if="!showPassword" viewBox="0 0 24 24" aria-hidden="true"><path d="M2.5 12s3.3-6 9.5-6 9.5 6 9.5 6-3.3 6-9.5 6-9.5-6-9.5-6Z"/><circle cx="12" cy="12" r="2.7"/></svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="m3 3 18 18M10.6 6.2A10.7 10.7 0 0 1 12 6c6.2 0 9.5 6 9.5 6a16 16 0 0 1-3 3.5M6.2 6.3C3.8 8 2.5 12 2.5 12s3.3 6 9.5 6c1 0 2-.2 2.8-.5"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/></svg>
            </button>
          </div>
          <p v-if="isRegistering || isResetting" class="auth-field-hint">Mật khẩu cần có ít nhất 12 ký tự.</p>
        </div>
        <label v-if="isRegistering || isResetting" class="auth-field" for="auth-confirm-password">
          <span>Xác nhận mật khẩu</span>
          <input id="auth-confirm-password" v-model="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" minlength="12" maxlength="72" required placeholder="Nhập lại mật khẩu">
        </label>

        <p v-if="errorMessage" class="auth-error" role="alert">{{ errorMessage }}</p>
        <p v-if="successMessage" class="auth-success" role="status">{{ successMessage }}</p>
        <button class="button button-primary auth-submit" type="submit" :disabled="isSubmitting || !canSubmit">
          {{ isSubmitting ? 'Đang xử lý...' : submitLabel }}
        </button>
      </form>

      <button v-if="mode === 'login' || mode === 'register'" class="auth-mode-toggle" type="button" :disabled="isSubmitting" @click="toggleMode">
        {{ isRegistering ? 'Đã có tài khoản? Đăng nhập' : 'Chưa có tài khoản? Tạo tài khoản' }}
      </button>
      <button v-else class="auth-mode-toggle" type="button" :disabled="isSubmitting" @click="switchMode('login')">
        Quay lại đăng nhập
      </button>
      <p class="auth-security-note"><span aria-hidden="true">●</span> Dữ liệu tài chính được lưu riêng trong hộ gia đình của bạn.</p>
    </section>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue'
import { confirmPasswordReset, login, registerAccount, registerWithInvitation, requestPasswordReset } from '../services/auth.js'

const props = defineProps({
  invitationToken: { type: String, default: null },
  resetToken: { type: String, default: null },
})

const emit = defineEmits(['authenticated', 'password-reset'])

const mode = ref(props.resetToken ? 'reset' : (props.invitationToken ? 'register' : 'login'))
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const isSubmitting = ref(false)
const isRegistering = computed(() => mode.value === 'register')
const isResetting = computed(() => mode.value === 'reset')
const isForgot = computed(() => mode.value === 'forgot')
const canSubmit = computed(() => {
  if (isForgot.value) return Boolean(email.value.trim())
  if (isResetting.value || isRegistering.value) {
    return Boolean(password.value.length >= 12 && password.value === confirmPassword.value)
      && (isResetting.value || Boolean(email.value.trim()))
  }
  return Boolean(email.value.trim() && password.value)
})
const title = computed(() => {
  if (isForgot.value) return 'Khôi phục mật khẩu'
  if (isResetting.value) return 'Tạo mật khẩu mới'
  if (!isRegistering.value) return 'Chào mừng trở lại'
  return props.invitationToken ? 'Tham gia hộ gia đình' : 'Tạo tài khoản'
})
const description = computed(() => {
  if (isForgot.value) return 'Nhập email đã đăng ký. Nếu tài khoản tồn tại, chúng tôi sẽ gửi liên kết đặt lại mật khẩu.'
  if (isResetting.value) return 'Chọn mật khẩu mới để tiếp tục sử dụng kế hoạch tài chính gia đình.'
  if (isRegistering.value) return 'Tạo thông tin đăng nhập riêng để quản lý kế hoạch tài chính gia đình.'
  return 'Đăng nhập để tiếp tục kế hoạch tài chính gia đình.'
})
const submitLabel = computed(() => {
  if (isForgot.value) return 'Gửi liên kết khôi phục'
  if (isResetting.value) return 'Đặt lại mật khẩu'
  return isRegistering.value ? 'Tạo tài khoản' : 'Đăng nhập'
})

function toggleMode() {
  switchMode(isRegistering.value ? 'login' : 'register')
}

function switchMode(nextMode) {
  mode.value = nextMode
  errorMessage.value = ''
  successMessage.value = ''
}

async function submit() {
  errorMessage.value = ''
  successMessage.value = ''
  if (isForgot.value) {
    isSubmitting.value = true
    try {
      await requestPasswordReset({ email: email.value.trim() })
      successMessage.value = 'Nếu email thuộc về một tài khoản, chúng tôi sẽ gửi liên kết khôi phục trong ít phút.'
    } catch (error) {
      errorMessage.value = error.message || 'Không gửi được yêu cầu khôi phục mật khẩu.'
    } finally {
      isSubmitting.value = false
    }
    return
  }

  if (isResetting.value) {
    if (password.value !== confirmPassword.value) {
      errorMessage.value = 'Mật khẩu xác nhận không khớp.'
      return
    }
    isSubmitting.value = true
    try {
      await confirmPasswordReset({ token: props.resetToken, newPassword: password.value })
      password.value = ''
      confirmPassword.value = ''
      switchMode('login')
      successMessage.value = 'Mật khẩu đã được đặt lại. Vui lòng đăng nhập bằng mật khẩu mới.'
      emit('password-reset')
    } catch (error) {
      errorMessage.value = error.message || 'Không đặt lại được mật khẩu.'
    } finally {
      isSubmitting.value = false
    }
    return
  }

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
      user = await registerAccount(credentials)
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
