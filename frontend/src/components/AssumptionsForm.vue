<template>
  <section class="panel assumptions-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">ĐẦU VÀO</p>
        <h2>Giả định của bạn</h2>
      </div>
      <div class="assumptions-heading-actions">
        <button id="load-sample-button" class="button button-secondary sample-data-button" type="button" @click="$emit('load-sample')">Nạp dữ liệu mẫu</button>
        <button id="reset-button" class="text-button" type="button" @click="$emit('reset')">Khôi phục mặc định</button>
      </div>
    </div>
    <form id="assumptions-form" novalidate @input="onInput" @submit.prevent="onSubmit">
      <div class="input-groups">
        <fieldset class="input-group">
          <legend><span class="legend-icon income-icon" aria-hidden="true">↗</span> Thu nhập</legend>
          <p class="field-hint">Nhập mức thu nhập tháng và tháng bắt đầu áp dụng. Mỗi tháng dự phóng dùng mức gần nhất có hiệu lực; không tự động tăng theo tỷ lệ.</p>
          <article v-for="(change, changeIndex) in sortedIncomeChanges" :key="changeIndex" class="income-change-row">
            <label class="field">
              <span>Áp dụng từ</span>
              <span class="input-wrap"><month-input v-model="change.effectiveMonth" :disabled="change.isBaseline" :aria-label="`Tháng hiệu lực thu nhập, mốc ${changeIndex + 1}`" /></span>
            </label>
            <label class="field">
              <span>Thu nhập / tháng</span>
              <span class="input-wrap"><currency-input v-model="change.amount" :name="`income-change-${changeIndex}`" :aria-label="`Số tiền thu nhập, mốc ${changeIndex + 1}`" /><span>₫</span></span>
            </label>
            <button class="text-button income-change-remove" type="button" :disabled="incomeDraft.length <= 1 || change.isBaseline" :aria-label="`Xóa mốc thu nhập ${changeIndex + 1}`" @click="removeIncomeChange(change)">Xóa mốc</button>
          </article>
          <button class="text-button income-add-change" type="button" @click="addIncomeChange">+ Thêm mốc thu nhập</button>
          <label class="field" for="januaryMultiplier">
            <span>Hệ số lương tháng 1</span>
            <span class="input-wrap input-wrap-short"><input id="januaryMultiplier" v-model.number="formDto.januaryMultiplier" name="januaryMultiplier" type="number" min="0" max="20" step="0.1" required><span>×</span></span>
          </label>
          <p class="field-hint multiplier-hint">Hệ số này chỉ nhân vào thu nhập tháng 1 hằng năm (tương đương thưởng / lương tháng 13); các tháng còn lại giữ nguyên mức thu nhập đang có hiệu lực.</p>
          <label class="field" for="forecastYears">
            <span>Thời gian dự phóng</span>
            <span class="input-wrap input-wrap-short">
              <select id="forecastYears" :value="formDto.forecastYears" name="forecastYears" @input.stop="onHorizonChange">
                <option v-for="year in 6" :key="year" :value="year">{{ year }} năm</option>
              </select>
            </span>
          </label>
          <p class="field-hint">Dự phóng bắt đầu từ 11/2026 và kết thúc vào tháng 12 sau số năm đã chọn. Các mốc thu nhập sau kỳ dự phóng vẫn được lưu để dùng khi mở rộng thời gian.</p>
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
                <span class="input-wrap"><month-input v-model="change.effectiveMonth" :aria-label="`Tháng hiệu lực khoản ${expenseIndex + 1}, mốc ${changeIndex + 1}`" /></span>
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
        <div class="save-controls">
          <span v-if="status" id="save-status" class="save-status" :class="statusClass" role="status">{{ status }}</span>
          <span class="form-note">Đơn vị tiền tệ: VND</span>
          <button id="save-assumptions-button" class="button button-primary" type="submit" :disabled="status === 'Đang lưu...'">Lưu</button>
        </div>
      </div>
    </form>
  </section>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import CurrencyInput from './CurrencyInput.vue'
import MonthInput from './MonthInput.vue'

const props = defineProps(['dto', 'expenses', 'error', 'status'])
const emit = defineEmits(['input', 'expenses-input', 'reset', 'save', 'load-sample'])
const formDto = reactive({})
const incomeDraft = reactive([])
const expenseDraft = reactive([])
const sortedIncomeChanges = computed(() => [...incomeDraft].sort((a, b) => {
  if (a.isBaseline) return -1
  if (b.isBaseline) return 1
  return String(a.effectiveMonth || '').localeCompare(String(b.effectiveMonth || ''))
}))
const statusClass = computed(() => {
  if (statusIsDirty(props.status)) return 'save-status-dirty'
  return ''
})

function statusIsDirty(value) {
  return /chưa lưu|chưa hợp lệ|không cập nhật được/i.test(value || '')
}

function syncFormDto() {
  Object.assign(formDto, props.dto)
  const changes = JSON.parse(JSON.stringify(props.dto.incomeChanges || []))
  let baselineAssigned = false
  for (const change of changes) {
    change.isBaseline = change.effectiveMonth === '2026-11' && !baselineAssigned
    if (change.isBaseline) baselineAssigned = true
  }
  incomeDraft.splice(0, incomeDraft.length, ...changes)
}

watch(() => props.dto, syncFormDto, { deep: true, immediate: true })
watch(() => props.expenses, (value) => {
  expenseDraft.splice(0, expenseDraft.length, ...JSON.parse(JSON.stringify(value || [])))
}, { deep: true, immediate: true })

function buildDto() {
  return {
    ...formDto,
    incomeChanges: sortedIncomeChanges.value.map(({ effectiveMonth, amount }) => ({ effectiveMonth, amount })),
  }
}

function onInput() {
  emit('input', buildDto())
  emit('expenses-input', buildExpenses())
}

function onHorizonChange(event) {
  // Handle the select before the form's bubbling input listener. This keeps
  // the emitted preview payload aligned with the value shown in the control.
  formDto.forecastYears = Number(event.target.value)
  onInput()
}

function addIncomeChange() {
  const latest = sortedIncomeChanges.value.at(-1) || { effectiveMonth: '2026-11', amount: 0 }
  const [year, month] = latest.effectiveMonth.split('-').map(Number)
  const next = new Date(year, month, 1)
  incomeDraft.push({
    effectiveMonth: `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`,
    amount: latest.amount,
    isBaseline: false,
  })
  onInput()
}

function removeIncomeChange(change) {
  if (change.isBaseline) return
  const index = incomeDraft.indexOf(change)
  if (index >= 0) incomeDraft.splice(index, 1)
  onInput()
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
