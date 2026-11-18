<template>
  <section class="position-panel" aria-label="Tiết kiệm và dư nợ dự kiến">
    <div class="section-intro position-heading">
      <div><p class="eyebrow">VỊ THẾ TÀI CHÍNH</p><h2>Tiết kiệm &amp; dư nợ</h2><p class="section-description">Các số dư được mô phỏng từ giả định hiện tại, không phải số dư tài khoản thực tế.</p></div>
    </div>
    <div class="position-grid">
      <article class="position-card">
        <span class="position-label">TIẾT KIỆM BAN ĐẦU</span>
        <strong>{{ money(startingSavings) }}</strong>
        <span class="position-note">Số tiền dùng làm điểm xuất phát</span>
      </article>
      <article class="position-card">
        <span class="position-label">DƯ NỢ BAN ĐẦU</span>
        <strong>{{ money(startingDebt) }}</strong>
        <span class="position-note">Theo giả định khoản vay</span>
      </article>
      <article class="position-card" :class="{ 'position-card-good': remainingDebt === 0 }">
        <span class="position-label">DƯ NỢ CUỐI KỲ</span>
        <strong>{{ money(remainingDebt) }}</strong>
        <span class="position-note">{{ debtNote }}</span>
      </article>
      <article class="position-card position-card-highlight">
        <span class="position-label">TIẾT KIỆM CUỐI KỲ</span>
        <strong :class="remainingSavings < 0 ? 'negative' : 'positive'">{{ money(remainingSavings) }}</strong>
        <span class="position-note">{{ savingsNote }}</span>
      </article>
    </div>
    <div class="savings-trend" v-if="annual.length">
      <div class="trend-heading"><div><h3>Quỹ tiết kiệm dự kiến</h3><span>Theo từng năm · VND</span></div><strong :class="remainingSavings < 0 ? 'negative' : 'positive'">{{ money(remainingSavings) }}</strong></div>
      <svg viewBox="0 0 720 190" role="img" aria-label="Biểu đồ tiết kiệm lũy kế theo năm">
        <line x1="38" y1="145" x2="690" y2="145" stroke="#e8edf2" />
        <polyline :points="trendPoints" fill="none" stroke="#168d7a" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
        <g v-for="point in trend" :key="point.year">
          <circle :cx="point.x" :cy="point.y" r="5" fill="#168d7a" />
          <text :x="point.x" :y="point.y - 12" text-anchor="middle" fill="#53677c" font-size="11">{{ compact(point.value) }}</text>
          <text :x="point.x" y="171" text-anchor="middle" fill="#8190a0" font-size="11">{{ point.year }}{{ point.partial ? '*' : '' }}</text>
        </g>
      </svg>
      <p class="chart-footnote">* Năm 2026 chỉ gồm tháng 11–12. Giá trị có thể mang dấu âm nếu dự báo thiếu hụt tiền mặt.</p>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { formatCurrency } from '../utils/currency.js'

const props = defineProps(['assumptions', 'forecast'])
const money = formatCurrency
const annual = computed(() => props.forecast?.annual || [])
const months = computed(() => props.forecast?.months || [])
const startingSavings = computed(() => props.assumptions?.startingSavings || 0)
const startingDebt = computed(() => props.assumptions?.loanPrincipal || 0)
const lastMonth = computed(() => months.value.at(-1))
const remainingDebt = computed(() => {
  if (!lastMonth.value) return startingDebt.value
  return Math.max(0, lastMonth.value.openingPrincipal - lastMonth.value.principalPaid)
})
const remainingSavings = computed(() => lastMonth.value?.cumulativeSavings ?? annual.value.at(-1)?.endingSavings ?? startingSavings.value)
const payoffMonth = computed(() => months.value.find((month) => month.openingPrincipal > 0 && month.principalPaid >= month.openingPrincipal))
const debtNote = computed(() => remainingDebt.value === 0
  ? (payoffMonth.value ? `Dự kiến tất toán ${payoffMonth.value.label}` : 'Không còn dư nợ trong dự báo')
  : 'Vẫn còn dư nợ ở cuối kỳ')
const savingsNote = computed(() => {
  const last = annual.value.at(-1)
  return last?.endingSavingsProvisional ? 'Tạm tính · cần xác minh lãi tất toán' : 'Dự kiến đến cuối kỳ kế hoạch'
})
const trend = computed(() => {
  const rows = annual.value
  if (!rows.length) return []
  const values = rows.map((row) => row.endingSavings)
  const min = Math.min(0, ...values)
  const max = Math.max(0, ...values)
  const span = max - min || 1
  return rows.map((row, index) => ({
    year: row.year, partial: row.partial, value: row.endingSavings,
    x: rows.length === 1 ? 364 : 38 + index * 652 / (rows.length - 1),
    y: 135 - (row.endingSavings - min) / span * 100,
  }))
})
const trendPoints = computed(() => trend.value.map((point) => `${point.x},${point.y}`).join(' '))
function compact(value) {
  return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(value / 1_000_000)} tr`
}
</script>
