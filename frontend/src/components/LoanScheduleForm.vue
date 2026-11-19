<template>
  <section class="panel loan-schedule-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">QUẢN LÝ DƯ NỢ</p>
        <h2>Khoản vay</h2>
        <p class="section-description">Mỗi khoản vay có lịch trả riêng. Bạn có thể thêm nhiều kỳ trong cùng tháng.</p>
      </div>
      <span class="loan-count">{{ draft.length }} khoản vay</span>
    </div>

    <form @submit.prevent="save">
      <div v-if="draft.length" class="loan-list">
        <article v-for="(loan, loanIndex) in draft" :key="loan.key" class="loan-card">
          <div class="loan-card-heading">
            <label class="field loan-name-field" :for="`loan-name-${loan.key}`">
              <span>Tên khoản vay</span>
              <input :id="`loan-name-${loan.key}`" v-model.trim="loan.name" type="text" maxlength="120" placeholder="Ví dụ: Vay mua nhà" required>
            </label>
            <label class="field loan-principal-field" :for="`loan-principal-${loan.key}`">
              <span>Dư nợ gốc ban đầu (VND)</span>
              <input :id="`loan-principal-${loan.key}`" v-model.number="loan.openingPrincipal" type="number" min="0" step="1" required>
            </label>
            <button class="text-button danger-text" type="button" @click="removeLoan(loanIndex)">Xóa khoản vay</button>
          </div>

          <div class="repayment-heading">
            <strong>Lịch trả nợ</strong>
            <span>{{ loan.repayments.length }} kỳ</span>
          </div>
          <div v-if="loan.repayments.length" class="repayment-list">
            <div v-for="(payment, paymentIndex) in loan.repayments" :key="payment.key" class="repayment-row">
              <label class="field" :for="`repayment-date-${payment.key}`">
                <span>Ngày trả</span>
                <input :id="`repayment-date-${payment.key}`" v-model="payment.dueDate" type="date" required>
              </label>
              <label class="field" :for="`repayment-principal-${payment.key}`">
                <span>Tiền gốc (VND)</span>
                <input :id="`repayment-principal-${payment.key}`" v-model.number="payment.principalAmount" type="number" min="0" step="1" required>
              </label>
              <label class="field" :for="`repayment-interest-${payment.key}`">
                <span>Tiền lãi (VND)</span>
                <input :id="`repayment-interest-${payment.key}`" v-model.number="payment.interestAmount" type="number" min="0" step="1" required>
              </label>
              <div class="repayment-total"><span>Tổng kỳ</span><strong>{{ money(amount(payment.principalAmount) + amount(payment.interestAmount)) }}</strong></div>
              <button class="icon-button danger-text" type="button" :aria-label="`Xóa kỳ trả ${paymentIndex + 1}`" @click="removeRepayment(loan, paymentIndex)">×</button>
            </div>
          </div>
          <p v-else class="empty-repayments">Chưa có kỳ trả. Thêm kỳ theo kế hoạch của bạn.</p>
          <button class="text-button add-repayment" type="button" @click="addRepayment(loan)">＋ Thêm kỳ trả</button>
        </article>
      </div>
      <div v-else class="empty-state loan-empty">Chưa có khoản vay. Thêm khoản vay để đưa lịch trả vào dự báo.</div>

      <div v-if="validationError" class="form-error" role="alert">{{ validationError }}</div>
      <div v-if="error" class="form-error" role="alert">{{ error }}</div>
      <div class="loan-form-footer">
        <button class="button button-secondary" type="button" @click="addLoan">＋ Thêm khoản vay</button>
        <div class="loan-save-actions">
          <span class="save-status" role="status">{{ statusLabel }}</span>
          <button class="button button-primary" type="submit" :disabled="Boolean(validationError) || status === 'Đang lưu...'"><span aria-hidden="true">✓</span> Lưu</button>
        </div>
      </div>
      <p class="field-hint loan-footnote">Dự báo lấy lịch trả đã lưu. Gốc trả được giới hạn theo dư nợ còn lại; lãi được cộng đúng theo từng kỳ đã nhập.</p>
    </form>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { formatCurrency } from '../utils/currency.js'

const props = defineProps({ loans: { type: Array, default: () => [] }, status: { type: String, default: '' }, error: { type: String, default: '' } })
const emit = defineEmits(['save'])
const draft = ref([])
let nextKey = 0
const money = formatCurrency
const amount = (value) => Number.isSafeInteger(value) && value >= 0 ? value : 0
function validDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || '')
  if (!match) return false
  const [, year, month, day] = match.map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
}

function normalize(loans) {
  draft.value = (loans || []).map((loan) => ({
    ...loan,
    key: `loan-${nextKey++}`,
    repayments: (loan.repayments || []).map((payment) => ({ ...payment, key: `repayment-${nextKey++}` })),
  }))
}

watch(() => props.loans, normalize, { immediate: true, deep: true })

function addLoan() {
  draft.value.push({ key: `loan-${nextKey++}`, id: null, name: '', openingPrincipal: 0, repayments: [] })
}

function removeLoan(index) { draft.value.splice(index, 1) }

function addRepayment(loan) {
  loan.repayments.push({ key: `repayment-${nextKey++}`, id: null, dueDate: '', principalAmount: 0, interestAmount: 0 })
}

function removeRepayment(loan, index) { loan.repayments.splice(index, 1) }

const validationError = computed(() => {
  for (const loan of draft.value) {
    if (!loan.name?.trim()) return 'Vui lòng nhập tên cho từng khoản vay.'
    if (loan.name.trim().length > 120) return 'Tên khoản vay không được vượt quá 120 ký tự.'
    if (!Number.isSafeInteger(loan.openingPrincipal) || loan.openingPrincipal < 0) return 'Dư nợ gốc phải là số VND nguyên không âm.'
    for (const payment of loan.repayments) {
      if (!validDate(payment.dueDate)) return 'Vui lòng chọn ngày trả hợp lệ cho từng kỳ.'
      if (!Number.isSafeInteger(payment.principalAmount) || payment.principalAmount < 0) return 'Tiền gốc phải là số VND nguyên không âm.'
      if (!Number.isSafeInteger(payment.interestAmount) || payment.interestAmount < 0) return 'Tiền lãi phải là số VND nguyên không âm.'
    }
  }
  return ''
})

const savePayload = computed(() => draft.value.map(({ key, ...loan }) => ({
  ...loan,
  repayments: loan.repayments.map(({ key: repaymentKey, ...payment }) => payment),
})))
const isDirty = computed(() => JSON.stringify(savePayload.value) !== JSON.stringify(props.loans || []))
const statusLabel = computed(() => props.status === 'Đang lưu...'
  ? props.status
  : isDirty.value ? 'Có thay đổi chưa lưu' : props.status)

function save() {
  if (validationError.value) return
  emit('save', savePayload.value)
}
</script>
