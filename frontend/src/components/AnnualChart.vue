<template>
  <section class="panel chart-panel">
    <div class="panel-heading chart-heading">
      <div><p class="eyebrow">TỔNG HỢP NĂM</p><h2>Dòng tiền dư mỗi năm</h2></div>
      <span class="chart-unit">Đơn vị: triệu đồng</span>
    </div>
    <div class="chart-container">
      <svg width="700" height="300" viewBox="0 0 700 300" role="img" aria-label="Biểu đồ dòng tiền theo năm">
        <polyline fill="none" stroke="#9ca3af" stroke-width="1" points="0,150 700,150" />
        <g v-for="(y, i) in forecast?.annual || []" :key="y.year">
          <rect :x="50 + i * 120" :y="barY(y)" :width="90" :height="barHeight(y)" fill="#3b82f6" :class="{provisional: y.provisional}" />
          <text :x="50 + i * 120 + 45" y="280" text-anchor="middle" font-size="12" fill="#374151">{{ y.year }}</text>
          <text :x="50 + i * 120 + 45" :y="barY(y) - 6" text-anchor="middle" font-size="11" fill="#111827" font-weight="600">{{ formatMoneyShort(y.netCashFlow) }}</text>
        </g>
      </svg>
    </div>
    <div class="chart-footnote">Dòng tiền ròng = tổng thu nhập − tổng chi phí. Năm 2026 chỉ gồm tháng 11 và 12.</div>
  </section>
</template>

<script setup>
defineProps(['forecast'])
function barY(y) { return y.netCashFlow >= 0 ? 150 - y.netCashFlow / 100000 : 150 - y.netCashFlow / 100000 }
function barHeight(y) { return Math.abs(y.netCashFlow) / 100000 }
function formatMoneyShort(v) {
  const m = (v ?? 0) / 1_000_000
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(m) + ' tr'
}
</script>