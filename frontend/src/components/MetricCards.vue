<template>
  <section class="metrics">
    <article class="metric-card">
      <div class="metric-label">DÒNG TIỀN DƯ · 2030</div>
      <div class="metric-value">{{ formatMoney(metrics.netCashFlow2030) }}</div>
      <div class="metric-foot">Tổng thu nhập trừ tổng chi trong năm</div>
    </article>
    <article class="metric-card metric-card-accent">
      <div class="metric-label">TIẾT KIỆM CUỐI KỲ</div>
      <div class="metric-value">{{ formatMoney(metrics.savingsEnd) }}</div>
      <div class="metric-foot" :class="{positive: !provisional}">{{ savingsFoot }}</div>
    </article>
    <article class="metric-card">
      <div class="metric-label">LƯƠNG CƠ BẢN · 2030</div>
      <div class="metric-value">{{ formatMoney(metrics.salary2030) }}</div>
      <div class="metric-foot">Trước hệ số lương riêng tháng 1</div>
    </article>
  </section>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps(['metrics', 'provisional'])
const savingsFoot = computed(() => props.provisional ? 'Tạm tính · chưa gồm lãi tất toán chưa xác nhận' : 'Dự kiến đến tháng 12/2030')
function formatMoney(v) { return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(v ?? 0) + ' ₫' }
</script>