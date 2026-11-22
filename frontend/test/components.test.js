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

  it('explains the January multiplier and exposes a sample-data action', async () => {
    const wrapper = mount(AssumptionsForm, { props: { dto: assumptions(), expenses: expenses(), error: '', status: 'Đã lưu trên máy chủ' } })
    expect(wrapper.text()).toContain('Hệ số này chỉ nhân vào thu nhập tháng 1 hằng năm (tương đương thưởng / lương tháng 13); các tháng còn lại giữ nguyên mức thu nhập đang có hiệu lực.')
    await wrapper.get('#load-sample-button').trigger('click')
    expect(wrapper.emitted('load-sample')).toHaveLength(1)
  })

  it('shows the save state as a distinct status badge', () => {
    const wrapper = mount(AssumptionsForm, { props: { dto: assumptions(), expenses: expenses(), error: '', status: 'Đã cập nhật mô phỏng · Chưa lưu' } })
    expect(wrapper.get('#save-status').classes()).toContain('save-status-dirty')
    expect(wrapper.get('#save-status').text()).toContain('Chưa lưu')
    expect(wrapper.get('#save-status').element.parentElement.className).toBe('save-controls')
    expect(wrapper.get('#save-status').element.parentElement.querySelector('#save-assumptions-button')).not.toBeNull()
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

  it('disables income removal when only the required baseline remains', () => {
    const wrapper = mount(AssumptionsForm, { props: {
      dto: { ...assumptions(), incomeChanges: [{ effectiveMonth: '2026-11', amount: 0 }] },
      expenses: expenses(), error: '', status: '',
    } })
    expect(wrapper.findAll('.income-change-row')).toHaveLength(1)
    expect(wrapper.get('.income-change-remove').element.disabled).toBe(true)
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
    expect(wrapper.emitted('dirty-change').at(-1)).toEqual([true])
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
    expect(wrapper.text()).toContain('Dòng tiền ròng theo năm')
    expect(wrapper.text()).toContain('tr ₫')
    expect(wrapper.text()).toContain('* Năm 2026 chỉ gồm tháng 11–12 · † Có dòng tiền tạm tính')
    const positiveBar = wrapper.findAll('.chart-bar')[1]
    const positiveLabel = positiveBar.element.nextElementSibling
    expect(Number(positiveLabel.getAttribute('y'))).toBeLessThan(Number(positiveBar.attributes('y')))
    const negativeBar = wrapper.findAll('.chart-bar')[2]
    const negativeLabel = negativeBar.element.nextElementSibling
    expect(Number(negativeLabel.getAttribute('y'))).toBeGreaterThan(Number(negativeBar.attributes('y')) + Number(negativeBar.attributes('height')))
    const empty = mount(AnnualChart, { props: { forecast: null } })
    expect(empty.findAll('.chart-bar')).toHaveLength(0)
    expect(empty.text()).toContain('Chưa có dữ liệu biểu đồ')
  })

  it('renders annual chart and monthly baseline tables when all forecast values are zero', async () => {
    const zeroForecast = {
      annual: [
        { year: 2026, partial: true, salary: 0, expenses: 0, netCashFlow: 0, endingSavings: 0 },
        { year: 2027, partial: false, salary: 0, expenses: 0, netCashFlow: 0, endingSavings: 0 },
      ],
      months: Array.from({ length: 14 }, (_, index) => {
        const date = new Date(2026, 10 + index, 1)
        return {
          label: `thg ${date.getMonth() + 1} ${date.getFullYear()}`,
          year: date.getFullYear(),
          month: date.getMonth() + 1,
          salary: 0,
          recurringExpenses: 0,
          openingPrincipal: 0,
          principalPaid: 0,
          interestPaid: 0,
          loanPayment: 0,
          totalExpenses: 0,
          netCashFlow: 0,
          cumulativeSavings: 0,
        }
      }),
    }
    const chart = mount(AnnualChart, { props: { forecast: zeroForecast } })
    const monthly = mount(MonthlyTable, { props: { forecast: zeroForecast } })
    expect(chart.find('#annual-chart svg').exists()).toBe(true)
    expect(chart.findAll('.chart-bar')).toHaveLength(2)
    expect(chart.findAll('#annual-chart line[stroke-width="1.5"]')).toHaveLength(1)
    expect(chart.findAll('.chart-bar + text').every((node) => Number(node.attributes('y')) > 0)).toBe(true)
    expect(new Set(chart.findAll('#annual-chart svg text').slice(0, 5).map((node) => node.text())).size).toBeGreaterThan(1)
    expect(chart.findAll('#annual-chart text').some((node) => node.text().includes('0 tr'))).toBe(true)
    expect(chart.text()).toContain('Hãy nạp dữ liệu mẫu hoặc nhập giả định')
    expect(chart.text()).toContain('* Năm 2026 chỉ gồm tháng 11–12')
    expect(chart.text()).not.toContain('†')
    expect(monthly.findAll('.zero-value').length).toBeGreaterThan(0)
    expect(monthly.findAll('#monthly-table tbody tr')).toHaveLength(14)
    expect(monthly.find('#monthly-table tfoot tr').text()).toContain('Tổng cộng kỳ')
    expect(monthly.find('#monthly-table thead').text()).not.toContain('Gốc đầu kỳ')
    await monthly.get('.loan-columns-toggle').trigger('click')
    expect(monthly.find('#monthly-table thead').text()).toContain('Gốc đầu kỳ')
    await monthly.get('.loan-columns-toggle').trigger('click')
    expect(monthly.find('#monthly-table thead').text()).not.toContain('Gốc đầu kỳ')

    const multiYearForecast = {
      ...zeroForecast,
      months: zeroForecast.months.map((month) => ({
        ...month,
        salary: month.year === 2026 ? 100 : 200,
        cumulativeSavings: month.year === 2026 ? 10 : 20,
      })),
    }
    const filtered = mount(MonthlyTable, { props: { forecast: multiYearForecast } })
    await filtered.get('.year-filter button:nth-child(2)').trigger('click')
    expect(filtered.findAll('#monthly-table tbody tr')).toHaveLength(2)
    expect(filtered.find('#monthly-table tfoot tr').text()).toContain('200 ₫')
    expect(filtered.find('#monthly-table tfoot tr').text()).toContain('10 ₫')
    await filtered.get('.year-filter button:nth-child(3)').trigger('click')
    expect(filtered.findAll('#monthly-table tbody tr')).toHaveLength(12)
    expect(filtered.find('#monthly-table tfoot tr').text()).toContain('2.400 ₫')
    expect(filtered.find('#monthly-table tfoot tr').text()).toContain('20 ₫')
  })

  it('renders annual rows with partial-year and provisional labels', () => {
    const wrapper = mount(AnnualTable, { props: { forecast: forecast() } })
    expect(wrapper.findAll('tbody tr')).toHaveLength(3)
    expect(wrapper.text()).toContain('2 tháng')
    expect(wrapper.text()).toContain('tạm tính')
    expect(wrapper.findAll('.negative').length).toBeGreaterThan(0)
  })

  it('renders monthly provisional interest, missing interest and savings state', async () => {
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
    expect(wrapper.get('tfoot tr').text()).toContain('Tổng cộng kỳ')
    expect(wrapper.get('tfoot tr').text()).toContain('100.000.000 ₫')
    expect(wrapper.get('tfoot tr').text()).toContain('-15.700.000 ₫')
    expect(wrapper.get('tfoot tr').text()).toContain('-10.700.000 ₫')
    expect(wrapper.text()).toContain('Chi phí định kỳ')
    expect(wrapper.text()).toContain('Sinh hoạt phí: 4.000.000')
    expect(wrapper.get('tbody td:nth-child(3)').attributes('title')).toContain('Sinh hoạt phí: 4.000.000')
    await wrapper.get('.year-filter button:nth-child(2)').trigger('click')
    expect(wrapper.findAll('#monthly-table tbody tr')).toHaveLength(2)
    expect(wrapper.find('.row-count').text()).toBe('2 / 2 tháng')
    expect(wrapper.find('#monthly-table tfoot tr').text()).toContain('01/2027–02/2027')
    await wrapper.get('.year-filter button:first-child').trigger('click')
    expect(wrapper.find('.row-count').text()).toBe('2 tháng')
    expect(wrapper.find('tbody td[scope="row"]').exists()).toBe(false)

    const empty = mount(MonthlyTable, { props: { forecast: null } })
    expect(empty.text()).toContain('Chưa có dữ liệu dự báo theo tháng')
  })
})
