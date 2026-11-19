<template>
  <section class="panel monthly-panel">
    <div class="panel-heading">
      <div><p class="eyebrow">CHI TIẾT</p><h2>Dự báo theo tháng</h2></div>
          <span class="row-count">{{ months.length }} tháng</span>
    </div>
    <div v-if="months.length" class="table-scroll">
      <table id="monthly-table">
        <caption class="sr-only">Dự báo thu nhập, chi phí, khoản vay, dòng tiền ròng và tiết kiệm từng tháng</caption>
        <thead>
          <tr><th scope="col">Tháng</th><th scope="col">Lương</th><th scope="col">Sinh hoạt</th><th scope="col">Nuôi con</th><th scope="col">Tiền nhà</th><th scope="col">Gốc đầu kỳ</th><th scope="col">Gốc trả</th><th scope="col">Lãi trả</th><th scope="col">Tổng trả nợ</th><th scope="col">Tổng chi</th><th scope="col">Dòng tiền ròng</th><th scope="col">Tiết kiệm lũy kế</th></tr>
        </thead>
        <tbody>
          <tr v-for="m in months" :key="m.label">
            <th scope="row">{{ m.label }}</th>
            <td>{{ money(m.salary) }}</td>
            <td>{{ money(m.living) }}</td>
            <td>{{ money(m.child) }}</td>
            <td>{{ money(m.rent) }}</td>
            <td>{{ money(m.openingPrincipal) }}</td>
            <td>{{ money(m.principalPaid) }}</td>
            <td :class="{provisional: m.provisional}">{{ interestText(m) }}</td>
            <td>{{ money(m.loanPayment) }}</td>
            <td>{{ money(m.totalExpenses) }}</td>
            <td :class="signedClass(m.netCashFlow)">{{ money(m.netCashFlow) }}</td>
            <td class="savings-cell" :class="m.savingsProvisional ? 'provisional' : signedClass(m.cumulativeSavings)" :aria-label="savingsAriaLabel(m)">{{ money(m.cumulativeSavings) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="empty-state" role="status">Chưa có dữ liệu dự báo theo tháng.</div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { formatCurrency } from '../utils/currency.js'

const props = defineProps(['forecast'])
const months = computed(() => props.forecast?.months || [])
const money = formatCurrency
function signedClass(value) { return value < 0 ? 'negative' : 'positive' }
function savingsAriaLabel(m) {
  if (!m.savingsProvisional) return null
  return `${money(m.cumulativeSavings)}; tạm tính`
}
function interestText(m) {
  if (!m.provisional) return money(m.interestPaid)
  return `${money(m.interestPaid)} · tạm tính`
}
</script>
