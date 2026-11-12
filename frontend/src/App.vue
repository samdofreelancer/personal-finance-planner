<template>
  <header class="topbar">
    <a class="brand" href="#" aria-label="Kế hoạch tài chính cá nhân" @click.prevent>
      <span class="brand-mark" aria-hidden="true">₫</span>
      <span>Plan<span class="brand-light">wise</span></span>
    </a>
    <div class="topbar-actions">
      <span class="local-badge"><span aria-hidden="true"></span> Dữ liệu lưu trên máy chủ</span>
      <button id="export-button" class="button button-secondary" type="button" :disabled="hasValidationError" @click="exportCsv">Xuất CSV</button>
    </div>
  </header>

  <main class="page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">KẾ HOẠCH CÁ NHÂN · 5 NĂM</p>
        <h1>Dòng tiền của bạn,<br class="mobile-break"> rõ ràng từng tháng.</h1>
        <p class="subtitle">Điều chỉnh giả định để xem thu nhập, chi phí và khoản tiết kiệm thay đổi ra sao.</p>
      </div>
      <div class="heading-period"><span class="period-dot"></span> THÁNG 11/2026 — THÁNG 12/2030</div>
    </section>

    <section id="alert-area" aria-live="polite">
      <div v-if="alert" class="alert">
        <span class="alert-icon" aria-hidden="true">!</span>
        <span><strong v-if="alert.title">{{ alert.title }}</strong> {{ alert.text }}{{ alert.suffix ? ' ' + alert.suffix : '' }}</span>
      </div>
    </section>

    <metric-cards :metrics="metrics" :provisional="provisional" />
    <assumptions-form :dto="dto" :error="errorMessage" :status="saveStatus" @input="onFormInput" @save="saveAndForecast" @reset="resetDefaults" />
    <annual-chart :forecast="forecast" />
    <annual-table :forecast="forecast" />
    <monthly-table :forecast="forecast" :payoff-interest="dto.payoffInterest" />

    <footer class="page-footer">
      <span>Đây là dự báo theo các giả định bạn nhập, không phải tư vấn tài chính.</span>
      <span>Chạy cục bộ · Không gửi dữ liệu ra ngoài</span>
    </footer>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getAssumptions, saveAssumptions, seedDefaults, getForecast, exportCsv as exportCsvApi } from './services/api.js'
import { validateAssumptions } from './utils/validation.js'
import MetricCards from './components/MetricCards.vue'
import AssumptionsForm from './components/AssumptionsForm.vue'
import AnnualChart from './components/AnnualChart.vue'
import AnnualTable from './components/AnnualTable.vue'
import MonthlyTable from './components/MonthlyTable.vue'

const YEARS = [2027, 2028, 2029, 2030]
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
const provisional = ref(false)

let debounceTimer = null
let latestRequest = 0

const hasValidationError = computed(() => validateAssumptions(dto).length > 0)

const alert = computed(() => {
  if (dto.payoffInterest == null || !dto.payoffInterestConfirmed) {
    return {
      title: 'Lãi tất toán chưa được xác nhận.',
      text: dto.payoffInterest == null
        ? 'Chưa nhập lãi tất toán ngân hàng. Dòng tiền tháng 01/2027 và số dư tiết kiệm sau đó đang tạm tính chưa gồm khoản lãi này.'
        : `Lãi tất toán ${money(dto.payoffInterest)} đang lấy từ workbook nhưng chưa được đánh dấu đã đối chiếu với ngân hàng. Dòng tiền tháng 01/2027 và số dư tiết kiệm sau đó vẫn là tạm tính.`,
      suffix: 'Chỉ đánh dấu đã đối chiếu sau khi xác nhận với ngân hàng.',
    }
  }
  if (forecast.value?.annual?.some((row) => row.provisional)) {
    return {
      title: '',
      text: 'Một hoặc nhiều kết quả đang là tạm tính vì thiếu dữ liệu lãi tất toán.',
      suffix: '',
    }
  }
  return null
})

onMounted(async () => {
  try {
    const data = await getAssumptions()
    applyDto(data)
    await loadForecast()
    errorMessage.value = ''
    saveStatus.value = 'Đã lưu trên máy chủ'
  } catch (e) {
    errorMessage.value = e.message
    saveStatus.value = 'Lỗi kết nối máy chủ'
  }
})

onBeforeUnmount(() => clearTimeout(debounceTimer))

function applyDto(data) {
  Object.assign(dto, data)
  dto.raises = {}
  for (const year of YEARS) {
    dto.raises[year] = Number(data.raises?.[year] ?? 0.1)
  }
  if (dto.payoffInterestConfirmed == null) dto.payoffInterestConfirmed = false
  if (dto.payoffInterest === undefined) dto.payoffInterest = null
}

function snapshot() {
  return JSON.parse(JSON.stringify(dto))
}

async function loadForecast(source = dto) {
  applyForecast(await getForecast(source))
}

function applyForecast(res) {
  forecast.value = res
  const y2030 = res.annual.find((a) => a.year === 2030)
  const dec2030 = res.months.find((m) => m.year === 2030 && m.month === 12)
  metrics.value = {
    netCashFlow2030: y2030 ? y2030.netCashFlow : 0,
    savingsEnd: y2030 ? y2030.endingSavings : 0,
    salary2030: dec2030 ? dec2030.salary : 0,
  }
  provisional.value = y2030 ? (y2030.provisional || y2030.endingSavingsProvisional) : false
}

function onFormInput(updated) {
  Object.assign(dto, updated)
  queueUpdate()
}

function queueUpdate() {
  const requestId = ++latestRequest
  const errors = validateAssumptions(dto)
  if (errors.length) {
    clearTimeout(debounceTimer)
    errorMessage.value = errors[0]
    saveStatus.value = 'Kết quả chưa cập nhật do giả định chưa hợp lệ'
    return
  }
  errorMessage.value = ''
  saveStatus.value = 'Đang lưu...'
  const payload = snapshot()
  previewForecast(payload, requestId)
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => persistAndForecast(payload, requestId), 250)
}

async function previewForecast(payload, requestId) {
  try {
    const res = await getForecast(payload)
    if (requestId === latestRequest) applyForecast(res)
  } catch {
    // The debounced save below will surface any server-side error.
  }
}

async function persistAndForecast(payload = snapshot(), requestId = ++latestRequest) {
  try {
    await saveAssumptions(payload)
    if (requestId !== latestRequest) return
    const res = await getForecast(payload)
    if (requestId !== latestRequest) return
    applyForecast(res)
    errorMessage.value = ''
    saveStatus.value = 'Đã lưu trên máy chủ'
  } catch (e) {
    if (requestId !== latestRequest) return
    errorMessage.value = e.message
    saveStatus.value = 'Không lưu được giả định'
  }
}

async function saveAndForecast(updated) {
  Object.assign(dto, updated)
  const errors = validateAssumptions(dto)
  if (errors.length) {
    errorMessage.value = errors[0]
    saveStatus.value = 'Kết quả chưa cập nhật do giả định chưa hợp lệ'
    return
  }
  clearTimeout(debounceTimer)
  await persistAndForecast()
}

async function resetDefaults() {
  if (!window.confirm('Khôi phục toàn bộ giả định về giá trị ban đầu theo workbook?')) return
  clearTimeout(debounceTimer)
  const requestId = ++latestRequest
  try {
    const data = await seedDefaults()
    if (requestId !== latestRequest) return
    applyDto(data)
    errorMessage.value = ''
    const res = await getForecast(snapshot())
    if (requestId !== latestRequest) return
    applyForecast(res)
    saveStatus.value = 'Đã khôi phục mặc định'
  } catch (e) {
    if (requestId !== latestRequest) return
    errorMessage.value = e.message
    saveStatus.value = 'Không khôi phục được mặc định'
  }
}

async function exportCsv() {
  try {
    await exportCsvApi(dto)
  } catch (e) {
    errorMessage.value = e.message
  }
}

const formatter = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 })
function money(value) {
  return `${formatter.format(Math.round(value ?? 0))} ₫`
}
</script>
