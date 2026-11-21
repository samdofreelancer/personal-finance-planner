import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import App from '../src/App.vue'
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

async function mountLoadedApp() {
  getAssumptions.mockResolvedValue(validAssumptions())
  getLoans.mockResolvedValue([])
  getForecast.mockResolvedValue(forecastResult())
  const wrapper = mount(App)
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  getAssumptions.mockResolvedValue(validAssumptions())
  getLoans.mockResolvedValue([])
  getForecast.mockResolvedValue(forecastResult())
  saveAssumptions.mockResolvedValue(validAssumptions())
  saveLoans.mockImplementation(async (loans) => loans.map((loan, index) => ({ ...loan, id: index + 1,
    repayments: loan.repayments.map((payment, paymentIndex) => ({ ...payment, id: paymentIndex + 1 })) })))
  seedDefaults.mockResolvedValue(validAssumptions())
  exportCsv.mockResolvedValue(undefined)
})

describe('App', () => {
  it('loads assumptions and forecast, then displays calculated metrics', async () => {
    const wrapper = await mountLoadedApp()
    expect(getAssumptions).toHaveBeenCalledOnce()
    expect(getLoans).toHaveBeenCalledOnce()
    expect(getForecast).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Đã lưu trên máy chủ')
    expect(wrapper.get('.nav-link[href="#overview"]').attributes('aria-current')).toBe('page')
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

  it('shows an actionable error when initial assumptions cannot be loaded', async () => {
    getAssumptions.mockRejectedValue(new Error('API offline'))
    const wrapper = mount(App)
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
    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('#alert-area .alert').exists()).toBe(false)
    expect(wrapper.get('input[aria-label="Số tiền thu nhập, mốc 2"]').element.value).toBe('50.000.000')
    expect(wrapper.get('#metric-net-foot').text()).not.toContain('Tạm tính')
  })

  it('does not use the retired payoff-interest field to alter the schedule-driven forecast', async () => {
    const data = validAssumptions()
    getAssumptions.mockResolvedValue(data)
    getForecast.mockResolvedValue(forecastResult())

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('#alert-area .alert').exists()).toBe(false)
    expect(wrapper.get('#metric-net-foot').text()).not.toContain('Tạm tính')
  })

  it('still surfaces provisional status returned by the forecast engine', async () => {
    getAssumptions.mockResolvedValue(validAssumptions())
    getForecast.mockResolvedValue(forecastResult({ provisional: true }))
    const wrapper = mount(App)
    await flushPromises()
    expect(wrapper.find('#alert-area .alert').text()).toContain('Một hoặc nhiều kết quả đang là tạm tính')
  })

  it('rejects invalid saves locally and persists valid saves before refreshing forecast', async () => {
    const wrapper = await mountLoadedApp()
    const form = wrapper.findComponent(AssumptionsForm)
    form.vm.$emit('save', { ...validAssumptions(), incomeChanges: [{ effectiveMonth: '2026-11', amount: -1 }] })
    await flushPromises()
    expect(wrapper.text()).toContain('Số tiền thu nhập phải là VND nguyên không âm.')
    expect(saveAssumptions).not.toHaveBeenCalled()

    form.vm.$emit('save', validAssumptions())
    await flushPromises()
    expect(saveAssumptions).toHaveBeenCalledOnce()
    expect(getForecast).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Đã lưu trên máy chủ')
  })

  it('exports CSV and handles export errors', async () => {
    const wrapper = await mountLoadedApp()
    await wrapper.get('#export-button').trigger('click')
    expect(exportCsv).toHaveBeenCalledOnce()

    exportCsv.mockRejectedValueOnce(new Error('Không thể xuất'))
    await wrapper.get('#export-button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Không thể xuất')
  })

  it('debounces forecast previews without persisting until the user explicitly saves', async () => {
    const wrapper = await mountLoadedApp()
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

  it('keeps preview failures quiet and surfaces an explicit save failure', async () => {
    const wrapper = await mountLoadedApp()
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
    const wrapper = await mountLoadedApp()
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
    const wrapper = await mountLoadedApp()
    window.confirm = vi.fn().mockReturnValue(true)
    await wrapper.findComponent(AssumptionsForm).vm.$emit('reset')
    await flushPromises()
    expect(seedDefaults).toHaveBeenCalledOnce()
    expect(getLoans).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Đã khôi phục mặc định')
    expect(getForecast).toHaveBeenCalledTimes(2)
  })

  it('persists edited loan schedules and refreshes the forecast', async () => {
    const wrapper = await mountLoadedApp()
    const schedule = wrapper.findComponent(LoanScheduleForm)
    const updated = [{ id: null, name: 'Vay cá nhân', openingPrincipal: 5000000,
      repayments: [{ id: null, dueDate: '2026-12-15', principalAmount: 1000000, interestAmount: 50000 }] }]
    schedule.vm.$emit('save', updated)
    await flushPromises()

    expect(saveLoans).toHaveBeenCalledWith(updated)
    expect(getForecast).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Đã lưu trên máy chủ')
  })

  it('handles a forecast without a 2030 annual row', async () => {
    getForecast.mockResolvedValue(forecastResult({ include2030: false }))
    const wrapper = mount(App)
    await flushPromises()
    expect(wrapper.get('#metric-net').text()).toContain('0 ₫')
    expect(wrapper.get('#metric-income').text()).toContain('0 ₫')
    expect(wrapper.get('#metric-expenses').text()).toContain('0 ₫')
  })
})
