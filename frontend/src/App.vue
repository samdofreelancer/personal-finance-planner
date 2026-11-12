<template>
  <div class="page">
    <header class="topbar">
      <a class="brand" href="#" @click.prevent>
        <span class="brand-mark">₫</span>
        <span>Plan<span class="brand-light">wise</span></span>
      </a>
      <div class="topbar-actions">
        <span class="local-badge">Dữ liệu lưu trên máy chủ</span>
        <button class="button button-secondary" @click="exportCsv">Xuất CSV</button>
      </div>
    </header>

    <main>
      <section class="page-heading">
        <div>
          <p class="eyebrow">KẾ HOẠCH CÁ NHÂN · 5 NĂM</p>
          <h1>Dòng tiền của bạn, rõ ràng từng tháng.</h1>
          <p class="subtitle">Điều chỉnh giả định để xem thu nhập, chi phí và khoản tiết kiệm thay đổi ra sao.</p>
        </div>
        <div class="heading-period"><span class="period-dot"></span> THÁNG 11/2026 — THÁNG 12/2030</div>
      </section>

      <section id="alert-area" v-if="alertMessage" role="alert" style="padding:0.75rem 1rem;background:#fef2f2;border:1px solid #fecaca;border-radius:6px;color:#991b1b;font-size:0.875rem;">{{ alertMessage }}</section>

      <metric-cards :metrics="metrics" :provisional="provisional" />
      <assumptions-form :dto="dto" @save="saveAndForecast" @reset="resetDefaults" :error="errorMessage" :status="saveStatus" />
      <annual-chart :forecast="forecast" />
      <annual-table :forecast="forecast" />
      <monthly-table :forecast="forecast" />

      <footer class="page-footer">
        <span>Đây là dự báo theo các giả định bạn nhập, không phải tư vấn tài chính.</span>
        <span>Chạy cục bộ · Gửi dữ liệu qua REST API</span>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAssumptions, saveAssumptions, seedDefaults, getForecast, exportCsv } from './services/api.js'
import MetricCards from './components/MetricCards.vue'
import AssumptionsForm from './components/AssumptionsForm.vue'
import AnnualChart from './components/AnnualChart.vue'
import AnnualTable from './components/AnnualTable.vue'
import MonthlyTable from './components/MonthlyTable.vue'

const dto = reactive({
  baseSalary: [REDACTED], januaryMultiplier: 2.5, living: [REDACTED],
  childThrough2026: [REDACTED], childQ12027: [REDACTED], childFromApr2027: [REDACTED],
  rentThroughJan2027: [REDACTED], rentFromFeb2027: [REDACTED],
  loanPrincipal: [REDACTED], fixedPayment: [REDACTED],
  novInterest: [REDACTED], decInterest: [REDACTED], payoffInterest: [REDACTED],
  payoffInterestConfirmed: false, startingSavings: [REDACTED],
  raises: { 2027: 0.1, 2028: 0.1, 2029: 0.1, 2030: 0.1 }
})

const forecast = ref(null)
const metrics = ref({ netCashFlow2030: 0, savingsEnd: 0, salary2030: 0 })
const errorMessage = ref('')
const saveStatus = ref('Đang tải...')
const alertMessage = ref('')
const provisional = ref(false)

onMounted(async () => {
  try {
    const data = await getAssumptions()
    Object.assign(dto, data)
    if (data.raises) dto.raises = { 2027: data.raises[2027], 2028: data.raises[2028], 2029: data.raises[2029], 2030: data.raises[2030] }
    await runForecast()
    saveStatus.value = 'Đã tải từ máy chủ'
  } catch (e) {
    errorMessage.value = e.message
    saveStatus.value = 'Lỗi kết nối máy chủ'
    await runForecast()
  }
})

async function runForecast() {
  try {
    const res = await getForecast(dto)
    forecast.value = res
    const y2030 = res.annual.find(a => a.year === 2030)
    metrics.value = {
      netCashFlow2030: y2030 ? y2030.netCashFlow : 0,
      savingsEnd: y2030 ? y2030.endingSavings : 0,
      salary2030: y2030 ? y2030.salary : 0
    }
    provisional.value = y2030 ? y2030.provisional : false
    alertMessage.value = dto.payoffInterest == null || !dto.payoffInterestConfirmed
      ? 'Lãi tất toán chưa được xác nhận. Dòng tiền tháng 01/2027 và số dư tiết kiệm sau đó đang tạm tính.'
      : ''
    saveStatus.value = 'Đã lưu trên máy chủ'
  } catch (e) {
    errorMessage.value = e.message
  }
}

async function saveAndForecast(updated) {
  Object.assign(dto, updated)
  try {
    await saveAssumptions(dto)
    await runForecast()
  } catch (e) {
    errorMessage.value = e.message
  }
}

async function resetDefaults() {
  if (!window.confirm('Khôi phục toàn bộ giả định về giá trị ban đầu từ workbook?')) return
  try {
    const data = await seedDefaults()
    Object.assign(dto, data)
    if (data.raises) dto.raises = { 2027: data.raises[2027], 2028: data.raises[2028], 2029: data.raises[2029], 2030: data.raises[2030] }
    await runForecast()
    saveStatus.value = 'Đã khôi phục mặc định'
  } catch (e) {
    errorMessage.value = e.message
  }
}

async function exportCsv() {
  try { await exportCsv(dto) } catch (e) { errorMessage.value = e.message }
}
</script>