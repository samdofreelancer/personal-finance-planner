<template>
  <section class="panel monthly-panel">
    <div class="panel-heading">
      <div><p class="eyebrow">CHI TIẾT</p><h2>Dự báo theo tháng</h2></div>
      <span class="row-count">50 tháng</span>
    </div>
    <div class="table-scroll">
      <table id="monthly-table">
        <caption class="sr-only">Dự báo thu nhập, chi phí, khoản vay, dòng tiền ròng và tiết kiệm từng tháng</caption>
        <thead>
          <tr><th scope="col">Tháng</th><th scope="col">Lương</th><th scope="col">Sinh hoạt</th><th scope="col">Nuôi con</th><th scope="col">Tiền nhà</th><th scope="col">Gốc đầu kỳ</th><th scope="col">Gốc trả</th><th scope="col">Lãi trả</th><th scope="col">Tổng trả nợ</th><th scope="col">Tổng chi</th><th scope="col">Dòng tiền ròng</th><th scope="col">Tiết kiệm lũy kế</th></tr>
        </thead>
        <tbody>
          <tr v-for="m in forecast?.months || []" :key="m.label">
            <td scope="row">{{ m.label }}</td>
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
            <td class="savings-cell" :class="m.savingsProvisional ? 'provisional' : signedClass(m.cumulativeSavings)" :aria-label="m.savingsProvisional ? `${money(m.cumulativeSavings)}; tạm tính chưa gồm lãi tất toán` : null">{{ money(m.cumulativeSavings) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
const props = defineProps(['forecast', 'payoffInterest'])
const formatter = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 })
function money(value) { return `${formatter.format(Math.round(value ?? 0))} ₫` }
function signedClass(value) { return value < 0 ? 'negative' : 'positive' }
function interestText(m) {
  if (!m.provisional) return money(m.interestPaid)
  if (m.interestPaid === 0 && props.payoffInterest == null) return 'Chưa nhập · tạm tính'
  return `${money(m.interestPaid)} · tạm tính`
}
</script>
