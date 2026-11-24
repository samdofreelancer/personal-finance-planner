import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import PlannerView from '../src/PlannerView.vue'
import AssumptionsForm from '../src/components/AssumptionsForm.vue'
import LoanScheduleForm from '../src/components/LoanScheduleForm.vue'
import {
  exportCsv,
  getAssumptions,
  getLoans,
  getForecast,
  saveLoans,
  saveAssumptions,
  seedDefaults,
} from '../src/services/api.js'

vi.mock('../src/services/api.js', () => ({
  getAssumptions: vi.fn(),
  getLoans: vi.fn(),
  saveAssumptions: vi.fn(),
  saveLoans: vi.fn(),
  seedDefaults: vi.fn(),
  getForecast: vi.fn(),
  exportCsv: vi.fn(),
}))

const RouterLinkStub = defineComponent({
  props: { to: { type: String, required: true } },
  setup(props, { slots }) {
    return () => h('a', {
      href: props.to,
      onClick: (event) => event.preventDefault(),
    }, slots.default?.())
  },
})

function mountPlanner(section = 'overview') {
  return mount(PlannerView, { props: { section }, global: { stubs: { RouterLink: RouterLinkStub } } })
}

function validAssumptions() {
  return {
    januaryMultiplier: 2,
    startingSavings: 30000000,
    forecastYears: 4,
    incomeChanges: [{ effectiveMonth: '2026-11', amount: 42000000 }],
    expenses: [
      { name: 'Sinh hoạt phí', changes: [{ effectiveMonth: '2026-11', amount: 4250000 }] },
      { name: 'Quỹ nuôi con', changes: [
        { effectiveMonth: '2026-11', amount: 2500000 },
        { effectiveMonth: '2027-01', amount: 2750000 },
        { effectiveMonth: '2027-04', amount: 3250000 },
      ] },
      { name: 'Tiền nhà', changes: [
        { effectiveMonth: '2026-11', amount: 3750000 },
        { effectiveMonth: '2027-02', amount: 5750000 },
      ] },
    ],
  }
}

function forecastResult({ provisional = false, include2030 = true } = {}) {
  const annual = [{
    year: 2027, salary: 40000000, expenses: 30000000, netCashFlow: 10000000, endingSavings: 40000000,
    provisional: false, endingSavingsProvisional: false,
  }]
  if (include2030) annual.push({
    year: 2030, salary: 60000000, expenses: 40000000, netCashFlow: 20000000, endingSavings: 90000000,
    provisional, endingSavingsProvisional: provisional,
  })
  return {
    annual,
    months: include2030 ? [{ year: 2030, month: 12, salary: 50000000 }] : [],
  }
}

async function mountLoadedApp(section = 'overview') {
  getAssumptions.mockResolvedValue(validAssumptions())
  getLoans.mockResolvedValue([])
  getForecast.mockResolvedValue(forecastResult())
  const wrapper = mountPlanner(section)
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  window.innerWidth = 1024
  getAssumptions.mockResolvedValue(validAssumptions())
  getLoans.mockResolvedValue([])
  getForecast.mockResolvedValue(forecastResult())
  saveAssumptions.mockResolvedValue(validAssumptions())
  saveLoans.mockImplementation(async (loans) => loans.map((loan, index) => ({ ...loan, id: index + 1,
    repayments: loan.repayments.map((payment, paymentIndex) => ({ ...payment, id: paymentIndex + 1 })) })))
  seedDefaults.mockResolvedValue(validAssumptions())
  exportCsv.mockResolvedValue(undefined)
})

describe('PlannerView', () => {
  it('renders one functional page at a time and exposes five mobile destinations', async () => {
    const wrapper = await mountLoadedApp('loans')
    expect(wrapper.find('#loans').exists()).toBe(true)
    expect(wrapper.find('#assumptions').exists()).toBe(false)
    expect(wrapper.find('#overview').exists()).toBe(false)
    expect(wrapper.get('.breadcrumb strong').text()).toBe('Khoản vay')
    expect(wrapper.findAll('.mobile-bottom-nav .mobile-nav-link')).toHaveLength(5)

    await wrapper.setProps({ section: 'assumptions' })
    expect(wrapper.find('#loans').exists()).toBe(false)
    expect(wrapper.find('#assumptions').exists()).toBe(true)
    wrapper.unmount()
  })

  it('loads assumptions and forecast, then displays calculated metrics', async () => {
    const wrapper = await mountLoadedApp()
    expect(getAssumptions).toHaveBeenCalledOnce()
    expect(getLoans).toHaveBeenCalledOnce()
    expect(getForecast).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Dữ liệu lưu trên máy chủ')
    expect(wrapper.get('.nav-link[href="/dashboard"]').attributes('aria-current')).toBe('page')
    expect(wrapper.text()).toContain('12/2030 — 12/2030')
    expect(wrapper.text()).toContain('Kế hoạch cá nhân · 2030')
    expect(wrapper.get('#metric-net').text()).toContain('20.000.000')
    expect(wrapper.get('#metric-income').text()).toContain('60.000.000')
    expect(wrapper.get('#metric-expenses').text()).toContain('40.000.000')
    expect(wrapper.find('.outlook-result strong').text()).toContain('90.000.000')
    expect(wrapper.text()).toContain('Hành trình tài chính dự kiến')
    expect(wrapper.text()).toContain('DƯ NỢ BAN ĐẦU')
    expect(wrapper.find('#alert-area .alert').exists()).toBe(false)
  })

  it('collapses and reopens the sidebar from the desktop menu controls', async () => {
    window.innerWidth = 1280
    const wrapper = await mountLoadedApp()

    await wrapper.get('.drawer-close').trigger('click')
    expect(wrapper.get('.app-shell').classes()).toContain('sidebar-collapsed')
    expect(wrapper.get('.sidebar-reopen-button').attributes('aria-label')).toBe('Mở menu điều hướng')

    await wrapper.get('.sidebar-reopen-button').trigger('click')
    expect(wrapper.get('.app-shell').classes()).not.toContain('sidebar-collapsed')
    expect(wrapper.find('.sidebar-reopen-button').exists()).toBe(false)
  })

  it('keeps mobile navigation links closing the drawer without collapsing the desktop sidebar', async () => {
    window.innerWidth = 390
    const wrapper = await mountLoadedApp()

    await wrapper.get('.drawer-open-button').trigger('click')
    expect(wrapper.get('.sidebar').classes()).toContain('drawer-open')

    await wrapper.get('.nav-link[href="/loans"]').trigger('click')
    expect(wrapper.get('.sidebar').classes()).not.toContain('drawer-open')
    expect(wrapper.get('.app-shell').classes()).not.toContain('sidebar-collapsed')
  })

  it('shows an actionable error when initial assumptions cannot be loaded', async () => {
    getAssumptions.mockRejectedValue(new Error('API offline'))
    const wrapper = mountPlanner('assumptions')
    await flushPromises()
    expect(wrapper.text()).toContain('API offline')
    expect(wrapper.text()).toContain('Lỗi kết nối máy chủ')
    expect(getForecast).not.toHaveBeenCalled()
  })

  it('loads explicit income changes without applying automatic raise defaults', async () => {
    const data = validAssumptions()
    data.incomeChanges.push({ effectiveMonth: '2027-04', amount: 50000000 })
    getAssumptions.mockResolvedValue(data)
    getForecast.mockResolvedValue(forecastResult())
    const wrapper = mountPlanner('assumptions')
    await flushPromises()

    expect(wrapper.find('#alert-area .alert').exists()).toBe(false)
    expect(wrapper.get('input[aria-label="Số tiền thu nhập, mốc 2"]').element.value).toBe('50.000.000')
    expect(wrapper.get('#assumptions').text()).toContain('Giả định của bạn')
  })

  it('does not use the retired payoff-interest field to alter the schedule-driven forecast', async () => {
    const data = validAssumptions()
    getAssumptions.mockResolvedValue(data)
    getForecast.mockResolvedValue(forecastResult())

    const wrapper = mountPlanner()
    await flushPromises()

    expect(wrapper.find('#alert-area .alert').exists()).toBe(false)
  })

  it('still surfaces provisional status returned by the forecast engine', async () => {
    getAssumptions.mockResolvedValue(validAssumptions())
    getForecast.mockResolvedValue(forecastResult({ provisional: true }))
    const wrapper = mountPlanner()
    await flushPromises()
    expect(wrapper.find('#alert-area .alert').text()).toContain('Một hoặc nhiều kết quả đang là tạm tính')
  })

  it('rejects invalid saves locally and persists valid saves before refreshing forecast', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    const form = wrapper.findComponent(AssumptionsForm)
    form.vm.$emit('save', { ...validAssumptions(), incomeChanges: [{ effectiveMonth: '2026-11', amount: -1 }] })
    await flushPromises()
    expect(wrapper.text()).toContain('Số tiền thu nhập phải là VND nguyên không âm.')
    expect(saveAssumptions).not.toHaveBeenCalled()

    form.vm.$emit('save', validAssumptions())
    await flushPromises()
    expect(saveAssumptions).toHaveBeenCalledOnce()
    expect(getForecast).toHaveBeenCalledTimes(2)
    expect(wrapper.get('.app-toast').text()).toBe('Đã lưu thay đổi thành công')
  })

  it('shows a blocking progress overlay instead of a small saving label while assumptions are persisted', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    let finishSave
    saveAssumptions.mockImplementationOnce(() => new Promise((resolve) => { finishSave = resolve }))

    wrapper.findComponent(AssumptionsForm).vm.$emit('save', validAssumptions())
    await flushPromises()

    expect(wrapper.get('.save-progress-overlay').text()).toContain('Đang lưu giả định tài chính...')
    expect(wrapper.find('#save-status').exists()).toBe(false)
    expect(wrapper.get('.save-progress-overlay').attributes('aria-busy')).toBe('true')

    finishSave(validAssumptions())
    await flushPromises()
    expect(wrapper.find('.save-progress-overlay').exists()).toBe(false)
  })

  it('does not show a success toast when saving assumptions fails', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    saveAssumptions.mockRejectedValueOnce(new Error('Máy chủ không khả dụng'))
    wrapper.findComponent(AssumptionsForm).vm.$emit('save', validAssumptions())
    await flushPromises()

    expect(wrapper.text()).toContain('Máy chủ không khả dụng')
    expect(wrapper.find('.app-toast').exists()).toBe(false)
  })

  it('confirms persistence even when the subsequent forecast refresh fails', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    getForecast.mockRejectedValueOnce(new Error('Lỗi dự báo'))
    wrapper.findComponent(AssumptionsForm).vm.$emit('save', validAssumptions())
    await flushPromises()

    expect(wrapper.get('.app-toast').text()).toBe('Đã lưu thay đổi thành công')
    expect(wrapper.text()).toContain('Đã lưu giả định, nhưng chưa cập nhật được dự báo: Lỗi dự báo')
  })

  it('exports CSV and handles export errors', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    await wrapper.get('#export-button').trigger('click')
    expect(exportCsv).toHaveBeenCalledOnce()

    exportCsv.mockRejectedValueOnce(new Error('Không thể xuất'))
    await wrapper.get('#export-button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Không thể xuất')
  })

  it('exposes a separate personal profile action in the account drawer', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    expect(wrapper.get('#profile-button').text()).toBe('Hồ sơ cá nhân')
    await wrapper.get('#profile-button').trigger('click')
    expect(wrapper.emitted('open-profile')).toHaveLength(1)
  })

  it('debounces forecast previews without persisting until the user explicitly saves', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    vi.useFakeTimers()
    const form = wrapper.findComponent(AssumptionsForm)
    form.vm.$emit('input', validAssumptions())
    await vi.advanceTimersByTimeAsync(180)
    await flushPromises()
    expect(getForecast).toHaveBeenCalledTimes(2)
    expect(saveAssumptions).not.toHaveBeenCalled()

    form.vm.$emit('save', validAssumptions())
    await flushPromises()
    expect(saveAssumptions).toHaveBeenCalledOnce()
    expect(getForecast).toHaveBeenCalledTimes(3)
    vi.useRealTimers()
  })

  it('loads sample assumptions only after confirmation and leaves them unsaved', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    window.confirm = vi.fn().mockReturnValue(true)
    vi.useFakeTimers()
    wrapper.findComponent(AssumptionsForm).vm.$emit('load-sample')
    await flushPromises()
    await vi.advanceTimersByTimeAsync(180)
    await flushPromises()

    expect(window.confirm).toHaveBeenCalledOnce()
    expect(wrapper.get('input[aria-label="Số tiền thu nhập, mốc 1"]').element.value).toBe('25.000.000')
    expect(wrapper.get('#startingSavings').element.value).toBe('100.000.000')
    expect(wrapper.get('input[aria-label="Số tiền khoản Chi phí gia đình, mốc 1"]').element.value).toBe('15.000.000')
    expect(saveAssumptions).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Đã cập nhật mô phỏng · Chưa lưu')
    await wrapper.setProps({ section: 'loans' })
    await flushPromises()
    const visibleLoanForm = wrapper.findComponent(LoanScheduleForm)
    expect(visibleLoanForm.props('sampleLoanDraft')).toMatchObject({ name: 'Khoản vay minh họa' })
    expect(visibleLoanForm.find('input[id^="loan-name-"]').element.value).toBe('Khoản vay minh họa')
    expect(visibleLoanForm.findAll('input[id^="repayment-principal-"]')).toHaveLength(2)
    expect(visibleLoanForm.text()).toContain('Lịch trả đang chỉnh sửa chưa được tính vào dự báo')
    expect(saveLoans).not.toHaveBeenCalled()
    vi.useRealTimers()
  })

  it('replaces zero-only dashboard metrics with a guided empty state', async () => {
    getAssumptions.mockResolvedValue({
      januaryMultiplier: 2, startingSavings: 0, forecastYears: 2,
      incomeChanges: [{ effectiveMonth: '2026-11', amount: 0 }], expenses: [],
    })
    getForecast.mockResolvedValue({ annual: [{ year: 2026, salary: 0, expenses: 0, netCashFlow: 0, endingSavings: 0 }], months: [] })
    const wrapper = mountPlanner()
    await flushPromises()
    expect(wrapper.find('.dashboard-empty-state').text()).toContain('Bắt đầu từ các giả định tài chính')
    expect(wrapper.find('.sample-empty-banner').exists()).toBe(true)
    expect(wrapper.find('#metric-net').exists()).toBe(false)
  })

  it('warns before leaving when assumptions have unsaved edits', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    wrapper.findComponent(AssumptionsForm).vm.$emit('input', { ...validAssumptions(), startingSavings: 31000000 })
    await flushPromises()
    expect(wrapper.text()).toContain('Đang cập nhật mô phỏng · Chưa lưu')
    const dirtyEvent = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(dirtyEvent)
    expect(dirtyEvent.defaultPrevented).toBe(true)
    wrapper.unmount()
  })

  it('keeps preview failures quiet and surfaces an explicit save failure', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    vi.useFakeTimers()
    getForecast.mockRejectedValueOnce(new Error('Preview unavailable'))
    saveAssumptions.mockRejectedValueOnce(new Error('Save unavailable'))
    wrapper.findComponent(AssumptionsForm).vm.$emit('input', validAssumptions())
    await vi.advanceTimersByTimeAsync(180)
    await flushPromises()
    expect(wrapper.text()).not.toContain('Preview unavailable')

    wrapper.findComponent(AssumptionsForm).vm.$emit('save', validAssumptions())
    await flushPromises()
    expect(wrapper.text()).toContain('Save unavailable')
    vi.useRealTimers()
  })

  it('resets only after confirmation and reports a reset failure', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    vi.stubGlobal('confirm', vi.fn().mockReturnValue(false))
    await wrapper.findComponent(AssumptionsForm).vm.$emit('reset')
    await flushPromises()
    expect(seedDefaults).not.toHaveBeenCalled()

    window.confirm = vi.fn().mockReturnValue(true)
    seedDefaults.mockRejectedValueOnce(new Error('Reset failed'))
    await wrapper.findComponent(AssumptionsForm).vm.$emit('reset')
    await flushPromises()
    expect(seedDefaults).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Reset failed')
    vi.unstubAllGlobals()
  })

  it('restores defaults after confirmation and refreshes the forecast', async () => {
    const wrapper = await mountLoadedApp('assumptions')
    window.confirm = vi.fn().mockReturnValue(true)
    await wrapper.findComponent(AssumptionsForm).vm.$emit('reset')
    await flushPromises()
    expect(seedDefaults).toHaveBeenCalledOnce()
    expect(getLoans).toHaveBeenCalledTimes(2)
    expect(wrapper.get('.app-toast').text()).toBe('Đã khôi phục mặc định thành công')
    expect(getForecast).toHaveBeenCalledTimes(2)
  })

  it('persists edited loan schedules and refreshes the forecast', async () => {
    const wrapper = await mountLoadedApp('loans')
    const schedule = wrapper.findComponent(LoanScheduleForm)
    const updated = [{ id: null, name: 'Vay cá nhân', openingPrincipal: 5000000,
      repayments: [{ id: null, dueDate: '2026-12-15', principalAmount: 1000000, interestAmount: 50000 }] }]
    schedule.vm.$emit('save', updated)
    await flushPromises()

    expect(saveLoans).toHaveBeenCalledWith(updated)
    expect(getForecast).toHaveBeenCalledTimes(2)
    expect(wrapper.get('.app-toast').text()).toBe('Đã lưu lịch trả nợ thành công')
  })

  it('shows the same full-screen progress treatment while a loan schedule is being saved', async () => {
    const wrapper = await mountLoadedApp('loans')
    let finishSave
    saveLoans.mockImplementationOnce(() => new Promise((resolve) => { finishSave = resolve }))
    wrapper.findComponent(LoanScheduleForm).vm.$emit('save', [])
    await flushPromises()

    expect(wrapper.get('.save-progress-overlay').text()).toContain('Đang lưu lịch trả nợ...')
    expect(wrapper.find('.loan-save-actions .save-status').exists()).toBe(false)

    finishSave([])
    await flushPromises()
    expect(wrapper.find('.save-progress-overlay').exists()).toBe(false)
  })

  it('does not show a success toast when saving a loan schedule fails', async () => {
    const wrapper = await mountLoadedApp('loans')
    saveLoans.mockRejectedValueOnce(new Error('Không thể lưu lịch trả'))
    wrapper.findComponent(LoanScheduleForm).vm.$emit('save', [])
    await flushPromises()

    expect(wrapper.text()).toContain('Không thể lưu lịch trả')
    expect(wrapper.find('.app-toast').exists()).toBe(false)
  })

  it('handles a forecast without a 2030 annual row', async () => {
    getForecast.mockResolvedValue(forecastResult({ include2030: false }))
    const wrapper = mountPlanner()
    await flushPromises()
    expect(wrapper.get('#metric-net').text()).toContain('10.000.000 ₫')
    expect(wrapper.get('#metric-income').text()).toContain('40.000.000 ₫')
    expect(wrapper.get('#metric-expenses').text()).toContain('30.000.000 ₫')
  })
})
