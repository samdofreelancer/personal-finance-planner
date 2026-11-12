<template>
  <section class="panel annual-panel">
    <div class="panel-heading">
      <div><p class="eyebrow">ĐỐI CHIẾU</p><h2>Tổng hợp theo năm</h2></div>
    </div>
    <div class="table-scroll">
      <table id="annual-table">
        <caption class="sr-only">Tổng thu nhập, tổng chi, dòng tiền ròng và tiết kiệm cuối năm theo từng năm</caption>
        <thead>
          <tr><th scope="col">Năm</th><th scope="col">Tổng thu nhập</th><th scope="col">Tổng chi</th><th scope="col">Dòng tiền ròng</th><th scope="col">Tiết kiệm cuối năm</th></tr>
        </thead>
        <tbody>
          <tr v-for="y in forecast?.annual || []" :key="y.year">
            <td><strong>{{ y.year }}</strong><span v-if="y.partial" class="partial-tag">2 tháng</span></td>
            <td>{{ money(y.salary) }}</td>
            <td>{{ money(y.expenses) }}</td>
            <td><span :class="signedClass(y.netCashFlow)">{{ money(y.netCashFlow) }}{{ y.provisional ? ' · tạm tính' : '' }}</span></td>
            <td><span class="savings-cell" :class="y.endingSavingsProvisional ? 'provisional' : signedClass(y.endingSavings)">{{ money(y.endingSavings) }}{{ y.endingSavingsProvisional ? ' · tạm tính' : '' }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
defineProps(['forecast'])
const formatter = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 })
function money(value) { return `${formatter.format(Math.round(value ?? 0))} ₫` }
function signedClass(value) { return value < 0 ? 'negative' : 'positive' }
</script>
