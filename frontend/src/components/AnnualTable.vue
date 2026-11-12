<template>
  <section class="panel annual-panel">
    <div class="panel-heading">
      <div><p class="eyebrow">ĐỐI CHIẾU</p><h2>Tổng hợp theo năm</h2></div>
    </div>
    <div class="table-scroll">
      <table>
        <thead>
          <tr><th>Năm</th><th>Tổng thu nhập</th><th>Tổng chi</th><th>Dòng tiền ròng</th><th>Tiết kiệm cuối năm</th><th>Trạng thái</th></tr>
        </thead>
        <tbody>
          <tr v-for="y in forecast?.annual || []" :key="y.year" :class="{provisional: y.provisional}">
            <td>{{ y.year }}{{ y.partial ? ' (partial)' : '' }}</td>
            <td>{{ formatMoney(y.salary) }}</td>
            <td>{{ formatMoney(y.expenses) }}</td>
            <td :class="{positive: y.netCashFlow >= 0, negative: y.netCashFlow < 0}">{{ formatMoney(y.netCashFlow) }}</td>
            <td>{{ formatMoney(y.endingSavings) }}</td>
            <td>{{ y.provisional ? 'Có mục tạm tính' : 'Đầy đủ' }}</td>
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