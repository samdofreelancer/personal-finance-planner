<template>
  <section class="position-panel journey-panel" aria-label="Hành trình tiết kiệm và trả nợ dự kiến">
    <div class="journey-summary">
      <div class="journey-milestone">
        <span class="journey-kicker">MỐC QUAN TRỌNG</span>
        <strong>{{ payoffMonth ? `Dự kiến tất toán ${payoffMonth.label}` : remainingDebt === 0 ? 'Không còn dư nợ trong kỳ dự báo' : 'Chưa tất toán hết khoản vay' }}</strong>
        <span class="position-note">{{ remainingDebt === 0 ? 'Sau mốc này, dòng tiền trả nợ có thể chuyển sang tích lũy.' : 'Thời điểm tất toán phụ thuộc vào giả định trả nợ hiện tại.' }}</span>
      </div>
      <span class="journey-simulated"><span></span> KẾT QUẢ MÔ PHỎNG</span>
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
      <div class="trend-heading"><div><h3>Quỹ tiết kiệm dự kiến · dư nợ còn lại</h3><span>Số dư dự kiến tại cuối mỗi năm · VND</span></div></div>
      <div class="journey-legend"><span><i class="legend-savings"></i> Tiết kiệm lũy kế</span><span><i class="legend-debt"></i> Dư nợ còn lại</span></div>
      <svg viewBox="0 0 720 190" role="img" aria-label="Biểu đồ diễn biến tiết kiệm và dư nợ dự kiến theo năm">
        <line x1="38" y1="145" x2="690" y2="145" stroke="#e6e9df" />
        <polyline :points="trendPoints" fill="none" stroke="#25866a" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" />
        <polyline v-if="debtTrend.length" :points="debtTrendPoints" fill="none" stroke="#c38b3d" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="6 5" />
        <g v-for="(point, index) in trend" :key="point.year">
          <circle :cx="point.x" :cy="point.y" r="5" fill="#25866a" />
          <circle v-if="debtTrend[index]" :cx="debtTrend[index].x" :cy="debtTrend[index].y" r="4" fill="#c38b3d" />
          <text :x="point.x" :y="point.y - 12" text-anchor="middle" fill="#536b60" font-size="11">{{ compact(point.value) }}</text>
          <text :x="point.x" y="171" text-anchor="middle" fill="#819087" font-size="11">{{ point.year }}{{ point.partial ? '*' : '' }}</text>
        </g>
      </svg>
      <p class="chart-footnote">Hai đường dùng cùng thang hiển thị tương đối để thể hiện xu hướng, không so sánh trực tiếp quy mô. * Năm 2026 chỉ gồm tháng 11–12.</p>
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
  if (!Number.isFinite(lastMonth.value.openingPrincipal) || !Number.isFinite(lastMonth.value.principalPaid)) return 0
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
const debtTrend = computed(() => {
  const rows = annual.value
  if (!rows.length || !months.value.length) return []
  const values = rows.map((row) => {
    const last = months.value.filter((month) => month.year === row.year).at(-1)
    return last ? Math.max(0, last.openingPrincipal - last.principalPaid) : null
  })
  const available = values.filter((value) => value != null)
  if (!available.length) return []
  const max = Math.max(1, ...available)
  return values.map((value, index) => value == null ? null : ({
    x: rows.length === 1 ? 364 : 38 + index * 652 / (rows.length - 1),
    y: 135 - value / max * 100,
  }))
})
const trendPoints = computed(() => trend.value.map((point) => `${point.x},${point.y}`).join(' '))
const debtTrendPoints = computed(() => debtTrend.value.filter(Boolean).map((point) => `${point.x},${point.y}`).join(' '))
function compact(value) {
  return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(value / 1_000_000)} tr`
}
</script>
