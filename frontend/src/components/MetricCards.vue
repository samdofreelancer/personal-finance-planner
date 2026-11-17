<template>
  <section class="metrics" aria-label="Tổng quan kế hoạch">
    <article class="metric-card">
      <div class="metric-label">DÒNG TIỀN DƯ · 2030</div>
      <div id="metric-net" class="metric-value" :class="signedClass(metrics.netCashFlow2030)">{{ money(metrics.netCashFlow2030) }}</div>
      <div class="metric-foot">Tổng thu nhập trừ tổng chi trong năm</div>
    </article>
    <article class="metric-card metric-card-accent">
      <div class="metric-label">TIẾT KIỆM CUỐI KỲ</div>
      <div id="metric-savings" class="metric-value" :class="signedClass(metrics.savingsEnd)">{{ money(metrics.savingsEnd) }}</div>
      <div id="metric-savings-foot" class="metric-foot">{{ savingsFoot }}</div>
    </article>
    <article class="metric-card">
      <div class="metric-label">LƯƠNG CƠ BẢN · 2030</div>
      <div id="metric-salary" class="metric-value">{{ money(metrics.salary2030) }}</div>
      <div class="metric-foot">Trước hệ số lương riêng tháng 1</div>
    </article>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { formatCurrency } from '../utils/currency.js'
const props = defineProps(['metrics', 'provisional'])
const savingsFoot = computed(() => props.provisional ? 'Tạm tính · chưa gồm lãi tất toán chưa xác nhận' : 'Dự kiến đến tháng 12/2030')
const money = formatCurrency
function signedClass(value) { return value < 0 ? 'negative' : 'positive' }
</script>
