import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CurrencyInput from '../src/components/CurrencyInput.vue'
import AssumptionsForm from '../src/components/AssumptionsForm.vue'
import MetricCards from '../src/components/MetricCards.vue'
import AnnualChart from '../src/components/AnnualChart.vue'
import AnnualTable from '../src/components/AnnualTable.vue'
import MonthlyTable from '../src/components/MonthlyTable.vue'

function assumptions() {
  return {
    baseSalary: 42000000, januaryMultiplier: 2, living: 4000000,
    childThrough2026: 1000000, childQ12027: 1100000, childFromApr2027: 1200000,
    rentThroughJan2027: 2000000, rentFromFeb2027: 3000000,
    loanPrincipal: 100000000, fixedPayment: 10000000, novInterest: 1000000,
    decInterest: 900000, payoffInterest: null, payoffInterestConfirmed: false,
    startingSavings: 5000000,
    raises: { 2027: 0.08, 2028: 0.09, 2029: 0.1, 2030: 0.11 },
  }
}

function forecast() {
  return {
    annual: [
      { year: 2026, partial: true, salary: 84000000, expenses: 30000000, netCashFlow: 54000000, endingSavings: 59000000, provisional: true, endingSavingsProvisional: true },
      { year: 2027, partial: false, salary: 500000000, expenses: 200000000, netCashFlow: 300000000, endingSavings: 359000000, provisional: false, endingSavingsProvisional: false },
      { year: 2028, partial: false, salary: 550000000, expenses: 210000000, netCashFlow: -10000000, endingSavings: -1000000, provisional: false, endingSavingsProvisional: false },
    ],
    months: [
      { label: 'thg 1 2027', year: 2027, month: 1, salary: 50000000, living: 4000000, child: 1100000, rent: 2000000, openingPrincipal: 100000000, principalPaid: 100000000, interestPaid: 0, loanPayment: 100000000, totalExpenses: 107100000, netCashFlow: -57100000, cumulativeSavings: -52100000, provisional: true, savingsProvisional: true },
      { label: 'thg 2 2027', year: 2027, month: 2, salary: 50000000, living: 4000000, child: 1100000, rent: 3000000, openingPrincipal: 0, principalPaid: 0, interestPaid: 500000, loanPayment: 500000, totalExpenses: 8600000, netCashFlow: 41400000, cumulativeSavings: -10700000, provisional: false, savingsProvisional: true },
    ],
  }
}

describe('CurrencyInput', () => {
  it('formats the displayed value and emits an integer on input', async () => {
    const wrapper = mount(CurrencyInput, { props: { modelValue: 42000000, name: 'salary' } })
    const input = wrapper.get('input')
    expect(input.element.value).toBe('42.000.000')
    await input.setValue('42.500.000')
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([42500000])
  })

  it('supports nullable input, empty values, accessibility props and blur formatting', async () => {
    const wrapper = mount(CurrencyInput, { props: {
      modelValue: null, name: 'payoffInterest', nullable: true, required: false,
      placeholder: 'Chưa xác nhận', ariaLabel: 'Lãi tất toán', ariaInvalid: 'true',
    } })
    const input = wrapper.get('input')
    expect(input.element.value).toBe('')
    expect(input.attributes('required')).toBeUndefined()
    expect(input.attributes('aria-label')).toBe('Lãi tất toán')
    expect(input.attributes('aria-invalid')).toBe('true')
    await input.setValue('')
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([null])
    await input.setValue('1000')
    await wrapper.setProps({ modelValue: 1000 })
    await input.trigger('blur')
    expect(input.element.value).toBe('1.000')
  })
})

describe('AssumptionsForm', () => {
  it('emits reset, input and save payloads with percentage raises converted to fractions', async () => {
    const dto = assumptions()
    const wrapper = mount(AssumptionsForm, { props: { dto, error: 'Lỗi', status: 'Đã lưu' } })
    expect(wrapper.text()).toContain('Lỗi')
    expect(wrapper.text()).toContain('Đã lưu')
    expect(wrapper.get('input[name="raise2027"]').element.value).toBe('8')

    await wrapper.get('input[name="raise2027"]').setValue('12.5')
    await wrapper.get('form').trigger('input')
    expect(wrapper.emitted('input').at(-1)[0].raises[2027]).toBe(0.125)

    await wrapper.get('input[name="payoffInterestConfirmed"]').setValue(true)
    expect(wrapper.emitted('input').at(-1)[0].payoffInterestConfirmed).toBe(true)
    expect(dto.payoffInterestConfirmed).toBe(false)
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('save')).toHaveLength(1)
    await wrapper.get('#reset-button').trigger('click')
    expect(wrapper.emitted('reset')).toHaveLength(1)
  })

  it('normalizes blank and invalid raise values to zero and blank text', async () => {
    const dto = assumptions()
    dto.raises[2028] = 0
    const wrapper = mount(AssumptionsForm, { props: { dto, error: '', status: '' } })
    expect(wrapper.get('input[name="raise2028"]').element.value).toBe('')
    await wrapper.get('input[name="raise2029"]').setValue('')
    await wrapper.get('form').trigger('input')
    expect(wrapper.emitted('input').at(-1)[0].raises[2029]).toBe(0)
  })

  it('keeps percentage text synchronized when external raise assumptions change', async () => {
    const dto = assumptions()
    const wrapper = mount(AssumptionsForm, { props: { dto, error: '', status: '' } })
    await wrapper.setProps({ dto: { ...dto, raises: { ...dto.raises, 2030: 0.2 } } })
    expect(wrapper.get('input[name="raise2030"]').element.value).toBe('20')

    await wrapper.get('input[name="raise2030"]').setValue('15')
    await wrapper.get('form').trigger('input')
    expect(wrapper.emitted('input').at(-1)[0].raises[2030]).toBe(0.15)
  })

  it('edits a local draft without mutating the dto prop and associates labels with controls', async () => {
    const dto = assumptions()
    const wrapper = mount(AssumptionsForm, { props: { dto, error: '', status: '' } })

    expect(wrapper.get('label[for="living"]').element.control?.id).toBe('living')
    await wrapper.get('#living').setValue('4500000')
    await wrapper.get('form').trigger('input')

    expect(dto.living).toBe(4000000)
    expect(wrapper.emitted('input').at(-1)[0].living).toBe(4500000)
  })
})

describe('forecast presentation components', () => {
  it('renders signed metrics and provisional savings copy', () => {
    const wrapper = mount(MetricCards, { props: {
      metrics: { netCashFlow2030: -1000, savingsEnd: 5000, salary2030: 42000000 },
      provisional: true, payoffInterest: 1000000, payoffInterestConfirmed: false,
    } })
    expect(wrapper.get('#metric-net').classes()).toContain('negative')
    expect(wrapper.get('#metric-savings').classes()).toContain('positive')
    expect(wrapper.get('#metric-savings-foot').text()).toContain('Tạm tính')
    expect(wrapper.get('#metric-savings-foot').text()).toContain('đã bao gồm lãi tất toán 1.000.000 ₫ chưa xác nhận')

    const missing = mount(MetricCards, { props: {
      metrics: { netCashFlow2030: 0, savingsEnd: 0, salary2030: 0 },
      provisional: true, payoffInterest: null, payoffInterestConfirmed: false,
    } })
    expect(missing.get('#metric-savings-foot').text()).toContain('chưa gồm lãi tất toán')
  })

  it('renders chart ticks, positive and negative bars, and an empty state', () => {
    const wrapper = mount(AnnualChart, { props: { forecast: forecast() } })
    expect(wrapper.findAll('.chart-bar')).toHaveLength(3)
    expect(wrapper.text()).toContain('2026')
    expect(wrapper.text()).toContain('Dòng tiền dư mỗi năm')
    const empty = mount(AnnualChart, { props: { forecast: null } })
    expect(empty.findAll('.chart-bar')).toHaveLength(0)
  })

  it('renders annual rows with partial-year and provisional labels', () => {
    const wrapper = mount(AnnualTable, { props: { forecast: forecast() } })
    expect(wrapper.findAll('tbody tr')).toHaveLength(3)
    expect(wrapper.text()).toContain('2 tháng')
    expect(wrapper.text()).toContain('tạm tính')
    expect(wrapper.findAll('.negative').length).toBeGreaterThan(0)
  })

  it('renders monthly provisional interest, missing interest and savings state', () => {
    const data = forecast()
    const wrapper = mount(MonthlyTable, { props: { forecast: data, payoffInterest: null } })
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('Chưa nhập · tạm tính')
    expect(wrapper.find('.savings-cell').attributes('aria-label')).toContain('tạm tính')

    const withAmount = mount(MonthlyTable, { props: { forecast: data, payoffInterest: 1000, payoffInterestConfirmed: false } })
    expect(withAmount.text()).toContain('0 ₫ · tạm tính')
    expect(withAmount.find('.savings-cell').attributes('aria-label')).toContain('đã gồm lãi tất toán')
    expect(withAmount.find('.savings-cell').attributes('aria-label')).not.toContain('chưa gồm')
    expect(wrapper.get('tbody th[scope="row"]').text()).toBe('thg 1 2027')
    expect(wrapper.find('tbody td[scope="row"]').exists()).toBe(false)
  })
})
