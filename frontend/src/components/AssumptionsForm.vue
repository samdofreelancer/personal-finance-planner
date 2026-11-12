<template>
  <section class="panel assumptions-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">ĐẦU VÀO</p>
        <h2>Giả định của bạn</h2>
      </div>
      <button id="reset-button" class="text-button" type="button" @click="$emit('reset')">Khôi phục mặc định</button>
    </div>
    <form id="assumptions-form" novalidate @input="onInput" @submit.prevent="onSubmit">
      <div class="input-groups">
        <fieldset class="input-group">
          <legend><span class="legend-icon income-icon" aria-hidden="true">↗</span> Thu nhập</legend>
          <label class="field">
            <span>Lương tháng cơ sở năm 2026</span>
            <span class="input-wrap"><input v-model.number="dto.baseSalary" name="baseSalary" type="number" min="0" step="1" required><span>₫ / tháng</span></span>
          </label>
          <label class="field">
            <span>Hệ số lương tháng 1</span>
            <span class="input-wrap input-wrap-short"><input v-model.number="dto.januaryMultiplier" name="januaryMultiplier" type="number" min="0" max="20" step="0.1" required><span>×</span></span>
          </label>
          <p class="field-hint">Các đợt tăng lương tháng 4 được áp dụng cộng dồn.</p>
          <div class="raise-grid">
            <label v-for="year in years" :key="year" class="field">
              <span>Tăng từ 04/{{ year }}</span>
              <span class="input-wrap"><input :value="raiseText[year]" :name="'raise' + year" type="number" min="0" max="100" step="0.1" required @input="onRaiseInput(year, $event)"><span>%</span></span>
            </label>
          </div>
        </fieldset>

        <fieldset class="input-group">
          <legend><span class="legend-icon expense-icon" aria-hidden="true">↘</span> Chi phí hằng tháng</legend>
          <label class="field"><span>Sinh hoạt phí</span><span class="input-wrap"><input v-model.number="dto.living" name="living" type="number" min="0" step="1" required><span>₫</span></span></label>
          <label class="field"><span>Quỹ nuôi con · đến hết 12/2026</span><span class="input-wrap"><input v-model.number="dto.childThrough2026" name="childThrough2026" type="number" min="0" step="1" required><span>₫</span></span></label>
          <label class="field"><span>Quỹ nuôi con · 01–03/2027</span><span class="input-wrap"><input v-model.number="dto.childQ12027" name="childQ12027" type="number" min="0" step="1" required><span>₫</span></span></label>
          <label class="field"><span>Quỹ nuôi con · từ 04/2027</span><span class="input-wrap"><input v-model.number="dto.childFromApr2027" name="childFromApr2027" type="number" min="0" step="1" required><span>₫</span></span></label>
          <label class="field"><span>Tiền nhà · đến hết 01/2027</span><span class="input-wrap"><input v-model.number="dto.rentThroughJan2027" name="rentThroughJan2027" type="number" min="0" step="1" required><span>₫</span></span></label>
          <label class="field"><span>Tiền nhà · từ 02/2027</span><span class="input-wrap"><input v-model.number="dto.rentFromFeb2027" name="rentFromFeb2027" type="number" min="0" step="1" required><span>₫</span></span></label>
        </fieldset>

        <fieldset class="input-group">
          <legend><span class="legend-icon loan-icon" aria-hidden="true">⌁</span> Khoản vay &amp; tiết kiệm</legend>
          <label class="field"><span>Dư nợ gốc tại 28/10/2026</span><span class="input-wrap"><input v-model.number="dto.loanPrincipal" name="loanPrincipal" type="number" min="0" step="1" required><span>₫</span></span></label>
          <label class="field"><span>Khoản trả cố định tháng 11–12/2026</span><span class="input-wrap"><input v-model.number="dto.fixedPayment" name="fixedPayment" type="number" min="0" step="1" required><span>₫ / tháng</span></span></label>
          <label class="field"><span>Lãi ngân hàng · kỳ 30/11/2026</span><span class="input-wrap"><input v-model.number="dto.novInterest" name="novInterest" type="number" min="0" step="1" required><span>₫</span></span></label>
          <label class="field"><span>Lãi ngân hàng · kỳ 28/12/2026</span><span class="input-wrap"><input v-model.number="dto.decInterest" name="decInterest" type="number" min="0" step="1" required><span>₫</span></span></label>
          <label class="field"><span>Lãi tất toán · 25/01/2027</span><span class="input-wrap"><input :value="dto.payoffInterest ?? ''" name="payoffInterest" type="number" min="0" step="1" placeholder="Chưa xác nhận" :aria-invalid="dto.payoffInterest == null ? 'true' : undefined" @input="onPayoffInput"><span>₫</span></span></label>
          <label class="confirm-field"><input v-model="dto.payoffInterestConfirmed" name="payoffInterestConfirmed" type="checkbox" @change="onConfirmedChange"><span>Đã đối chiếu số lãi này với ngân hàng</span></label>
          <p class="field-hint">Lãi tất toán cần đối chiếu báo giá ngân hàng. Lãi suất 13,2%/năm chỉ để tham khảo, không dùng để ước tính các khoản lãi thực tế.</p>
          <label class="field"><span>Tiền tiết kiệm ban đầu</span><span class="input-wrap"><input v-model.number="dto.startingSavings" name="startingSavings" type="number" min="0" step="1" required><span>₫</span></span></label>
        </fieldset>
      </div>
      <div id="form-error" class="form-error" role="alert">{{ error }}</div>
      <div class="form-footer"><span id="save-status" class="save-status" role="status">{{ status }}</span><span class="form-note">Đơn vị tiền tệ: VND</span></div>
    </form>
  </section>
</template>

<script setup>
import { reactive, watch } from 'vue'

const props = defineProps(['dto', 'error', 'status'])
const emit = defineEmits(['input', 'reset', 'save'])
const years = [2027, 2028, 2029, 2030]
const raiseText = reactive({})

function fractionToText(value) {
  const n = Number(value)
  return Number.isFinite(n) ? String(Math.round(n * 1000000) / 10000) : ''
}

function sameFraction(text, fraction) {
  if (text === '' || text == null) return !Number(fraction)
  return Math.abs(Number(text) / 100 - Number(fraction)) < 1e-12
}

function syncRaiseText() {
  for (const year of years) {
    const fraction = props.dto.raises?.[year]
    if (!sameFraction(raiseText[year], fraction)) raiseText[year] = fractionToText(fraction)
  }
}

syncRaiseText()
watch(() => props.dto.raises, syncRaiseText, { deep: true })

function buildDto() {
  const copy = { ...props.dto, raises: {} }
  for (const year of years) {
    copy.raises[year] = raiseText[year] === '' ? 0 : Number(raiseText[year]) / 100
  }
  return copy
}

function onRaiseInput(year, event) {
  raiseText[year] = event.target.value
}

function onPayoffInput(event) {
  const raw = event.target.value
  props.dto.payoffInterest = raw === '' ? null : Number(raw)
}

function onConfirmedChange(event) {
  props.dto.payoffInterestConfirmed = event.target.checked
  emit('input', buildDto())
}

function onInput() {
  emit('input', buildDto())
}

function onSubmit() {
  emit('save', buildDto())
}
</script>
