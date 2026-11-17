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
          <label class="field" for="baseSalary">
            <span>Lương tháng cơ sở năm 2026</span>
            <span class="input-wrap"><currency-input id="baseSalary" v-model="formDto.baseSalary" name="baseSalary" /><span>₫ / tháng</span></span>
          </label>
          <label class="field" for="januaryMultiplier">
            <span>Hệ số lương tháng 1</span>
            <span class="input-wrap input-wrap-short"><input id="januaryMultiplier" v-model.number="formDto.januaryMultiplier" name="januaryMultiplier" type="number" min="0" max="20" step="0.1" required><span>×</span></span>
          </label>
          <p class="field-hint">Các đợt tăng lương tháng 4 được áp dụng cộng dồn.</p>
          <div class="raise-grid">
            <label v-for="year in years" :key="year" class="field" :for="'raise' + year">
              <span>Tăng từ 04/{{ year }}</span>
              <span class="input-wrap"><input :id="'raise' + year" :value="raiseText[year]" :name="'raise' + year" type="number" min="0" max="100" step="0.1" required @input="onRaiseInput(year, $event)"><span>%</span></span>
            </label>
          </div>
        </fieldset>

        <fieldset class="input-group">
          <legend><span class="legend-icon expense-icon" aria-hidden="true">↘</span> Chi phí hằng tháng</legend>
          <label class="field" for="living"><span>Sinh hoạt phí</span><span class="input-wrap"><currency-input id="living" v-model="formDto.living" name="living" /><span>₫</span></span></label>
          <label class="field" for="childThrough2026"><span>Quỹ nuôi con · đến hết 12/2026</span><span class="input-wrap"><currency-input id="childThrough2026" v-model="formDto.childThrough2026" name="childThrough2026" /><span>₫</span></span></label>
          <label class="field" for="childQ12027"><span>Quỹ nuôi con · 01–03/2027</span><span class="input-wrap"><currency-input id="childQ12027" v-model="formDto.childQ12027" name="childQ12027" /><span>₫</span></span></label>
          <label class="field" for="childFromApr2027"><span>Quỹ nuôi con · từ 04/2027</span><span class="input-wrap"><currency-input id="childFromApr2027" v-model="formDto.childFromApr2027" name="childFromApr2027" /><span>₫</span></span></label>
          <label class="field" for="rentThroughJan2027"><span>Tiền nhà · đến hết 01/2027</span><span class="input-wrap"><currency-input id="rentThroughJan2027" v-model="formDto.rentThroughJan2027" name="rentThroughJan2027" /><span>₫</span></span></label>
          <label class="field" for="rentFromFeb2027"><span>Tiền nhà · từ 02/2027</span><span class="input-wrap"><currency-input id="rentFromFeb2027" v-model="formDto.rentFromFeb2027" name="rentFromFeb2027" /><span>₫</span></span></label>
        </fieldset>

        <fieldset class="input-group">
          <legend><span class="legend-icon loan-icon" aria-hidden="true">⌁</span> Khoản vay &amp; tiết kiệm</legend>
          <label class="field" for="loanPrincipal"><span>Dư nợ gốc ban đầu</span><span class="input-wrap"><currency-input id="loanPrincipal" v-model="formDto.loanPrincipal" name="loanPrincipal" /><span>₫</span></span></label>
          <label class="field" for="fixedPayment"><span>Khoản trả cố định tháng 11–12/2026</span><span class="input-wrap"><currency-input id="fixedPayment" v-model="formDto.fixedPayment" name="fixedPayment" /><span>₫ / tháng</span></span></label>
          <label class="field" for="novInterest"><span>Lãi ngân hàng · kỳ 1</span><span class="input-wrap"><currency-input id="novInterest" v-model="formDto.novInterest" name="novInterest" /><span>₫</span></span></label>
          <label class="field" for="decInterest"><span>Lãi ngân hàng · kỳ 2</span><span class="input-wrap"><currency-input id="decInterest" v-model="formDto.decInterest" name="decInterest" /><span>₫</span></span></label>
          <label class="field" for="payoffInterest"><span>Lãi tất toán dự kiến</span><span class="input-wrap"><currency-input id="payoffInterest" v-model="formDto.payoffInterest" name="payoffInterest" nullable :required="false" :aria-invalid="formDto.payoffInterest == null ? 'true' : undefined" placeholder="Chưa xác nhận" /><span>₫</span></span></label>
          <label class="confirm-field"><input v-model="formDto.payoffInterestConfirmed" name="payoffInterestConfirmed" type="checkbox" @change="onConfirmedChange"><span>Đã đối chiếu số lãi này với ngân hàng</span></label>
          <p class="field-hint">Lãi tất toán cần đối chiếu báo giá ngân hàng trước khi xác nhận.</p>
          <label class="field" for="startingSavings"><span>Tiền tiết kiệm ban đầu</span><span class="input-wrap"><currency-input id="startingSavings" v-model="formDto.startingSavings" name="startingSavings" /><span>₫</span></span></label>
        </fieldset>
      </div>
      <div id="form-error" class="form-error" role="alert">{{ error }}</div>
      <div class="form-footer"><span id="save-status" class="save-status" role="status">{{ status }}</span><span class="form-note">Đơn vị tiền tệ: VND</span></div>
    </form>
  </section>
</template>

<script setup>
import { reactive, watch } from 'vue'
import CurrencyInput from './CurrencyInput.vue'

const props = defineProps(['dto', 'error', 'status'])
const emit = defineEmits(['input', 'reset', 'save'])
const years = [2027, 2028, 2029, 2030]
const formDto = reactive({})
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
    const fraction = formDto.raises?.[year]
    if (!sameFraction(raiseText[year], fraction)) raiseText[year] = fractionToText(fraction)
  }
}

function syncFormDto() {
  Object.assign(formDto, props.dto, { raises: { ...props.dto.raises } })
  syncRaiseText()
}

watch(() => props.dto, syncFormDto, { deep: true, immediate: true })

function buildDto() {
  const copy = { ...formDto, raises: {} }
  for (const year of years) {
    copy.raises[year] = raiseText[year] === '' ? 0 : Number(raiseText[year]) / 100
  }
  return copy
}

function onRaiseInput(year, event) {
  raiseText[year] = event.target.value
}

function onConfirmedChange(event) {
  formDto.payoffInterestConfirmed = event.target.checked
  emit('input', buildDto())
}

function onInput() {
  emit('input', buildDto())
}

function onSubmit() {
  emit('save', buildDto())
}
</script>
