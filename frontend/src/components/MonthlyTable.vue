<template>
  <section class="panel monthly-panel">
    <div class="panel-heading">
      <div><p class="eyebrow">CHI TIẾT</p><h2>Dự báo theo tháng</h2></div>
          <span class="row-count">{{ rowCountLabel }}</span>
    </div>
    <div v-if="months.length" class="monthly-table-controls" aria-label="Bộ lọc bảng dự báo theo tháng">
      <div class="year-filter" role="group" aria-label="Lọc theo năm">
        <button v-for="year in years" :key="year" type="button" class="filter-chip" :class="{ active: selectedYear === year }" :aria-pressed="selectedYear === year" @click="selectedYear = year">{{ year === 'all' ? 'Tất cả' : year }}</button>
      </div>
      <button type="button" class="filter-chip loan-columns-toggle" :aria-pressed="showLoanColumns" @click="loanColumnsOverride = !showLoanColumns">
        {{ showLoanColumns ? 'Ẩn chi tiết khoản vay' : 'Hiện chi tiết khoản vay' }}
      </button>
    </div>
    <div v-if="months.length" class="table-scroll">
      <table id="monthly-table" :class="{ 'monthly-table-loans-visible': showLoanColumns }">
        <caption class="sr-only">Dự báo thu nhập, chi phí, khoản vay, dòng tiền ròng và tiết kiệm từng tháng</caption>
        <thead>
          <tr><th scope="col">Tháng</th><th scope="col">Lương</th><th scope="col">Chi phí định kỳ</th><th v-if="showLoanColumns" scope="col">Gốc đầu kỳ</th><th v-if="showLoanColumns" scope="col">Gốc trả</th><th v-if="showLoanColumns" scope="col">Lãi trả</th><th v-if="showLoanColumns" scope="col">Tổng trả nợ</th><th scope="col">Tổng chi</th><th scope="col">Dòng tiền ròng</th><th scope="col">Tiết kiệm lũy kế</th></tr>
        </thead>
        <tbody>
          <tr v-for="m in visibleMonths" :key="m.label">
            <th scope="row">{{ m.label }}</th>
            <td :class="{ 'zero-value': isZero(m.salary) }">{{ money(m.salary) }}</td>
            <td :title="expenseDetails(m)" :class="{ 'zero-value': isZero(m.recurringExpenses) }">{{ money(m.recurringExpenses) }}<span class="expense-breakdown-hint">{{ expenseSummary(m) }}</span></td>
            <td v-if="showLoanColumns" :class="{ 'zero-value': isZero(m.openingPrincipal) }">{{ money(m.openingPrincipal) }}</td>
            <td v-if="showLoanColumns" :class="{ 'zero-value': isZero(m.principalPaid) }">{{ money(m.principalPaid) }}</td>
            <td v-if="showLoanColumns" :class="[{provisional: m.provisional}, { 'zero-value': isZero(m.interestPaid) }]">{{ interestText(m) }}</td>
            <td v-if="showLoanColumns" :class="{ 'zero-value': isZero(m.loanPayment) }">{{ money(m.loanPayment) }}</td>
            <td :class="{ 'zero-value': isZero(m.totalExpenses) }">{{ money(m.totalExpenses) }}</td>
            <td class="net-cash-flow-cell" :class="[signedClass(m.netCashFlow), { 'zero-value': isZero(m.netCashFlow) }]">{{ money(m.netCashFlow) }}</td>
            <td class="savings-cell" :class="[m.savingsProvisional ? 'provisional' : signedClass(m.cumulativeSavings), { 'zero-value': isZero(m.cumulativeSavings) }]" :aria-label="savingsAriaLabel(m)">{{ money(m.cumulativeSavings) }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr class="monthly-total-row">
            <th scope="row">Tổng cộng kỳ <span>{{ periodLabel }}</span></th>
            <td :class="{ 'zero-value': isZero(totals.salary) }">{{ money(totals.salary) }}</td>
            <td :class="{ 'zero-value': isZero(totals.recurringExpenses) }">{{ money(totals.recurringExpenses) }}</td>
            <td v-if="showLoanColumns">—</td>
            <td v-if="showLoanColumns" :class="{ 'zero-value': isZero(totals.principalPaid) }">{{ money(totals.principalPaid) }}</td>
            <td v-if="showLoanColumns" :class="{ 'zero-value': isZero(totals.interestPaid) }">{{ money(totals.interestPaid) }}</td>
            <td v-if="showLoanColumns" :class="{ 'zero-value': isZero(totals.loanPayment) }">{{ money(totals.loanPayment) }}</td>
            <td :class="{ 'zero-value': isZero(totals.totalExpenses) }">{{ money(totals.totalExpenses) }}</td>
            <td class="net-cash-flow-cell" :class="[signedClass(totals.netCashFlow), { 'zero-value': isZero(totals.netCashFlow) }]">{{ money(totals.netCashFlow) }}</td>
            <td class="savings-cell" :class="[signedClass(totals.endingSavings), { 'zero-value': isZero(totals.endingSavings) }]">{{ money(totals.endingSavings) }}</td>
          </tr>
        </tfoot>
      </table>
    </div>
    <div v-else class="empty-state" role="status">Chưa có dữ liệu dự báo theo tháng.</div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { formatCurrency } from '../utils/currency.js'

const props = defineProps(['forecast'])
const months = computed(() => props.forecast?.months || [])
const selectedYear = ref('all')
const loanColumnsOverride = ref(null)
const years = computed(() => ['all', ...new Set(months.value.map((month) => String(month.year)))])
watch(years, (availableYears) => {
  if (!availableYears.includes(selectedYear.value)) selectedYear.value = 'all'
})
const visibleMonths = computed(() => selectedYear.value === 'all'
  ? months.value
  : months.value.filter((month) => String(month.year) === selectedYear.value))
const hasLoanActivity = computed(() => months.value.some((month) =>
  ['openingPrincipal', 'principalPaid', 'interestPaid', 'loanPayment'].some((key) => Number(month[key] || 0) !== 0),
))
const showLoanColumns = computed(() => loanColumnsOverride.value ?? hasLoanActivity.value)
const rowCountLabel = computed(() => selectedYear.value === 'all'
  ? `${visibleMonths.value.length} tháng`
  : `${visibleMonths.value.length} / ${months.value.length} tháng`)
const sum = (key) => visibleMonths.value.reduce((total, month) => total + Number(month[key] || 0), 0)
const totals = computed(() => ({
  salary: sum('salary'),
  recurringExpenses: sum('recurringExpenses'),
  principalPaid: sum('principalPaid'),
  interestPaid: sum('interestPaid'),
  loanPayment: sum('loanPayment'),
  totalExpenses: sum('totalExpenses'),
  netCashFlow: sum('netCashFlow'),
  endingSavings: Number(visibleMonths.value.at(-1)?.cumulativeSavings || 0),
}))
const periodLabel = computed(() => {
  if (!visibleMonths.value.length) return ''
  const format = (month) => `${String(month.month).padStart(2, '0')}/${month.year}`
  return `(${format(visibleMonths.value[0])}–${format(visibleMonths.value.at(-1))})`
})
const money = formatCurrency
function signedClass(value) { return value < 0 ? 'negative' : 'positive' }
function isZero(value) { return Number(value) === 0 }
function expenseDetails(month) {
  return (month.expenseBreakdown || []).map((item) => `${item.name}: ${money(item.amount)}`).join('\n')
}
function expenseSummary(month) {
  return (month.expenseBreakdown || []).map((item) => `${item.name}: ${money(item.amount)}`).join(' · ')
}
function savingsAriaLabel(m) {
  if (!m.savingsProvisional) return null
  return `${money(m.cumulativeSavings)}; tạm tính`
}
function interestText(m) {
  if (!m.provisional) return money(m.interestPaid)
  return `${money(m.interestPaid)} · tạm tính`
}
</script>
