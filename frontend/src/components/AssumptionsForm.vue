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

        <fieldset class="input-group expense-schedules-group">
          <legend><span class="legend-icon expense-icon" aria-hidden="true">↘</span> Chi phí hằng tháng</legend>
          <p class="field-hint">Tự thêm khoản chi và đặt số tiền theo từng tháng bắt đầu áp dụng. Đặt số tiền bằng 0 để ngừng khoản chi.</p>
          <article v-for="(expense, expenseIndex) in expenseDraft" :key="expenseIndex" class="expense-editor">
            <div class="expense-editor-heading">
              <label class="field expense-name-field">
                <span>Tên khoản chi</span>
                <span class="input-wrap"><input v-model="expense.name" :aria-label="`Tên khoản chi ${expenseIndex + 1}`" maxlength="120" placeholder="Ví dụ: Học phí, bảo hiểm..."></span>
              </label>
              <button class="text-button expense-remove" type="button" @click="removeExpense(expenseIndex)">Xóa khoản</button>
            </div>
            <div v-for="(change, changeIndex) in expense.changes" :key="changeIndex" class="expense-change-row">
              <label class="field">
                <span>Áp dụng từ</span>
                <span class="input-wrap"><input v-model="change.effectiveMonth" type="month" :aria-label="`Tháng hiệu lực khoản ${expenseIndex + 1}, mốc ${changeIndex + 1}`"></span>
              </label>
              <label class="field">
                <span>Số tiền / tháng</span>
                <span class="input-wrap"><currency-input v-model="change.amount" :name="`expense-${expenseIndex}-change-${changeIndex}`" :aria-label="`Số tiền khoản ${expense.name || expenseIndex + 1}, mốc ${changeIndex + 1}`" /><span>₫</span></span>
              </label>
              <button class="text-button expense-change-remove" type="button" :disabled="expense.changes.length === 1" @click="removeChange(expenseIndex, changeIndex)">Xóa mốc</button>
            </div>
            <button class="text-button expense-add-change" type="button" @click="addChange(expenseIndex)">+ Thêm mốc thay đổi</button>
          </article>
          <button class="button button-secondary expense-add" type="button" @click="addExpense">+ Thêm khoản chi</button>
        </fieldset>

        <fieldset class="input-group savings-group">
          <legend><span class="legend-icon income-icon" aria-hidden="true">◎</span> Tiết kiệm</legend>
          <label class="field" for="startingSavings"><span>Tiền tiết kiệm ban đầu</span><span class="input-wrap"><currency-input id="startingSavings" v-model="formDto.startingSavings" name="startingSavings" /><span>₫</span></span></label>
          <p class="field-hint">Tiết kiệm ban đầu là số dư khởi điểm; tiết kiệm lũy kế được tính từ dòng tiền dự báo.</p>
        </fieldset>
      </div>
      <div id="form-error" class="form-error" role="alert">{{ error }}</div>
      <div class="form-footer">
        <span id="save-status" class="save-status" role="status">{{ status }}</span>
        <div class="form-actions">
          <span class="form-note">Đơn vị tiền tệ: VND</span>
          <button id="save-assumptions-button" class="button button-primary" type="submit" :disabled="status === 'Đang lưu...'">Lưu</button>
        </div>
      </div>
    </form>
  </section>
</template>

<script setup>
import { reactive, watch } from 'vue'
import CurrencyInput from './CurrencyInput.vue'

const props = defineProps(['dto', 'expenses', 'error', 'status'])
const emit = defineEmits(['input', 'expenses-input', 'reset', 'save'])
const years = [2027, 2028, 2029, 2030]
const formDto = reactive({})
const raiseText = reactive({})
const expenseDraft = reactive([])

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
watch(() => props.expenses, (value) => {
  expenseDraft.splice(0, expenseDraft.length, ...JSON.parse(JSON.stringify(value || [])))
}, { deep: true, immediate: true })

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

function onInput() {
  emit('input', buildDto())
  emit('expenses-input', buildExpenses())
}

function buildExpenses() {
  return JSON.parse(JSON.stringify(expenseDraft))
}

function emitExpenseInput() {
  emit('expenses-input', buildExpenses())
}

function addExpense() {
  expenseDraft.push({ name: '', changes: [{ effectiveMonth: '2026-11', amount: 0 }] })
  emitExpenseInput()
}

function removeExpense(index) {
  expenseDraft.splice(index, 1)
  emitExpenseInput()
}

function addChange(index) {
  const lastMonth = expenseDraft[index].changes.at(-1)?.effectiveMonth || '2026-11'
  const [year, month] = lastMonth.split('-').map(Number)
  const next = new Date(year, month, 1)
  expenseDraft[index].changes.push({
    effectiveMonth: `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`,
    amount: expenseDraft[index].changes.at(-1)?.amount ?? 0,
  })
  emitExpenseInput()
}

function removeChange(expenseIndex, changeIndex) {
  expenseDraft[expenseIndex].changes.splice(changeIndex, 1)
  emitExpenseInput()
}

function onSubmit() {
  emit('save', buildDto(), buildExpenses())
}
</script>
