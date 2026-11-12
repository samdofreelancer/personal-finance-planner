import test from "node:test";
import assert from "node:assert/strict";
import { DEFAULT_ASSUMPTIONS, buildForecast, validateAssumptions } from "./fixtures/legacy-forecast.mjs";

function defaults() {
  return {
    ...DEFAULT_ASSUMPTIONS,
    raises: { ...DEFAULT_ASSUMPTIONS.raises },
  };
}

test("generates 50 months and annual rows for 2026 through 2030", () => {
  const forecast = buildForecast(defaults());
  assert.equal(forecast.months.length, 50);
  assert.deepEqual(forecast.annual.map((row) => row.year), [2026, 2027, 2028, 2029, 2030]);
  assert.equal(forecast.annual[0].partial, true);
});

test("calculates the sample loan schedule and annual cash flow", () => {
  const { months, annual } = buildForecast(defaults());
  assert.equal(months[0].principalPaid, 8_800_000);
  assert.equal(months[1].principalPaid, 8_900_000);
  assert.equal(months[2].openingPrincipal, 102_300_000);
  assert.equal(months[2].principalPaid, 102_300_000);
  assert.equal(months[2].interestPaid, 1_000_000);
  assert.equal(months[2].provisional, true);
  assert.equal(months[2].savingsProvisional, true);
  assert.equal(annual[0].netCashFlow, 43_000_000);
  assert.equal(annual[1].salary, 576_240_000);
  assert.equal(annual[1].netCashFlow, 317_440_000);
  assert.equal(annual[4].netCashFlow, 566_896_440);
  assert.equal(annual[4].endingSavings, 1_933_801_976);
});

test("removes provisional markers only after payoff interest is confirmed", () => {
  const input = defaults();
  input.payoffInterestConfirmed = true;
  const { months, annual } = buildForecast(input);
  assert.equal(months[2].provisional, false);
  assert.equal(months.at(-1).savingsProvisional, false);
  assert.equal(annual[1].provisional, false);
  assert.equal(annual[4].endingSavingsProvisional, false);
});

test("applies raises from April and the editable January multiplier", () => {
  const input = defaults();
  input.januaryMultiplier = 2;
  const { months } = buildForecast(input);
  assert.equal(months.find((row) => row.year === 2027 && row.month === 1).salary, 84_000_000);
  assert.equal(months.find((row) => row.year === 2027 && row.month === 3).salary, 42_000_000);
  assert.equal(months.find((row) => row.year === 2027 && row.month === 4).salary, 45_360_000);
});

test("keeps missing payoff interest explicitly provisional", () => {
  const input = defaults();
  input.payoffInterest = null;
  const { months, annual } = buildForecast(input);
  assert.equal(months[2].provisional, true);
  assert.equal(months[2].interestPaid, 0);
  assert.equal(months[2].savingsProvisional, true);
  assert.equal(months.at(-1).savingsProvisional, true);
  assert.equal(annual[1].provisional, true);
  assert.equal(annual[1].endingSavingsProvisional, true);
  assert.equal(annual[2].provisional, false);
  assert.equal(annual[2].endingSavingsProvisional, true);
});

test("allows a cash shortfall and carries it into cumulative savings", () => {
  const input = defaults();
  input.startingSavings = 0;
  input.baseSalary = 0;
  const { months } = buildForecast(input);
  assert.ok(months[0].cumulativeSavings < 0);
  assert.equal(months[1].cumulativeSavings, months[0].cumulativeSavings + months[1].netCashFlow);
});

test("validates money, rate, and payment constraints", () => {
  const input = defaults();
  input.baseSalary = -1;
  input.raises[2028] = 1.1;
  input.novInterest = input.fixedPayment + 1;
  assert.equal(validateAssumptions(input).length, 3);
});

test("requires a payoff-interest amount before it can be marked confirmed", () => {
  const input = defaults();
  input.payoffInterest = null;
  input.payoffInterestConfirmed = true;
  assert.match(validateAssumptions(input).join(" "), /Nhập số lãi tất toán/);
});
