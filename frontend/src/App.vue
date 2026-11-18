<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="Điều hướng chính">
      <a class="brand" href="#overview" aria-label="Planwise - Tổng quan">
        <svg class="brand-mark" viewBox="0 0 40 40" role="img" aria-label="">
          <rect width="40" height="40" rx="12" fill="currentColor" />
          <path d="M12 29V11h9.2a7 7 0 0 1 0 14H17" fill="none" stroke="#F7F8F2" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round" />
          <path d="m23 25 5 4-5 3" fill="none" stroke="#9DE3C7" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="brand-word">plan<span class="brand-light">wise</span></span>
      </a>
      <p class="nav-caption">WORKSPACE</p>
      <nav class="side-nav">
        <a class="nav-link" :class="{ active: activeSection === 'overview' }" href="#overview"><span class="nav-icon">▦</span> Tổng quan</a>
        <a class="nav-link" :class="{ active: activeSection === 'assumptions' }" href="#assumptions"><span class="nav-icon">⚙</span> Giả định tài chính</a>
        <a class="nav-link" :class="{ active: activeSection === 'annual-report' }" href="#annual-report"><span class="nav-icon">▥</span> Tổng hợp theo năm</a>
        <a class="nav-link" :class="{ active: activeSection === 'monthly-forecast' }" href="#monthly-forecast"><span class="nav-icon">▤</span> Dự báo theo tháng</a>
      </nav>
      <div class="sidebar-bottom">
        <span class="local-indicator"><span></span> Dữ liệu lưu trên máy chủ</span>
        <span class="sidebar-footnote">Kế hoạch cá nhân · 2026–2030</span>
      </div>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div class="breadcrumb">Không gian tài chính <span>/</span> <strong>{{ activeSectionLabel }}</strong></div>
        <div class="topbar-actions">
          <span class="local-badge"><span aria-hidden="true"></span> Dữ liệu lưu trên máy chủ</span>
          <button id="export-button" class="button button-secondary" type="button" :disabled="hasValidationError" @click="exportCsv">↓ Xuất CSV</button>
        </div>
      </header>

      <main class="page">
    <section id="overview" class="page-heading">
      <div>
        <p class="eyebrow">YOUR FINANCIAL JOURNEY</p>
        <h1>Tổng quan tài chính</h1>
        <p class="subtitle">Theo dõi dòng tiền và xem kế hoạch hiện tại sẽ đưa bạn đến đâu.</p>
      </div>
      <div class="heading-actions">
        <div class="heading-period"><span class="period-dot"></span> 11/2026 — 12/2030</div>
        <a class="button button-primary" href="#assumptions">✎ Điều chỉnh kế hoạch</a>
      </div>
    </section>

    <section id="alert-area" aria-live="polite">
      <div v-if="alert" class="alert">
        <span class="alert-icon" aria-hidden="true">!</span>
        <span><strong v-if="alert.title">{{ alert.title }}</strong> {{ alert.text }}{{ alert.suffix ? ' ' + alert.suffix : '' }}</span>
      </div>
    </section>

    <section class="overview-section" aria-label="Các chỉ số kế hoạch">
      <div class="section-intro"><div><p class="eyebrow">BỨC TRANH TỔNG THỂ</p><h2>Chỉ số kế hoạch</h2></div><span class="section-meta">Dự báo đến cuối năm 2030</span></div>
    <metric-cards
      :metrics="metrics"
      :provisional="provisional"
      :payoff-interest="dto.payoffInterest"
      :payoff-interest-confirmed="dto.payoffInterestConfirmed"
    />
    <financial-position :assumptions="dto" :forecast="forecast" />
    </section>

    <section id="assumptions" class="content-section">
      <div class="section-intro"><div><p class="eyebrow">THIẾT LẬP</p><h2>Giả định tài chính</h2><p class="section-description">Cập nhật thu nhập, chi phí và khoản vay để mô phỏng kế hoạch của gia đình.</p></div></div>
      <assumptions-form :dto="dto" :error="errorMessage" :status="saveStatus" @input="onFormInput" @save="saveAndForecast" @reset="resetDefaults" />
    </section>

    <section id="annual-report" class="content-section">
      <div class="section-intro"><div><p class="eyebrow">PHÂN TÍCH DÀI HẠN</p><h2>Dòng tiền theo năm</h2><p class="section-description">So sánh thu nhập và chi phí qua từng năm trong kỳ kế hoạch.</p></div></div>
      <annual-chart :forecast="forecast" />
      <annual-table :forecast="forecast" />
    </section>

    <section id="monthly-forecast" class="content-section">
      <div class="section-intro"><div><p class="eyebrow">CHI TIẾT DỰ BÁO</p><h2>Kế hoạch từng tháng</h2><p class="section-description">Theo dõi các khoản thu, chi, trả nợ và tiết kiệm lũy kế.</p></div></div>
    <monthly-table
      :forecast="forecast"
      :payoff-interest="dto.payoffInterest"
      :payoff-interest-confirmed="dto.payoffInterestConfirmed"
    />
    </section>

      </main>
      <footer class="page-footer">
        <span>Đây là dự báo theo các giả định bạn nhập, không phải tư vấn tài chính.</span>
        <span>Chạy cục bộ · Không gửi dữ liệu ra ngoài</span>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getAssumptions, saveAssumptions, seedDefaults, getForecast, exportCsv as exportCsvApi } from './services/api.js'
import { validateAssumptions } from './utils/validation.js'
import MetricCards from './components/MetricCards.vue'
import FinancialPosition from './components/FinancialPosition.vue'
import AssumptionsForm from './components/AssumptionsForm.vue'
import AnnualChart from './components/AnnualChart.vue'
import AnnualTable from './components/AnnualTable.vue'
import MonthlyTable from './components/MonthlyTable.vue'
import { formatCurrency } from './utils/currency.js'

const YEARS = [2027, 2028, 2029, 2030]
const dto = reactive({
  baseSalary: 42000000, januaryMultiplier: 2, living: 4250000,
  childThrough2026: 2500000, childQ12027: 2750000, childFromApr2027: 3250000,
  rentThroughJan2027: 3750000, rentFromFeb2027: 5750000,
  loanPrincipal: 120000000, fixedPayment: 10000000,
  novInterest: 1200000, decInterest: 1100000, payoffInterest: 1000000,
  payoffInterestConfirmed: false, startingSavings: 30000000,
  raises: { 2027: 0.08, 2028: 0.08, 2029: 0.08, 2030: 0.08 }
})

const forecast = ref(null)
const metrics = ref({ netCashFlow2030: 0, savingsEnd: 0, salary2030: 0 })
const errorMessage = ref('')
const saveStatus = ref('Đang tải...')
const provisional = ref(false)
const activeSection = ref('overview')
const sectionLabels = {
  overview: 'Tổng quan',
  assumptions: 'Giả định tài chính',
  'annual-report': 'Tổng hợp theo năm',
  'monthly-forecast': 'Dự báo theo tháng',
}
const activeSectionLabel = computed(() => sectionLabels[activeSection.value] || sectionLabels.overview)

function syncActiveSection() {
  const section = window.location.hash.slice(1)
  activeSection.value = sectionLabels[section] ? section : 'overview'
}

let debounceTimer = null
let latestRequest = 0

const hasValidationError = computed(() => validateAssumptions(dto).length > 0)

const alert = computed(() => {
  if (dto.payoffInterest == null || !dto.payoffInterestConfirmed) {
    return {
      title: 'Lãi tất toán chưa được xác nhận.',
      text: dto.payoffInterest == null
        ? 'Chưa nhập lãi tất toán ngân hàng. Dự báo đang tạm tính với 0 ₫ lãi tất toán; khoản lãi thực tế chưa được tính.'
        : `Dự báo đang tạm tính và đã bao gồm ${money(dto.payoffInterest)} lãi tất toán ước tính, chưa được đối chiếu với ngân hàng.`,
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
  syncActiveSection()
  window.addEventListener('hashchange', syncActiveSection)
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

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  window.removeEventListener('hashchange', syncActiveSection)
})

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

const money = formatCurrency
</script>
