import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import App from '../src/App.vue'
import AssumptionsForm from '../src/components/AssumptionsForm.vue'
import {
  exportCsv,
  getAssumptions,
  getForecast,
  saveAssumptions,
  seedDefaults,
} from '../src/services/api.js'

vi.mock('../src/services/api.js', () => ({
  getAssumptions: vi.fn(),
  saveAssumptions: vi.fn(),
  seedDefaults: vi.fn(),
  getForecast: vi.fn(),
  exportCsv: vi.fn(),
}))

function validAssumptions() {
  return {
    baseSalary: 42000000, januaryMultiplier: 2, living: 4250000,
    childThrough2026: 2500000, childQ12027: 2750000, childFromApr2027: 3250000,
    rentThroughJan2027: 3750000, rentFromFeb2027: 5750000,
    loanPrincipal: 120000000, fixedPayment: 10000000,
    novInterest: 1200000, decInterest: 1100000, payoffInterest: 1000000,
    payoffInterestConfirmed: true, startingSavings: 30000000,
    raises: { 2027: 0.08, 2028: 0.08, 2029: 0.08, 2030: 0.08 },
  }
}

function forecastResult({ provisional = false, include2030 = true } = {}) {
  const annual = [{
    year: 2027, netCashFlow: 10000000, endingSavings: 40000000,
    provisional: false, endingSavingsProvisional: false,
  }]
  if (include2030) annual.push({
    year: 2030, netCashFlow: 20000000, endingSavings: 90000000,
    provisional, endingSavingsProvisional: provisional,
  })
  return {
    annual,
    months: include2030 ? [{ year: 2030, month: 12, salary: 50000000 }] : [],
  }
}

async function mountLoadedApp() {
  getAssumptions.mockResolvedValue(validAssumptions())
  getForecast.mockResolvedValue(forecastResult())
  const wrapper = mount(App)
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  getAssumptions.mockResolvedValue(validAssumptions())
  getForecast.mockResolvedValue(forecastResult())
  saveAssumptions.mockResolvedValue(validAssumptions())
  seedDefaults.mockResolvedValue(validAssumptions())
  exportCsv.mockResolvedValue(undefined)
})

describe('App', () => {
  it('loads assumptions and forecast, then displays calculated metrics', async () => {
    const wrapper = await mountLoadedApp()
    expect(getAssumptions).toHaveBeenCalledOnce()
    expect(getForecast).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Đã lưu trên máy chủ')
    expect(wrapper.get('#metric-net').text()).toContain('20.000.000')
    expect(wrapper.get('#metric-savings').text()).toContain('90.000.000')
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

  it('uses fallback raise defaults and keeps a warning for provisional payoff interest', async () => {
    const data = validAssumptions()
    delete data.raises[2028]
    data.payoffInterestConfirmed = false
    getAssumptions.mockResolvedValue(data)
    getForecast.mockResolvedValue(forecastResult({ provisional: true }))
    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('#alert-area .alert').text()).toContain('Lãi tất toán chưa được xác nhận')
    expect(wrapper.get('input[name="raise2028"]').element.value).toBe('10')
    expect(wrapper.get('#metric-savings-foot').text()).toContain('Tạm tính')
  })

  it('shows the generic provisional warning after payoff interest has been confirmed', async () => {
    getAssumptions.mockResolvedValue(validAssumptions())
    getForecast.mockResolvedValue(forecastResult({ provisional: true }))
    const wrapper = mount(App)
    await flushPromises()
    expect(wrapper.find('#alert-area .alert').text()).toContain('Một hoặc nhiều kết quả đang là tạm tính')
  })

  it('rejects invalid saves locally and persists valid saves before refreshing forecast', async () => {
    const wrapper = await mountLoadedApp()
    const form = wrapper.findComponent(AssumptionsForm)
    form.vm.$emit('save', { ...validAssumptions(), living: -1 })
    await flushPromises()
    expect(wrapper.text()).toContain('living phải là số VND nguyên không âm.')
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

  it('previews valid edits immediately and persists the latest snapshot after debounce', async () => {
    const wrapper = await mountLoadedApp()
    vi.useFakeTimers()
    const form = wrapper.findComponent(AssumptionsForm)
    form.vm.$emit('input', validAssumptions())
    await Promise.resolve()
    await Promise.resolve()
    expect(getForecast).toHaveBeenCalledTimes(2)

    await vi.advanceTimersByTimeAsync(250)
    await flushPromises()
    expect(saveAssumptions).toHaveBeenCalledOnce()
    expect(getForecast).toHaveBeenCalledTimes(3)
    vi.useRealTimers()
  })

  it('keeps preview failures quiet and surfaces the debounced save failure', async () => {
    const wrapper = await mountLoadedApp()
    vi.useFakeTimers()
    getForecast.mockRejectedValueOnce(new Error('Preview unavailable'))
    saveAssumptions.mockRejectedValueOnce(new Error('Save unavailable'))
    wrapper.findComponent(AssumptionsForm).vm.$emit('input', validAssumptions())
    await Promise.resolve()
    await Promise.resolve()
    expect(wrapper.text()).not.toContain('Preview unavailable')

    await vi.advanceTimersByTimeAsync(250)
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
    expect(wrapper.text()).toContain('Đã khôi phục mặc định')
    expect(getForecast).toHaveBeenCalledTimes(2)
  })

  it('handles a forecast without a 2030 annual row', async () => {
    getForecast.mockResolvedValue(forecastResult({ include2030: false }))
    const wrapper = mount(App)
    await flushPromises()
    expect(wrapper.get('#metric-net').text()).toContain('0 ₫')
    expect(wrapper.get('#metric-savings').text()).toContain('0 ₫')
  })
})
