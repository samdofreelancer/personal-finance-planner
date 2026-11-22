<template>
  <section class="panel chart-panel">
    <div class="panel-heading chart-heading">
      <div><p class="eyebrow">TỔNG HỢP NĂM</p><h2>Dòng tiền ròng theo năm</h2></div>
      <span class="chart-unit">Đơn vị: triệu VND</span>
    </div>
    <div v-if="chart.bars.length" id="annual-chart" class="chart-container">
      <svg :viewBox="`0 0 ${chart.width} ${chart.height}`" role="img" aria-labelledby="chart-title chart-desc">
        <title id="chart-title">Dòng tiền ròng theo năm, từ 2026 đến {{ chart.endYear }}</title>
        <desc id="chart-desc">{{ chart.description }}</desc>
        <g v-for="(tick, index) in chart.ticks" :key="'tick-' + index">
          <line :x1="chart.left" :y1="tick.y" :x2="chart.width - chart.right" :y2="tick.y" :stroke="tick.strong ? '#aebac7' : '#edf1f5'" :stroke-width="tick.strong ? 1.5 : 1" :stroke-dasharray="tick.strong ? '4 4' : undefined" />
          <text :x="chart.left - 10" :y="tick.y + 4" text-anchor="end" fill="#586b60" font-size="10">{{ tick.label }}</text>
        </g>
        <g v-for="bar in chart.bars" :key="bar.year" role="img" :aria-label="bar.aria">
          <title>{{ bar.aria }}</title>
          <rect class="chart-bar" :x="bar.x" :y="bar.y" :width="bar.width" :height="bar.height" rx="5" :fill="bar.color" />
          <text :x="bar.center" :y="bar.labelY" text-anchor="middle" :fill="bar.isZero ? '#829187' : bar.color" font-size="11" font-weight="700">{{ bar.label }}</text>
          <text :x="bar.center" :y="chart.height - 13" text-anchor="middle" fill="#40594c" font-size="11" font-weight="650">{{ bar.year }}{{ bar.partial ? '*' : '' }}</text>
        </g>
      </svg>
    </div>
    <div v-else class="empty-state" role="status">
      <strong>Chưa có dữ liệu biểu đồ</strong>
      <span>Dữ liệu dòng tiền theo năm sẽ xuất hiện sau khi tải được dự báo.</span>
    </div>
    <p v-if="chart.allZero" class="chart-zero-hint">Các năm hiện đều ở mức 0 tr ₫. Hãy nạp dữ liệu mẫu hoặc nhập giả định để xem dòng tiền dự kiến.</p>
    <div v-if="chart.footnotes.length" class="chart-footnote">{{ chart.footnotes.join(' · ') }}</div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { formatCurrency, formatCompactCurrency } from '../utils/currency.js'

const props = defineProps(['forecast'])
const viewportWidth = ref(typeof window === 'undefined' ? 1024 : window.innerWidth)
const updateViewportWidth = () => { viewportWidth.value = window.innerWidth }

onMounted(() => window.addEventListener('resize', updateViewportWidth, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('resize', updateViewportWidth))

const chart = computed(() => {
  const annual = props.forecast?.annual || []
  const compact = viewportWidth.value < 768
  const width = compact ? 420 : 900
  const height = 270
  const left = compact ? 68 : 82
  const right = compact ? 16 : 22
  const top = 48
  const bottom = 38
  const plotHeight = height - top - bottom
  const plotWidth = width - left - right
  const values = annual.map((row) => row.netCashFlow)
  const allZero = values.length > 0 && values.every((value) => Number(value) === 0)
  let min = Math.min(0, ...values)
  let max = Math.max(0, ...values)
  if (values.length && min === 0 && max === 0) {
    min = -1_000_000
    max = 1_000_000
  }
  // Reserve vertical breathing room so value labels stay outside the bars,
  // including when a bar reaches the data minimum or maximum.
  const dataSpan = max - min || 1
  min -= dataSpan * 0.18
  max += dataSpan * 0.18
  const span = max - min
  const y = (value) => top + (max - value) / span * plotHeight
  const zeroY = y(0)
  const ticks = []
  for (let index = 0; index <= 4; index += 1) {
    const value = min + span * index / 4
    ticks.push({
      y: y(value),
      strong: value === 0,
      label: compactMoney(value),
    })
  }
  const band = annual.length ? plotWidth / annual.length : 0
  const barWidth = Math.min(70, band * 0.48)
  const bars = annual.map((row, index) => {
    const center = left + band * (index + 0.5)
    const valueY = y(row.netCashFlow)
    const rectY = Math.min(zeroY, valueY)
    const rectHeight = Math.max(2, Math.abs(zeroY - valueY))
    const color = row.netCashFlow < 0 ? '#cc6668' : '#3e83ca'
    const labelY = row.netCashFlow === 0
      ? zeroY - 12
      : row.netCashFlow < 0
      ? Math.min(height - bottom - 8, rectY + rectHeight + 16)
      : Math.max(top + 14, rectY - 9)
    const aria = `${row.year}${row.partial ? ', năm chỉ gồm hai tháng' : ''}: ${money(row.netCashFlow)} dòng tiền ròng${row.provisional ? ', kết quả tạm tính' : ''}`
    return {
      year: row.year,
      partial: row.partial,
      center,
      x: center - barWidth / 2,
      y: rectY,
      width: barWidth,
      height: rectHeight,
      color,
      isZero: row.netCashFlow === 0,
      labelY,
      label: compactMoney(row.netCashFlow) + (row.provisional ? '†' : ''),
      aria,
    }
  })
  const description = annual
    .map((row) => `${row.year}${row.partial ? ' (hai tháng)' : ''}: ${money(row.netCashFlow)}${row.provisional ? ' (tạm tính)' : ''}`)
    .join('; ')
  const footnotes = []
  if (annual.some((row) => row.partial)) footnotes.push('* Năm 2026 chỉ gồm tháng 11–12')
  if (annual.some((row) => row.provisional)) footnotes.push('† Có dòng tiền tạm tính')
  return { width, height, left, right, top, bottom, ticks, bars, description, allZero, footnotes, endYear: annual.at(-1)?.year ?? 2030 }
})

const money = formatCurrency

function compactMoney(value) {
  return formatCompactCurrency(value)
}
</script>
