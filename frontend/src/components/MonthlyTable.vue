<template>
  <section class="panel monthly-panel">
    <div class="panel-heading">
      <div><p class="eyebrow">CHI TIẾT</p><h2>Dự báo theo tháng</h2></div>
      <span class="row-count">50 tháng</span>
    </div>
    <div class="table-scroll">
      <table>
        <thead>
          <tr><th>Tháng</th><th>Lương</th><th>Sinh hoạt</th><th>Nuôi con</th><th>Tiền nhà</th><th>Gốc đầu kỳ</th><th>Gốc trả</th><th>Lãi trả</th><th>Tổng trả nợ</th><th>Tổng chi</th><th>Dòng tiền ròng</th><th>Tiết kiệm lũy kế</th><th>Trạng thái</th></tr>
        </thead>
        <tbody>
          <tr v-for="m in forecast?.months || []" :key="m.label" :class="{provisional: m.provisional}">
            <td>{{ m.label }}</td>
            <td>{{ formatMoney(m.salary) }}</td>
            <td>{{ formatMoney(m.living) }}</td>
            <td>{{ formatMoney(m.child) }}</td>
            <td>{{ formatMoney(m.rent) }}</td>
            <td>{{ formatMoney(m.openingPrincipal) }}</td>
            <td>{{ formatMoney(m.principalPaid) }}</td>
            <td>{{ formatMoney(m.interestPaid) }}</td>
            <td>{{ formatMoney(m.loanPayment) }}</td>
            <td>{{ formatMoney(m.totalExpenses) }}</td>
            <td :class="{positive: m.netCashFlow >= 0, negative: m.netCashFlow < 0}">{{ formatMoney(m.netCashFlow) }}</td>
            <td>{{ formatMoney(m.cumulativeSavings) }}</td>
            <td>{{ m.provisional ? 'Tạm tính' : 'Đầy đủ' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
defineProps(['forecast'])
function formatMoney(v) { return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(v ?? 0) + ' ₫' }
</script>