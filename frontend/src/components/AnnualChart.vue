<template>
  <section class="panel chart-panel">
    <div class="panel-heading chart-heading">
      <div><p class="eyebrow">TỔNG HỢP NĂM</p><h2>Dòng tiền dư mỗi năm</h2></div>
      <span class="chart-unit">Đơn vị: triệu đồng</span>
    </div>
    <div id="annual-chart" class="chart-container">
      <svg :viewBox="`0 0 ${chart.width} ${chart.height}`" role="img" aria-labelledby="chart-title chart-desc">
        <title id="chart-title">Dòng tiền ròng theo năm, từ 2026 đến 2030</title>
        <desc id="chart-desc">{{ chart.description }}</desc>
        <g v-for="(tick, index) in chart.ticks" :key="'tick-' + index">
          <line :x1="chart.left" :y1="tick.y" :x2="chart.width - chart.right" :y2="tick.y" :stroke="tick.strong ? '#aebac7' : '#edf1f5'" :stroke-width="tick.strong ? 1.5 : 1" :stroke-dasharray="tick.strong ? '4 4' : undefined" />
          <text :x="chart.left - 10" :y="tick.y + 4" text-anchor="end" fill="#8492a1" font-size="10">{{ tick.label }}</text>
        </g>
        <g v-for="bar in chart.bars" :key="bar.year" role="img" :aria-label="bar.aria">
          <title>{{ bar.aria }}</title>
          <rect class="chart-bar" :x="bar.x" :y="bar.y" :width="bar.width" :height="bar.height" rx="5" :fill="bar.color" />
          <text :x="bar.center" :y="bar.labelY" text-anchor="middle" :fill="bar.color" font-size="10" font-weight="700">{{ bar.label }}</text>
          <text :x="bar.center" :y="chart.height - 13" text-anchor="middle" fill="#5f7184" font-size="11" font-weight="650">{{ bar.year }}{{ bar.partial ? '*' : '' }}</text>
        </g>
        <text :x="chart.left" :y="chart.height - 1" fill="#91a0ad" font-size="9">* Năm 2026 gồm tháng 11–12 · † Dòng tiền tạm tính</text>
      </svg>
    </div>
    <div class="chart-footnote">Dòng tiền ròng = tổng thu nhập − tổng chi phí. Năm 2026 chỉ gồm tháng 11 và 12.</div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps(['forecast'])

const chart = computed(() => {
  const annual = props.forecast?.annual || []
  const width = 900
  const height = 260
  const left = 82
  const right = 22
  const top = 23
  const bottom = 38
  const plotHeight = height - top - bottom
  const plotWidth = width - left - right
  const values = annual.map((row) => row.netCashFlow)
  const min = Math.min(0, ...values)
  const max = Math.max(0, ...values)
  const span = max - min || 1
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
    const labelY = row.netCashFlow < 0 ? rectY + rectHeight + 15 : rectY - 7
    const aria = `${row.year}${row.partial ? ', năm chỉ gồm hai tháng' : ''}: ${money(row.netCashFlow)} dòng tiền ròng${row.provisional ? ', tạm tính do lãi tất toán chưa xác nhận' : ''}`
    return {
      year: row.year,
      partial: row.partial,
      center,
      x: center - barWidth / 2,
      y: rectY,
      width: barWidth,
      height: rectHeight,
      color,
      labelY,
      label: compactMoney(row.netCashFlow) + (row.provisional ? '†' : ''),
      aria,
    }
  })
  const description = annual
    .map((row) => `${row.year}${row.partial ? ' (hai tháng)' : ''}: ${money(row.netCashFlow)}${row.provisional ? ' (tạm tính)' : ''}`)
    .join('; ')
  return { width, height, left, right, top, bottom, ticks, bars, description }
})

const formatter = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 })

function money(value) {
  return `${formatter.format(Math.round(value))} ₫`
}

function compactMoney(value) {
  const millions = value / 1_000_000
  return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(millions)} tr`
}
</script>
