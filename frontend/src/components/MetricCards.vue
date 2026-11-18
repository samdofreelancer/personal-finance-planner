<template>
  <section class="metrics" aria-label="Tổng quan kế hoạch">
    <article class="metric-card">
      <div class="metric-label">TỔNG THU NHẬP · 2030</div>
      <div id="metric-income" class="metric-value">{{ money(metrics.income2030) }}</div>
      <div class="metric-foot">Tổng thu nhập trong năm</div>
    </article>
    <article class="metric-card">
      <div class="metric-label">TỔNG CHI PHÍ · 2030</div>
      <div id="metric-expenses" class="metric-value">{{ money(metrics.expenses2030) }}</div>
      <div class="metric-foot">Bao gồm sinh hoạt và trả nợ</div>
    </article>
    <article class="metric-card metric-card-accent">
      <div class="metric-label">DÒNG TIỀN DƯ · 2030</div>
      <div id="metric-net" class="metric-value" :class="signedClass(metrics.netCashFlow2030)">{{ money(metrics.netCashFlow2030) }}</div>
      <div id="metric-net-foot" class="metric-foot">{{ provisional ? 'Tạm tính theo giả định hiện tại' : 'Thu nhập trừ tổng chi phí trong năm' }}</div>
    </article>
  </section>
</template>

<script setup>
import { formatCurrency } from '../utils/currency.js'
const props = defineProps(['metrics', 'provisional'])
const money = formatCurrency
function signedClass(value) { return value < 0 ? 'negative' : 'positive' }
</script>
