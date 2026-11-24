<template>
  <section class="metrics" aria-label="Tổng quan kế hoạch">
    <article class="metric-card">
      <div class="metric-label">TỔNG THU NHẬP · {{ year }}</div>
      <div id="metric-income" class="metric-value"><money-amount :value="metrics.income" /></div>
      <div class="metric-foot">Tổng thu nhập trong năm</div>
    </article>
    <article class="metric-card">
      <div class="metric-label">TỔNG CHI PHÍ · {{ year }}</div>
      <div id="metric-expenses" class="metric-value"><money-amount :value="metrics.expenses" /></div>
      <div class="metric-foot">Bao gồm sinh hoạt và trả nợ</div>
    </article>
    <article class="metric-card metric-card-accent">
      <div class="metric-label">DÒNG TIỀN DƯ · {{ year }}</div>
      <div id="metric-net" class="metric-value" :class="signedClass(metrics.netCashFlow)"><money-amount :value="metrics.netCashFlow" /></div>
      <div id="metric-net-foot" class="metric-foot">{{ provisional ? 'Tạm tính theo giả định hiện tại' : 'Thu nhập trừ tổng chi phí trong năm' }}</div>
    </article>
  </section>
</template>

<script setup>
import MoneyAmount from './MoneyAmount.vue'
defineProps(['metrics', 'provisional', 'year'])
function signedClass(value) { return value < 0 ? 'negative' : 'positive' }
</script>
