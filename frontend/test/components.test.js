import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CurrencyInput from '../src/components/CurrencyInput.vue'
import AssumptionsForm from '../src/components/AssumptionsForm.vue'
import LoanScheduleForm from '../src/components/LoanScheduleForm.vue'
import MetricCards from '../src/components/MetricCards.vue'
import FinancialPosition from '../src/components/FinancialPosition.vue'
import AnnualChart from '../src/components/AnnualChart.vue'
import AnnualTable from '../src/components/AnnualTable.vue'
import MonthlyTable from '../src/components/MonthlyTable.vue'

function assumptions() {
  return {
    januaryMultiplier: 2,
    startingSavings: 5000000,
    forecastYears: 4,
    incomeChanges: [
      { effectiveMonth: '2026-11', amount: 42000000 },
      { effectiveMonth: '2027-04', amount: 50000000 },
      { effectiveMonth: '2031-04', amount: 60000000 },
    ],
  }
}

function expenses() {
  return [
    { name: 'Sinh hoạt phí', changes: [{ effectiveMonth: '2026-11', amount: 4000000 }] },
    { name: 'Tiền nhà', changes: [
      { effectiveMonth: '2026-11', amount: 2000000 },
      { effectiveMonth: '2027-02', amount: 3000000 },
    ] },
  ]
}

function forecast() {
  return {
    annual: [
      { year: 2026, partial: true, salary: 84000000, expenses: 30000000, netCashFlow: 54000000, endingSavings: 59000000, provisional: true, endingSavingsProvisional: true },
      { year: 2027, partial: false, salary: 500000000, expenses: 200000000, netCashFlow: 300000000, endingSavings: 359000000, provisional: false, endingSavingsProvisional: false },
      { year: 2028, partial: false, salary: 550000000, expenses: 210000000, netCashFlow: -10000000, endingSavings: -1000000, provisional: false, endingSavingsProvisional: false },
    ],
    months: [
      { label: 'thg 1 2027', year: 2027, month: 1, salary: 50000000, recurringExpenses: 7100000, expenseBreakdown: [{ name: 'Sinh hoạt phí', amount: 4000000 }, { name: 'Tiền nhà', amount: 2000000 }, { name: 'Nuôi con', amount: 1100000 }], openingPrincipal: 100000000, principalPaid: 100000000, interestPaid: 0, loanPayment: 100000000, totalExpenses: 107100000, netCashFlow: -57100000, cumulativeSavings: -52100000, provisional: true, savingsProvisional: true },
      { label: 'thg 2 2027', year: 2027, month: 2, salary: 50000000, recurringExpenses: 8100000, expenseBreakdown: [{ name: 'Sinh hoạt phí', amount: 4000000 }, { name: 'Tiền nhà', amount: 3000000 }, { name: 'Nuôi con', amount: 1100000 }], openingPrincipal: 0, principalPaid: 0, interestPaid: 500000, loanPayment: 500000, totalExpenses: 8600000, netCashFlow: 41400000, cumulativeSavings: -10700000, provisional: false, savingsProvisional: true },
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
  it('renders and emits an editable effective-month income schedule', async () => {
    const dto = assumptions()
    const wrapper = mount(AssumptionsForm, { props: { dto, expenses: expenses(), error: 'Lỗi', status: 'Đã lưu' } })
    expect(wrapper.text()).toContain('Lỗi')
    expect(wrapper.text()).toContain('Đã lưu')
    expect(wrapper.findAll('.income-change-row')).toHaveLength(3)
    expect(wrapper.get('.income-change-row .month-input-display').text()).toBe('11/2026')

    await wrapper.get('input[aria-label="Số tiền thu nhập, mốc 1"]').setValue('50.000.000')
    await wrapper.get('form').trigger('input')
    expect(wrapper.emitted('input').at(-1)[0].incomeChanges[0].amount).toBe(50000000)

    expect(wrapper.text()).toContain('Tiết kiệm')
    expect(wrapper.get('label[for="startingSavings"]').exists()).toBe(true)
    expect(wrapper.find('#loanPrincipal').exists()).toBe(false)
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('save')).toHaveLength(1)
    await wrapper.get('#reset-button').trigger('click')
    expect(wrapper.emitted('reset')).toHaveLength(1)
  })

  it('adds and removes future income changes but keeps the required baseline', async () => {
    const dto = assumptions()
    const wrapper = mount(AssumptionsForm, { props: { dto, expenses: expenses(), error: '', status: '' } })
    expect(wrapper.get('.income-change-remove').attributes('disabled')).toBeDefined()
    await wrapper.get('.income-add-change').trigger('click')
    expect(wrapper.findAll('.income-change-row')).toHaveLength(4)
    await wrapper.findAll('.income-change-remove').at(-1).trigger('click')
    expect(wrapper.findAll('.income-change-row')).toHaveLength(3)
    expect(wrapper.get('input[aria-label="Tháng hiệu lực thu nhập, mốc 1"]').element.value).toBe('2026-11')
  })

  it('shows effective months in MM/YYYY while keeping the native month picker input', () => {
    const wrapper = mount(AssumptionsForm, { props: { dto: assumptions(), expenses: expenses(), error: '', status: '' } })
    const incomeMonth = wrapper.get('input[aria-label="Tháng hiệu lực thu nhập, mốc 2"]')
    const expenseMonth = wrapper.get('input[aria-label="Tháng hiệu lực khoản 1, mốc 1"]')

    expect(incomeMonth.attributes('type')).toBe('month')
    expect(incomeMonth.element.value).toBe('2027-04')
    expect(incomeMonth.element.disabled).toBe(false)
    expect(wrapper.findAll('.month-input-display').map((node) => node.text())).toContain('04/2027')
    expect(expenseMonth.attributes('type')).toBe('month')
    expect(expenseMonth.element.value).toBe('2026-11')
  })

  it('allows correcting or removing a future entry that temporarily duplicates the baseline month', async () => {
    const wrapper = mount(AssumptionsForm, { props: { dto: assumptions(), expenses: expenses(), error: '', status: '' } })
    await wrapper.get('input[aria-label="Tháng hiệu lực thu nhập, mốc 2"]').setValue('2026-11')

    const removeButtons = wrapper.findAll('.income-change-remove')
    expect(removeButtons[0].attributes('disabled')).toBeDefined()
    expect(removeButtons[1].attributes('disabled')).toBeUndefined()
    await removeButtons[1].trigger('click')
    expect(wrapper.findAll('.income-change-row')).toHaveLength(2)
  })

  it('preserves income changes beyond the selected forecast horizon', async () => {
    const dto = { ...assumptions(), forecastYears: 6 }
    const wrapper = mount(AssumptionsForm, { props: { dto, expenses: expenses(), error: '', status: '' } })
    await wrapper.get('select[name="forecastYears"]').setValue('2')
    expect(wrapper.emitted('input').at(-1)[0].forecastYears).toBe(2)
    expect(wrapper.emitted('input').at(-1)[0].incomeChanges).toEqual(dto.incomeChanges)
  })

  it('emits the newly selected horizon immediately on the select change event', async () => {
    const wrapper = mount(AssumptionsForm, { props: { dto: assumptions(), expenses: expenses(), error: '', status: '' } })
    await wrapper.get('select[name="forecastYears"]').setValue('2')
    expect(wrapper.emitted('input').at(-1)[0].forecastYears).toBe(2)
    expect(wrapper.emitted('input').at(-1)[0].incomeChanges).toHaveLength(3)
  })

  it('edits dynamic expense schedules without mutating the expense prop', async () => {
    const dto = assumptions()
    const originalExpenses = expenses()
    const wrapper = mount(AssumptionsForm, { props: { dto, expenses: originalExpenses, error: '', status: '' } })

    expect(wrapper.text()).toContain('Chi phí hằng tháng')
    await wrapper.get('input[aria-label="Số tiền khoản Sinh hoạt phí, mốc 1"]').setValue('4500000')
    await wrapper.get('form').trigger('input')

    expect(originalExpenses[0].changes[0].amount).toBe(4000000)
    expect(wrapper.emitted('expenses-input').at(-1)[0][0].changes[0].amount).toBe(4500000)
    await wrapper.get('.expense-add').trigger('click')
    expect(wrapper.emitted('expenses-input').at(-1)[0]).toHaveLength(3)
    await wrapper.get('.expense-remove').trigger('click')
    expect(wrapper.emitted('expenses-input').at(-1)[0]).toHaveLength(2)
  })
})

describe('LoanScheduleForm', () => {
  it('supports adding a loan and multiple repayment periods before saving', async () => {
    const wrapper = mount(LoanScheduleForm, { props: { loans: [], status: '', error: '' } })
    await wrapper.get('button').trigger('click')
    await wrapper.get('input[id^="loan-name-"]').setValue('Vay mua nhà')
    await wrapper.get('input[id^="loan-principal-"]').setValue('100.000.000')
    await wrapper.get('button.add-repayment').trigger('click')

    const dates = wrapper.findAll('input[type="date"]')
    await dates[0].setValue('2026-11-10')
    await wrapper.get('input[id^="repayment-principal-"]').setValue('5.000.000')
    await wrapper.get('input[id^="repayment-interest-"]').setValue('100.000')
    await wrapper.get('button.add-repayment').trigger('click')
    const nextDate = wrapper.findAll('input[type="date"]')[1]
    await nextDate.setValue('2026-11-25')
    await wrapper.findAll('input[id^="repayment-principal-"]')[1].setValue('3.000.000')
    await wrapper.findAll('input[id^="repayment-interest-"]')[1].setValue('80.000')

    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('save')).toHaveLength(1)
    expect(wrapper.emitted('save')[0][0][0]).toMatchObject({
      name: 'Vay mua nhà', openingPrincipal: 100000000,
      repayments: [
        { dueDate: '2026-11-10', principalAmount: 5000000, interestAmount: 100000 },
        { dueDate: '2026-11-25', principalAmount: 3000000, interestAmount: 80000 },
      ],
    })
  })

  it('rejects invalid amounts and supports deleting a repayment and a loan', async () => {
    const wrapper = mount(LoanScheduleForm, { props: { loans: [{
      id: 1, name: 'Khoản vay', openingPrincipal: 1000,
      repayments: [{ id: 2, dueDate: '2026-11-10', principalAmount: 100, interestAmount: 10 }],
    }], status: '', error: '' } })
    await wrapper.get('input[id^="loan-principal-"]').setValue('')
    expect(wrapper.text()).toContain('Dư nợ gốc phải là số VND nguyên không âm.')
    await wrapper.get('input[id^="loan-principal-"]').setValue('1.000')
    await wrapper.get('button[aria-label="Xóa kỳ trả 1"]').trigger('click')
    expect(wrapper.text()).toContain('Chưa có kỳ trả')
    await wrapper.get('.danger-text').trigger('click')
    expect(wrapper.text()).toContain('Chưa có khoản vay')
  })

  it('marks edits as unsaved after a previously persisted schedule changes', async () => {
    const wrapper = mount(LoanScheduleForm, { props: { status: 'Đã lưu trên máy chủ', loans: [{
      id: 1, name: 'Khoản vay', openingPrincipal: 1000, repayments: [],
    }] } })
    expect(wrapper.text()).toContain('Đã lưu trên máy chủ')
    await wrapper.get('input[id^="loan-principal-"]').setValue('900')
    expect(wrapper.text()).toContain('Có thay đổi chưa lưu')
  })

  it('formats all loan amount inputs with Vietnamese thousands separators', () => {
    const wrapper = mount(LoanScheduleForm, { props: { loans: [{
      id: 1, name: 'Vay mua nhà', openingPrincipal: 100000000,
      repayments: [{ id: 2, dueDate: '2026-11-10', principalAmount: 5000000, interestAmount: 125000 }],
    }], status: '', error: '' } })

    expect(wrapper.get('input[id^="loan-principal-"]').element.value).toBe('100.000.000')
    expect(wrapper.get('input[id^="repayment-principal-"]').element.value).toBe('5.000.000')
    expect(wrapper.get('input[id^="repayment-interest-"]').element.value).toBe('125.000')
    expect(wrapper.text()).toContain('5.125.000 ₫')
  })
})

describe('forecast presentation components', () => {
  it('summarizes starting savings, debt payoff and the savings trajectory', () => {
    const wrapper = mount(FinancialPosition, { props: {
      assumptions: { startingSavings: 5000000 }, loans: [{ openingPrincipal: 100000000 }],
      forecast: forecast(),
    } })
    expect(wrapper.text()).toContain('TIẾT KIỆM BAN ĐẦU')
    expect(wrapper.text()).toContain('DƯ NỢ BAN ĐẦU')
    expect(wrapper.text()).toContain('Dự kiến tất toán thg 1 2027')
    expect(wrapper.text()).toContain('Quỹ tiết kiệm dự kiến')
    expect(wrapper.text()).toContain('tr ₫')
    expect(wrapper.findAll('circle').length).toBeGreaterThanOrEqual(3)
    expect(wrapper.text()).toContain('-10.700.000 ₫')
  })

  it('keeps the opening debt when the forecast has no monthly results', () => {
    const wrapper = mount(FinancialPosition, { props: {
      assumptions: { startingSavings: 0 }, loans: [{ openingPrincipal: 25000000 }],
      forecast: { annual: [], months: [] },
    } })
    expect(wrapper.text()).toContain('25.000.000 ₫')
    expect(wrapper.text()).toContain('Vẫn còn dư nợ ở cuối kỳ')
    expect(wrapper.find('svg').exists()).toBe(false)
  })

  it('renders annual income, expenses and signed net cash flow', () => {
    const wrapper = mount(MetricCards, { props: {
      metrics: { netCashFlow: -1000, income: 42000000, expenses: 42001000 },
      provisional: true,
      year: 2030,
    } })
    expect(wrapper.get('#metric-net').classes()).toContain('negative')
    expect(wrapper.get('#metric-income').text()).toContain('42.000.000')
    expect(wrapper.get('#metric-expenses').text()).toContain('42.001.000')
    expect(wrapper.get('#metric-net-foot').text()).toContain('Tạm tính')

    const missing = mount(MetricCards, { props: {
      metrics: { netCashFlow: 0, income: 0, expenses: 0 },
      provisional: false,
      year: 2030,
    } })
    expect(missing.get('#metric-net-foot').text()).toContain('Thu nhập trừ tổng chi phí')
  })

  it('renders chart ticks, positive and negative bars, and an empty state', () => {
    const wrapper = mount(AnnualChart, { props: { forecast: forecast() } })
    expect(wrapper.findAll('.chart-bar')).toHaveLength(3)
    expect(wrapper.text()).toContain('2026')
    expect(wrapper.text()).toContain('Dòng tiền dư mỗi năm')
    expect(wrapper.text()).toContain('tr ₫')
    const empty = mount(AnnualChart, { props: { forecast: null } })
    expect(empty.findAll('.chart-bar')).toHaveLength(0)
    expect(empty.text()).toContain('Chưa có dữ liệu biểu đồ')
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
    const wrapper = mount(MonthlyTable, { props: { forecast: data } })
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.find('.row-count').text()).toBe('2 tháng')
    expect(wrapper.text()).toContain('0 ₫ · tạm tính')
    expect(wrapper.find('.savings-cell').attributes('aria-label')).toContain('tạm tính')

    const withAmount = mount(MonthlyTable, { props: { forecast: data } })
    expect(withAmount.text()).toContain('0 ₫ · tạm tính')
    expect(withAmount.find('.savings-cell').attributes('aria-label')).toContain('tạm tính')
    expect(wrapper.get('tbody th[scope="row"]').text()).toBe('thg 1 2027')
    expect(wrapper.text()).toContain('Chi phí định kỳ')
    expect(wrapper.text()).toContain('Sinh hoạt phí: 4.000.000')
    expect(wrapper.get('tbody td:nth-child(3)').attributes('title')).toContain('Sinh hoạt phí: 4.000.000')
    expect(wrapper.find('tbody td[scope="row"]').exists()).toBe(false)

    const empty = mount(MonthlyTable, { props: { forecast: null } })
    expect(empty.text()).toContain('Chưa có dữ liệu dự báo theo tháng')
  })
})
