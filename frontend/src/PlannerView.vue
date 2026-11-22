<template>
  <div class="app-shell">
    <button v-if="isDrawerOpen" class="drawer-backdrop" type="button" aria-label="Đóng menu điều hướng" @click="closeDrawer"></button>
    <aside class="sidebar" :class="{ 'drawer-open': isDrawerOpen }" aria-label="Điều hướng chính">
      <a class="brand" href="#overview" aria-label="Planwise - Tổng quan">
        <svg class="brand-mark" viewBox="0 0 40 40" aria-hidden="true" focusable="false">
          <rect width="40" height="40" rx="12" fill="currentColor" />
          <path d="M12 29V11h9.2a7 7 0 0 1 0 14H17" fill="none" stroke="#F7F8F2" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round" />
          <path d="m23 25 5 4-5 3" fill="none" stroke="#9DE3C7" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="brand-word">plan<span class="brand-light">wise</span></span>
      </a>
      <div class="drawer-mobile-heading">
        <span>ĐIỀU HƯỚNG</span>
      </div>
      <button type="button" class="drawer-close" aria-label="Đóng menu điều hướng" @click="closeDrawer">Đóng menu ×</button>
      <p class="nav-caption">WORKSPACE</p>
      <nav class="side-nav" aria-label="Các mục Planwise">
        <a class="nav-link" :class="{ active: activeSection === 'overview' }" href="#overview" :aria-current="activeSection === 'overview' ? 'page' : undefined" @click="closeDrawer"><span class="nav-icon">▦</span><span class="nav-label-desktop">Tổng quan</span><span class="nav-label-mobile">Tổng quan</span></a>
        <a class="nav-link" :class="{ active: activeSection === 'assumptions' }" href="#assumptions" :aria-current="activeSection === 'assumptions' ? 'page' : undefined" @click="closeDrawer"><span class="nav-icon">⚙</span><span class="nav-label-desktop">Giả định tài chính</span><span class="nav-label-mobile">Giả định</span></a>
        <a class="nav-link" :class="{ active: activeSection === 'loans' }" href="#loans" :aria-current="activeSection === 'loans' ? 'page' : undefined" @click="closeDrawer"><span class="nav-icon">⌁</span><span class="nav-label-desktop">Khoản vay</span><span class="nav-label-mobile">Khoản vay</span></a>
        <a class="nav-link" :class="{ active: activeSection === 'annual-report' }" href="#annual-report" :aria-current="activeSection === 'annual-report' ? 'page' : undefined" @click="closeDrawer"><span class="nav-icon">▥</span><span class="nav-label-desktop">Tổng hợp theo năm</span><span class="nav-label-mobile">Theo năm</span></a>
        <a class="nav-link" :class="{ active: activeSection === 'monthly-forecast' }" href="#monthly-forecast" :aria-current="activeSection === 'monthly-forecast' ? 'page' : undefined" @click="closeDrawer"><span class="nav-icon">▤</span><span class="nav-label-desktop">Dự báo theo tháng</span><span class="nav-label-mobile">Theo tháng</span></a>
      </nav>
      <div class="drawer-actions" aria-label="Thao tác tài khoản và tiện ích">
        <p class="drawer-actions-caption">TÀI KHOẢN & TIỆN ÍCH</p>
        <button class="drawer-action" data-action="household" type="button" aria-label="Không gian hộ gia đình" title="Không gian hộ gia đình" @click="$emit('open-household-settings')">Không gian hộ gia đình</button>
        <button class="drawer-action" data-action="logout" type="button" aria-label="Đăng xuất" @click="$emit('logout')">Đăng xuất</button>
        <button class="drawer-action" data-action="export" type="button" aria-label="Xuất dữ liệu CSV" :disabled="hasValidationError" @click="exportCsv">Xuất dữ liệu CSV</button>
      </div>
      <div class="drawer-period">KỲ MÔ PHỎNG<strong>{{ forecastPeriod }}</strong></div>
      <div class="sidebar-bottom">
        <span class="local-indicator"><span></span> Dữ liệu lưu trên máy chủ</span>
        <span class="sidebar-footnote">Kế hoạch cá nhân · {{ forecastYears }}</span>
      </div>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div class="mobile-topbar-main">
          <button type="button" class="drawer-open-button" aria-label="Mở menu điều hướng" @click="openDrawer">☰</button>
          <a class="brand mobile-brand" href="#overview" aria-label="Planwise - Tổng quan">
            <svg class="brand-mark" viewBox="0 0 40 40" aria-hidden="true" focusable="false">
              <rect width="40" height="40" rx="12" fill="currentColor" />
              <path d="M12 29V11h9.2a7 7 0 0 1 0 14H17" fill="none" stroke="#F7F8F2" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round" />
              <path d="m23 25 5 4-5 3" fill="none" stroke="#9DE3C7" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            <span class="brand-word">plan<span class="brand-light">wise</span></span>
          </a>
        </div>
        <div class="topbar-actions">
          <span class="local-badge"><span aria-hidden="true"></span> Dữ liệu lưu trên máy chủ</span>
          <span class="user-badge"><strong>{{ user.email }}</strong><small>{{ user.role === 'OWNER' ? 'Chủ hộ' : 'Thành viên' }}</small></span>
          <button id="household-settings-button" class="button button-secondary" type="button" aria-label="Không gian hộ gia đình" title="Không gian hộ gia đình" @click="$emit('open-household-settings')">Hộ gia đình</button>
          <button id="logout-button" class="button button-secondary" type="button" aria-label="Đăng xuất" @click="$emit('logout')">Đăng xuất</button>
          <button id="export-button" class="button button-secondary" type="button" aria-label="Xuất dữ liệu CSV" :disabled="hasValidationError" @click="exportCsv">↓ Xuất CSV</button>
        </div>
        <div class="breadcrumb"><span class="breadcrumb-prefix">Không gian tài chính <span>/</span></span> <strong>{{ activeSectionLabel }}</strong></div>
      </header>

      <main class="page" :aria-busy="isLoading">
    <div v-if="isLoading && !forecast" class="loading-state" role="status">
      <span class="loading-spinner" aria-hidden="true"></span>
      Đang tải dữ liệu tài chính và dự báo...
    </div>
    <div v-else-if="errorMessage && !forecast" class="empty-state empty-state-error" role="alert">
      <strong>Chưa thể tải dữ liệu</strong>
      <span>{{ errorMessage }}</span>
    </div>
    <section id="overview" class="page-heading outlook-hero">
      <div class="outlook-copy">
        <p class="eyebrow">FINANCIAL OUTLOOK <span class="outlook-live-dot"></span> KỊCH BẢN ĐANG MÔ PHỎNG</p>
        <h1>Kế hoạch tài chính<br class="mobile-break"> của bạn sẽ đi đến đâu?</h1>
        <p class="subtitle">Bức tranh dự kiến từ {{ forecastPeriod }}, dựa trên các giả định hiện tại.</p>
        <div class="outlook-actions">
          <a class="button button-primary" href="#assumptions">Điều chỉnh giả định <span aria-hidden="true">↗</span></a>
          <span class="outlook-period">{{ forecastPeriod }}</span>
        </div>
      </div>
      <div class="outlook-result" aria-live="polite">
        <span class="outlook-result-label">TIẾT KIỆM DỰ KIẾN CUỐI KỲ</span>
        <strong :class="metrics.savingsEnd < 0 ? 'negative' : ''">{{ money(metrics.savingsEnd) }}</strong>
        <span class="outlook-result-note">{{ provisional ? 'Kết quả tạm tính · cần xác minh' : `Dự kiến đến tháng 12/${forecastEndYear}` }}</span>
        <span class="outlook-result-disclaimer">Không phải số dư tài khoản thực tế</span>
      </div>
    </section>

    <section id="alert-area" aria-live="polite">
      <div v-if="alert" class="alert">
        <span class="alert-icon" aria-hidden="true">!</span>
        <span><strong v-if="alert.title">{{ alert.title }}</strong> {{ alert.text }}{{ alert.suffix ? ' ' + alert.suffix : '' }}</span>
      </div>
    </section>

    <section class="overview-section" aria-label="Triển vọng tài chính">
      <template v-if="hasFinancialInputs">
        <div class="section-intro"><div><p class="eyebrow">YOUR FINANCIAL JOURNEY</p><h2>Hành trình tài chính dự kiến</h2><p class="section-description">Theo dõi quỹ tiết kiệm tăng lên và dư nợ giảm xuống theo thời gian.</p></div><span class="section-meta">Mô phỏng · 2026–{{ forecastEndYear }}</span></div>
        <financial-position :assumptions="dto" :loans="loans" :forecast="forecast" />
        <div class="section-intro indicator-intro"><div><p class="eyebrow">KEY INDICATORS</p><h2>Các chỉ số đáng chú ý</h2></div><span class="section-meta">Dự báo năm {{ forecastEndYear }}</span></div>
        <metric-cards :metrics="metrics" :provisional="provisional" :year="forecastEndYear" />
      </template>
      <div v-else class="empty-state dashboard-empty-state">
        <strong>Bắt đầu từ các giả định tài chính</strong>
        <span>Nhập thu nhập, chi phí hoặc tiết kiệm ban đầu để xem dự báo thay vì các chỉ số 0 ₫.</span>
        <a class="button button-primary" href="#assumptions">Thiết lập giả định <span aria-hidden="true">↘</span></a>
      </div>
    </section>

    <section id="assumptions" class="content-section">
      <span id="assumption" class="anchor-alias" aria-hidden="true"></span>
      <div class="section-intro"><div><p class="eyebrow">THIẾT LẬP</p><h2>Giả định tài chính</h2><p class="section-description">Cập nhật thu nhập, chi phí và khoản vay để mô phỏng kế hoạch của gia đình.</p></div></div>
      <assumptions-form :dto="dto" :expenses="expenses" :error="errorMessage" :status="saveStatus" @input="onFormInput" @expenses-input="onExpenseInput" @save="saveAndForecast" @reset="resetDefaults" @load-sample="loadSampleData" />
    </section>

    <section id="loans" class="content-section">
      <div class="section-intro"><div><p class="eyebrow">KHOẢN VAY</p><h2>Lịch trả nợ</h2><p class="section-description">Quản lý từng khoản vay và các kỳ trả linh hoạt; dữ liệu chỉ được cập nhật khi bạn lưu.</p></div></div>
      <loan-schedule-form :loans="loans" :status="loanSaveStatus" :error="loanError" @save="saveLoanSchedule" />
    </section>

    <section id="annual-report" class="content-section">
      <div class="section-intro"><div><p class="eyebrow">PHÂN TÍCH DÀI HẠN</p><h2>Dòng tiền theo năm</h2><p class="section-description">So sánh thu nhập và chi phí qua từng năm trong kỳ kế hoạch.</p></div></div>
      <template v-if="forecast">
        <annual-chart :forecast="forecast" />
        <annual-table :forecast="forecast" />
      </template>
    </section>

    <section id="monthly-forecast" class="content-section">
      <div class="section-intro"><div><p class="eyebrow">CHI TIẾT DỰ BÁO</p><h2>Kế hoạch từng tháng</h2><p class="section-description">Theo dõi các khoản thu, chi, trả nợ và tiết kiệm lũy kế.</p></div></div>
    <monthly-table v-if="forecast" :forecast="forecast" />
    </section>

      </main>
      <footer class="page-footer">
        <span>Đây là dự báo theo các giả định bạn nhập, không phải tư vấn tài chính.</span>
        <span>Dữ liệu tài chính được lưu trên máy chủ của Planwise.</span>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getAssumptions, saveAssumptions, seedDefaults, getForecast, exportCsv as exportCsvApi, getLoans, saveLoans } from './services/api.js'
import { validateAssumptions } from './utils/validation.js'
import MetricCards from './components/MetricCards.vue'
import FinancialPosition from './components/FinancialPosition.vue'
import AssumptionsForm from './components/AssumptionsForm.vue'
import LoanScheduleForm from './components/LoanScheduleForm.vue'
import AnnualChart from './components/AnnualChart.vue'
import AnnualTable from './components/AnnualTable.vue'
import MonthlyTable from './components/MonthlyTable.vue'
import { formatCurrency } from './utils/currency.js'

const props = defineProps({ user: { type: Object, default: () => ({ email: "", role: "MEMBER" }) } })
defineEmits(['logout', 'open-household-settings'])

const dto = reactive({
  incomeChanges: [{ effectiveMonth: '2026-11', amount: 0 }],
  januaryMultiplier: 2,
  startingSavings: 0,
  forecastYears: 4,
})

const forecast = ref(null)
const expenses = ref([])
const loans = ref([])
const metrics = ref({ netCashFlow: 0, income: 0, expenses: 0, savingsEnd: 0 })
const errorMessage = ref('')
const saveStatus = ref('Đang tải...')
const loanSaveStatus = ref('')
const loanError = ref('')
const isSavingLoans = ref(false)
const isLoading = ref(true)
const provisional = ref(false)
const activeSection = ref('overview')
const isDrawerOpen = ref(false)
const sectionLabels = {
  overview: 'Tổng quan',
  assumptions: 'Giả định tài chính',
  loans: 'Khoản vay',
  'annual-report': 'Tổng hợp theo năm',
  'monthly-forecast': 'Dự báo theo tháng',
}
const activeSectionLabel = computed(() => sectionLabels[activeSection.value] || sectionLabels.overview)
const forecastPeriod = computed(() => {
  const months = forecast.value?.months || []
  if (!months.length) return 'Chưa có dữ liệu dự báo'
  const format = (month) => `${String(month.month).padStart(2, '0')}/${month.year}`
  return `${format(months[0])} — ${format(months.at(-1))}`
})
const forecastYears = computed(() => {
  const months = forecast.value?.months || []
  if (!months.length) return 'Chưa xác định'
  const first = months[0].year
  const last = months.at(-1).year
  return first === last ? String(first) : `${first}–${last}`
})
const forecastEndYear = computed(() => forecast.value?.annual?.at(-1)?.year ?? 2030)
const hasFinancialInputs = computed(() =>
  Number(dto.startingSavings) > 0
  || (dto.incomeChanges || []).some((item) => Number(item.amount) > 0)
  || (expenses.value || []).some((item) => (item.changes || []).some((change) => Number(change.amount) > 0))
  || loans.value.some((loan) => Number(loan.openingPrincipal) > 0),
)

function syncActiveSection() {
  const section = window.location.hash.slice(1)
  activeSection.value = sectionLabels[section] ? section : 'overview'
}

function openDrawer() {
  isDrawerOpen.value = true
}

function closeDrawer() {
  isDrawerOpen.value = false
}

function onDrawerKeydown(event) {
  if (event.key === 'Escape') closeDrawer()
}

function updateActiveSectionFromScroll() {
  const sections = Object.keys(sectionLabels)
  let current = 'overview'
  for (const section of sections) {
    const element = document.getElementById(section)
    if (element && element.getBoundingClientRect().top <= 150) current = section
  }
  activeSection.value = current
}

let previewTimer = null
let latestRequest = 0

const hasValidationError = computed(() => validateAssumptions(snapshot()).length > 0)

const alert = computed(() => {
  if (forecast.value?.annual?.some((row) => row.provisional)) {
    return {
      title: '',
      text: 'Một hoặc nhiều kết quả đang là tạm tính theo dữ liệu dự báo.',
      suffix: '',
    }
  }
  return null
})

onMounted(async () => {
  syncActiveSection()
  window.addEventListener('hashchange', syncActiveSection)
  window.addEventListener('scroll', updateActiveSectionFromScroll, { passive: true })
  window.addEventListener('keydown', onDrawerKeydown)
  try {
    const data = await getAssumptions()
    applyDto(data)
    loans.value = await getLoans()
    await loadForecast()
    errorMessage.value = ''
    saveStatus.value = 'Đã lưu trên máy chủ'
  } catch (e) {
    errorMessage.value = e.message
    saveStatus.value = 'Lỗi kết nối máy chủ'
  } finally {
    isLoading.value = false
  }
})

onBeforeUnmount(() => {
  clearTimeout(previewTimer)
  window.removeEventListener('hashchange', syncActiveSection)
  window.removeEventListener('scroll', updateActiveSectionFromScroll)
  window.removeEventListener('keydown', onDrawerKeydown)
})

function applyDto(data) {
  Object.assign(dto, {
    incomeChanges: JSON.parse(JSON.stringify(data.incomeChanges || [])),
    januaryMultiplier: Number(data.januaryMultiplier),
    startingSavings: data.startingSavings,
    forecastYears: Number(data.forecastYears ?? 4),
  })
  expenses.value = JSON.parse(JSON.stringify(data.expenses || []))
}

function snapshot() {
  return JSON.parse(JSON.stringify({ ...dto, expenses: expenses.value }))
}

async function loadForecast(source = snapshot()) {
  applyForecast(await getForecast(source))
}

function applyForecast(res) {
  forecast.value = res
  const finalYear = res.annual.at(-1)
  metrics.value = {
    netCashFlow: finalYear ? finalYear.netCashFlow : 0,
    income: finalYear ? finalYear.salary : 0,
    expenses: finalYear ? finalYear.expenses : 0,
    savingsEnd: finalYear ? finalYear.endingSavings : 0,
  }
  provisional.value = finalYear ? (finalYear.provisional || finalYear.endingSavingsProvisional) : false
}

function onFormInput(updated) {
  Object.assign(dto, updated)
  schedulePreview()
}

function onExpenseInput(updated) {
  expenses.value = updated
  schedulePreview()
}

function schedulePreview() {
  const requestId = ++latestRequest
  const payload = snapshot()
  const errors = validateAssumptions(payload)
  if (errors.length) {
    clearTimeout(previewTimer)
    errorMessage.value = errors[0]
    saveStatus.value = 'Chưa lưu · giả định chưa hợp lệ'
    return
  }
  errorMessage.value = ''
  saveStatus.value = 'Đang cập nhật mô phỏng · Chưa lưu'
  clearTimeout(previewTimer)
  previewTimer = setTimeout(() => previewForecast(payload, requestId), 180)
}

async function previewForecast(payload, requestId) {
  try {
    const res = await getForecast(payload)
    if (requestId === latestRequest) {
      applyForecast(res)
      saveStatus.value = 'Đã cập nhật mô phỏng · Chưa lưu'
    }
  } catch {
    if (requestId === latestRequest) saveStatus.value = 'Chưa lưu · chưa cập nhật được mô phỏng'
  }
}

async function persistAndForecast(payload = snapshot(), requestId = ++latestRequest) {
  saveStatus.value = 'Đang lưu...'
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

async function saveAndForecast(updated, updatedExpenses) {
  Object.assign(dto, updated)
  if (updatedExpenses) expenses.value = updatedExpenses
  const payload = snapshot()
  const errors = validateAssumptions(payload)
  if (errors.length) {
    errorMessage.value = errors[0]
    saveStatus.value = 'Chưa lưu · giả định chưa hợp lệ'
    return
  }
  clearTimeout(previewTimer)
  await persistAndForecast(payload)
}

function loadSampleData() {
  if (!window.confirm('Nạp dữ liệu mẫu sẽ thay thế các giả định đang nhập trên biểu mẫu. Dữ liệu mẫu chưa được lưu lên máy chủ cho đến khi bạn bấm Lưu. Tiếp tục?')) return
  clearTimeout(previewTimer)
  Object.assign(dto, {
    incomeChanges: [{ effectiveMonth: '2026-11', amount: 25000000 }],
    januaryMultiplier: 2,
    startingSavings: 100000000,
    forecastYears: 2,
  })
  expenses.value = [{ name: 'Chi phí gia đình', changes: [{ effectiveMonth: '2026-11', amount: 15000000 }] }]
  schedulePreview()
}

async function resetDefaults() {
  if (!window.confirm('Khôi phục toàn bộ giả định về giá trị ban đầu theo workbook?')) return
  clearTimeout(previewTimer)
  const requestId = ++latestRequest
  try {
    const data = await seedDefaults()
    if (requestId !== latestRequest) return
    applyDto(data)
    loans.value = await getLoans()
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
      await exportCsvApi(snapshot())
  } catch (e) {
    errorMessage.value = e.message
  }
}

async function saveLoanSchedule(updatedLoans) {
  if (isSavingLoans.value) return
  isSavingLoans.value = true
  loanSaveStatus.value = 'Đang lưu...'
  loanError.value = ''
  try {
    loans.value = await saveLoans(updatedLoans)
    loanSaveStatus.value = 'Đã lưu trên máy chủ'
    try {
      const res = await getForecast(snapshot())
      applyForecast(res)
    } catch (e) {
      loanError.value = `Lịch trả đã lưu, nhưng chưa cập nhật được dự báo: ${e.message}`
    }
  } catch (e) {
    loanError.value = e.message
    loanSaveStatus.value = 'Không lưu được lịch trả'
  } finally {
    isSavingLoans.value = false
  }
}

const money = formatCurrency
</script>
