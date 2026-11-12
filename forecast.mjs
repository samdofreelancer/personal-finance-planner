export const DEFAULT_ASSUMPTIONS = Object.freeze({
  baseSalary: 50_000_000,
  januaryMultiplier: 2.5,
  raises: { 2027: 0.1, 2028: 0.1, 2029: 0.1, 2030: 0.1 },
  living: 3_000_000,
  childThrough2026: 5_000_000,
  childQ12027: 6_000_000,
  childFromApr2027: 7_000_000,
  rentThroughJan2027: 5_000_000,
  rentFromFeb2027: 8_000_000,
  loanPrincipal: 368_261_629,
  fixedPayment: 20_570_000,
  novInterest: 3_565_230,
  decInterest: 3_756_588,
  payoffInterest: 3_683_193,
  payoffInterestConfirmed: false,
  startingSavings: 220_000_000,
});

export function validateAssumptions(input) {
  const errors = [];
  const moneyFields = [
    "baseSalary", "living", "childThrough2026", "childQ12027",
    "childFromApr2027", "rentThroughJan2027", "rentFromFeb2027",
    "loanPrincipal", "fixedPayment", "novInterest", "decInterest",
    "startingSavings",
  ];

  for (const field of moneyFields) {
    if (!Number.isSafeInteger(input[field]) || input[field] < 0) {
      errors.push(`${field} phải là số VND nguyên không âm.`);
    }
  }
  if (input.payoffInterest !== null &&
      (!Number.isSafeInteger(input.payoffInterest) || input.payoffInterest < 0)) {
    errors.push("Lãi tất toán phải là số VND nguyên không âm hoặc để trống nếu chưa xác nhận.");
  }
  if (input.payoffInterestConfirmed && input.payoffInterest === null) {
    errors.push("Nhập số lãi tất toán trước khi đánh dấu đã xác nhận.");
  }
  if (typeof input.payoffInterestConfirmed !== "boolean") {
    errors.push("Trạng thái xác nhận lãi tất toán không hợp lệ.");
  }
  if (!Number.isFinite(input.januaryMultiplier) ||
      input.januaryMultiplier <= 0 || input.januaryMultiplier > 20) {
    errors.push("Hệ số lương tháng 1 phải lớn hơn 0 và không vượt quá 20.");
  }
  for (const year of [2027, 2028, 2029, 2030]) {
    if (!Number.isFinite(input.raises?.[year]) ||
        input.raises[year] < 0 || input.raises[year] > 1) {
      errors.push(`Mức tăng lương năm ${year} phải từ 0% đến 100%.`);
    }
  }
  if (input.novInterest > input.fixedPayment || input.decInterest > input.fixedPayment) {
    errors.push("Lãi từng kỳ không thể lớn hơn khoản trả cố định của kỳ đó.");
  }
  return errors;
}

export function buildForecast(input) {
  const errors = validateAssumptions(input);
  if (errors.length) throw new RangeError(errors.join(" "));

  const months = [];
  let openingPrincipal = input.loanPrincipal;
  let cumulativeSavings = input.startingSavings;
  let savingsProvisional = false;

  for (let index = 0; index < 50; index += 1) {
    const date = new Date(Date.UTC(2026, 10 + index, 1));
    const year = date.getUTCFullYear();
    const month = date.getUTCMonth() + 1;
    let salary = input.baseSalary;
    for (const raiseYear of [2027, 2028, 2029, 2030]) {
      if (year > raiseYear || (year === raiseYear && month >= 4)) {
        salary *= 1 + input.raises[raiseYear];
      }
    }
    if (month === 1) salary *= input.januaryMultiplier;
    salary = Math.round(salary);

    const child = year === 2026 ? input.childThrough2026
      : year === 2027 && month <= 3 ? input.childQ12027
        : input.childFromApr2027;
    const rent = year < 2027 || (year === 2027 && month === 1)
      ? input.rentThroughJan2027
      : input.rentFromFeb2027;
    const loanOpening = openingPrincipal;
    let principalPaid = 0;
    let interestPaid = 0;
    let loanPayment = 0;
    let provisional = false;

    if (year === 2026 && month === 11) {
      interestPaid = input.novInterest;
      principalPaid = Math.min(loanOpening, Math.max(0, input.fixedPayment - interestPaid));
      loanPayment = principalPaid + interestPaid;
    } else if (year === 2026 && month === 12) {
      interestPaid = input.decInterest;
      principalPaid = Math.min(loanOpening, Math.max(0, input.fixedPayment - interestPaid));
      loanPayment = principalPaid + interestPaid;
    } else if (year === 2027 && month === 1) {
      principalPaid = loanOpening;
      if (input.payoffInterest === null || !input.payoffInterestConfirmed) provisional = true;
      interestPaid = input.payoffInterest ?? 0;
      loanPayment = principalPaid + interestPaid;
    }
    if (provisional) savingsProvisional = true;

    openingPrincipal = Math.max(0, loanOpening - principalPaid);
    const totalExpenses = input.living + child + rent + loanPayment;
    const netCashFlow = salary - totalExpenses;
    cumulativeSavings += netCashFlow;
    months.push({
      date, year, month, salary, living: input.living, child, rent,
      openingPrincipal: loanOpening, principalPaid, interestPaid, loanPayment,
      totalExpenses, netCashFlow, cumulativeSavings, provisional,
      savingsProvisional,
    });
  }

  const annual = [];
  for (let year = 2026; year <= 2030; year += 1) {
    const yearMonths = months.filter((row) => row.year === year);
    annual.push({
      year,
      partial: year === 2026,
      salary: yearMonths.reduce((sum, row) => sum + row.salary, 0),
      expenses: yearMonths.reduce((sum, row) => sum + row.totalExpenses, 0),
      netCashFlow: yearMonths.reduce((sum, row) => sum + row.netCashFlow, 0),
      endingSavings: yearMonths.at(-1).cumulativeSavings,
      provisional: yearMonths.some((row) => row.provisional),
      endingSavingsProvisional: yearMonths.at(-1).savingsProvisional,
    });
  }

  return { months, annual };
}
