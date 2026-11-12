import { DEFAULT_ASSUMPTIONS, buildForecast, validateAssumptions } from "./forecast.mjs";

const STORAGE_KEY = "personal-finance-planner.v1";
const form = document.querySelector("#assumptions-form");
const formatter = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 });
const monthFormatter = new Intl.DateTimeFormat("vi-VN", {
  month: "short", year: "numeric", timeZone: "UTC",
});
let assumptions = loadAssumptions();

function cloneDefaults() {
  return {
    ...DEFAULT_ASSUMPTIONS,
    raises: { ...DEFAULT_ASSUMPTIONS.raises },
  };
}

function loadAssumptions() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) return cloneDefaults();
    const parsed = JSON.parse(saved);
    const restored = { ...cloneDefaults(), ...parsed };
    restored.raises = { ...DEFAULT_ASSUMPTIONS.raises, ...parsed.raises };
    return validateAssumptions(restored).length ? cloneDefaults() : restored;
  } catch {
    return cloneDefaults();
  }
}

function money(value) {
  return `${formatter.format(Math.round(value))} ₫`;
}

function compactMoney(value) {
  const millions = value / 1_000_000;
  return `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(millions)} tr`;
}

function readForm() {
  const data = new FormData(form);
  const result = {
    baseSalary: Number(data.get("baseSalary")),
    januaryMultiplier: Number(data.get("januaryMultiplier")),
    raises: {},
    living: Number(data.get("living")),
    childThrough2026: Number(data.get("childThrough2026")),
    childQ12027: Number(data.get("childQ12027")),
    childFromApr2027: Number(data.get("childFromApr2027")),
    rentThroughJan2027: Number(data.get("rentThroughJan2027")),
    rentFromFeb2027: Number(data.get("rentFromFeb2027")),
    loanPrincipal: Number(data.get("loanPrincipal")),
    fixedPayment: Number(data.get("fixedPayment")),
    novInterest: Number(data.get("novInterest")),
    decInterest: Number(data.get("decInterest")),
    payoffInterest: data.get("payoffInterest") === "" ? null : Number(data.get("payoffInterest")),
    payoffInterestConfirmed: data.get("payoffInterestConfirmed") === "on",
    startingSavings: Number(data.get("startingSavings")),
  };
  for (const year of [2027, 2028, 2029, 2030]) {
    result.raises[year] = Number(data.get(`raise${year}`)) / 100;
  }
  return result;
}

function fillForm() {
  for (const [key, value] of Object.entries(assumptions)) {
    if (key === "raises") {
      for (const [year, rate] of Object.entries(value)) {
        form.elements.namedItem(`raise${year}`).value = String(rate * 100);
      }
    } else {
      const field = form.elements.namedItem(key);
      if (field?.type === "checkbox") field.checked = value;
      else if (field) field.value = value === null ? "" : String(value);
    }
  }
}

function signedClass(value) {
  return value < 0 ? "negative" : "positive";
}

function renderAlert(annual) {
  const area = document.querySelector("#alert-area");
  const payoffMonth = document.querySelector('[name="payoffInterest"]');
  if (assumptions.payoffInterest === null || !assumptions.payoffInterestConfirmed) {
    const message = assumptions.payoffInterest === null
      ? "Chưa nhập lãi tất toán ngân hàng. Dòng tiền tháng 01/2027 và số dư tiết kiệm sau đó đang tạm tính chưa gồm khoản lãi này."
      : `Lãi tất toán ${money(assumptions.payoffInterest)} đang lấy từ workbook nhưng chưa được đánh dấu đã đối chiếu với ngân hàng. Dòng tiền tháng 01/2027 và số dư tiết kiệm sau đó vẫn là tạm tính.`;
    area.innerHTML = `<div class="alert"><span class="alert-icon" aria-hidden="true">!</span><span><strong>Lãi tất toán chưa được xác nhận.</strong> ${message} Chỉ đánh dấu đã đối chiếu sau khi xác nhận với ngân hàng.</span></div>`;
    if (assumptions.payoffInterest === null) payoffMonth.setAttribute("aria-invalid", "true");
    else payoffMonth.removeAttribute("aria-invalid");
    return;
  }
  payoffMonth.removeAttribute("aria-invalid");
  area.innerHTML = "";
  if (annual.some((row) => row.provisional)) {
    area.innerHTML = `<div class="alert"><span class="alert-icon" aria-hidden="true">!</span><span>Một hoặc nhiều kết quả đang là tạm tính vì thiếu dữ liệu lãi tất toán.</span></div>`;
  }
}

function renderMetrics(annual, months) {
  const lastYear = annual.at(-1);
  const lastMonth = months.at(-1);
  const salary = months.find((row) => row.year === 2030 && row.month === 12).salary;
  const net = document.querySelector("#metric-net");
  net.textContent = money(lastYear.netCashFlow);
  net.className = `metric-value ${signedClass(lastYear.netCashFlow)}`;
  const savings = document.querySelector("#metric-savings");
  savings.textContent = money(lastMonth.cumulativeSavings);
  savings.className = `metric-value ${signedClass(lastMonth.cumulativeSavings)}`;
  document.querySelector("#metric-salary").textContent = money(salary);
  document.querySelector("#metric-savings-foot").textContent = lastMonth.savingsProvisional
    ? "Tạm tính · chưa gồm lãi tất toán chưa xác nhận"
    : "Dự kiến đến tháng 12/2030";
}

function renderAnnualTable(annual) {
  const tbody = document.querySelector("#annual-table tbody");
  tbody.replaceChildren();
  for (const row of annual) {
    const tr = document.createElement("tr");
    const values = [
      `<strong>${row.year}</strong>${row.partial ? '<span class="partial-tag">2 tháng</span>' : ""}`,
      money(row.salary),
      money(row.expenses),
      `<span class="${signedClass(row.netCashFlow)}">${money(row.netCashFlow)}${row.provisional ? " · tạm tính" : ""}</span>`,
      `<span class="savings-cell ${row.endingSavingsProvisional ? "provisional" : signedClass(row.endingSavings)}">${money(row.endingSavings)}${row.endingSavingsProvisional ? " · tạm tính" : ""}</span>`,
    ];
    for (const value of values) {
      const td = document.createElement("td");
      td.innerHTML = value;
      tr.append(td);
    }
    tbody.append(tr);
  }
}

function renderChart(annual) {
  const width = 900;
  const height = 260;
  const left = 82;
  const right = 22;
  const top = 23;
  const bottom = 38;
  const plotHeight = height - top - bottom;
  const plotWidth = width - left - right;
  const values = annual.map((row) => row.netCashFlow);
  const min = Math.min(0, ...values);
  const max = Math.max(0, ...values);
  const span = max - min || 1;
  const y = (value) => top + (max - value) / span * plotHeight;
  const zeroY = y(0);
  const ticks = 4;
  const grid = Array.from({ length: ticks + 1 }, (_, index) => {
    const value = min + span * index / ticks;
    const tickY = y(value);
    return `<line x1="${left}" y1="${tickY}" x2="${width - right}" y2="${tickY}" stroke="${value === 0 ? "#aebac7" : "#edf1f5"}" stroke-width="${value === 0 ? 1.5 : 1}" ${value === 0 ? 'stroke-dasharray="4 4"' : ""}/><text x="${left - 10}" y="${tickY + 4}" text-anchor="end" fill="#8492a1" font-size="10">${compactMoney(value)}</text>`;
  }).join("");
  const band = plotWidth / annual.length;
  const barWidth = Math.min(70, band * 0.48);
  const bars = annual.map((row, index) => {
    const center = left + band * (index + 0.5);
    const valueY = y(row.netCashFlow);
    const rectY = Math.min(zeroY, valueY);
    const rectHeight = Math.max(2, Math.abs(zeroY - valueY));
    const color = row.netCashFlow < 0 ? "#cc6668" : "#3e83ca";
    const labelY = row.netCashFlow < 0 ? rectY + rectHeight + 15 : rectY - 7;
    const aria = `${row.year}${row.partial ? ", năm chỉ gồm hai tháng" : ""}: ${money(row.netCashFlow)} dòng tiền ròng${row.provisional ? ", tạm tính do lãi tất toán chưa xác nhận" : ""}`;
    return `<g role="img" aria-label="${aria}"><title>${aria}</title><rect class="chart-bar" x="${center - barWidth / 2}" y="${rectY}" width="${barWidth}" height="${rectHeight}" rx="5" fill="${color}"/><text x="${center}" y="${labelY}" text-anchor="middle" fill="${color}" font-size="10" font-weight="700">${compactMoney(row.netCashFlow)}${row.provisional ? "†" : ""}</text><text x="${center}" y="${height - 13}" text-anchor="middle" fill="#5f7184" font-size="11" font-weight="650">${row.year}${row.partial ? "*" : ""}</text></g>`;
  }).join("");
  document.querySelector("#annual-chart").innerHTML =
    `<svg viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="chart-title chart-desc"><title id="chart-title">Dòng tiền ròng theo năm, từ 2026 đến 2030</title><desc id="chart-desc">${annual.map((row) => `${row.year}${row.partial ? " (hai tháng)" : ""}: ${money(row.netCashFlow)}${row.provisional ? " (tạm tính)" : ""}`).join("; ")}</desc>${grid}${bars}<text x="${left}" y="${height - 1}" fill="#91a0ad" font-size="9">* Năm 2026 gồm tháng 11–12 · † Dòng tiền tạm tính</text></svg>`;
}

function renderMonthlyTable(months) {
  const tbody = document.querySelector("#monthly-table tbody");
  const fragment = document.createDocumentFragment();
  for (const row of months) {
    const tr = document.createElement("tr");
    const values = [
      monthFormatter.format(row.date),
      money(row.salary),
      money(row.living),
      money(row.child),
      money(row.rent),
      money(row.openingPrincipal),
      money(row.principalPaid),
      row.provisional
        ? row.interestPaid === 0 && assumptions.payoffInterest === null
          ? "Chưa nhập · tạm tính"
          : `${money(row.interestPaid)} · tạm tính`
        : money(row.interestPaid),
      money(row.loanPayment),
      money(row.totalExpenses),
      money(row.netCashFlow),
      money(row.cumulativeSavings),
    ];
    values.forEach((value, index) => {
      const td = document.createElement("td");
      td.textContent = value;
      if (index === 0) td.setAttribute("scope", "row");
      if (index === 10) td.classList.add(signedClass(row.netCashFlow));
      if (index === 11) {
        td.classList.add("savings-cell", row.savingsProvisional ? "provisional" : signedClass(row.cumulativeSavings));
        if (row.savingsProvisional) td.setAttribute("aria-label", `${value}; tạm tính chưa gồm lãi tất toán`);
      }
      if (index === 7 && row.provisional) td.classList.add("provisional");
      tr.append(td);
    });
    fragment.append(tr);
  }
  tbody.replaceChildren(fragment);
}

function render() {
  const errorArea = document.querySelector("#form-error");
  const errors = validateAssumptions(assumptions);
  errorArea.textContent = errors[0] ?? "";
  document.querySelector("#export-button").disabled = errors.length > 0;
  if (errors.length) return;
  const { months, annual } = buildForecast(assumptions);
  renderAlert(annual);
  renderMetrics(annual, months);
  renderChart(annual);
  renderAnnualTable(annual);
  renderMonthlyTable(months);
}

form.addEventListener("input", () => {
  assumptions = readForm();
  const errors = validateAssumptions(assumptions);
  document.querySelector("#form-error").textContent = errors[0] ?? "";
  document.querySelector("#export-button").disabled = errors.length > 0;
  if (errors.length) {
    document.querySelector("#save-status").textContent = "Kết quả chưa cập nhật do giả định chưa hợp lệ";
    return;
  }
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(assumptions));
    document.querySelector("#save-status").textContent = "Đã lưu trên thiết bị này";
  } catch {
    document.querySelector("#save-status").textContent = "Không lưu được — kiểm tra quyền lưu của trình duyệt";
  }
  render();
});

document.querySelector("#reset-button").addEventListener("click", () => {
  if (!window.confirm("Khôi phục toàn bộ giả định về giá trị ban đầu theo workbook?")) return;
  assumptions = cloneDefaults();
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    document.querySelector("#save-status").textContent = "Không xóa được dữ liệu đã lưu trong trình duyệt";
  }
  fillForm();
  document.querySelector("#save-status").textContent = "Đã khôi phục mặc định";
  render();
});

document.querySelector("#export-button").addEventListener("click", () => {
  const { months, annual } = buildForecast(assumptions);
  const quote = (value) => `"${String(value).replaceAll('"', '""')}"`;
  const rows = [
    ["Tổng hợp theo năm"],
    ["Năm", "Giai đoạn", "Tổng thu nhập (VND)", "Tổng chi (VND)", "Dòng tiền ròng (VND)", "Tiết kiệm cuối năm (VND)", "Trạng thái"],
    ...annual.map((row) => [row.year, row.partial ? "Tháng 11–12 (năm không đầy đủ)" : "Cả năm", row.salary, row.expenses, row.netCashFlow, row.endingSavings,     row.provisional || row.endingSavingsProvisional ? "Có mục tạm tính" : "Đầy đủ"]),
    [],
    ["Dự báo theo tháng"],
    ["Tháng", "Lương", "Sinh hoạt", "Nuôi con", "Tiền nhà", "Gốc đầu kỳ", "Gốc trả", "Lãi trả", "Tổng trả nợ", "Tổng chi", "Dòng tiền ròng", "Tiết kiệm lũy kế", "Trạng thái"],
    ...months.map((row) => [
      `${String(row.month).padStart(2, "0")}/${row.year}`, row.salary, row.living, row.child,
      row.rent, row.openingPrincipal, row.principalPaid, row.provisional ? "" : row.interestPaid,
      row.loanPayment, row.totalExpenses, row.netCashFlow, row.cumulativeSavings,
      row.provisional ? "Dòng tiền tạm tính - chưa gồm lãi tất toán"
        : row.savingsProvisional ? "Tiết kiệm lũy kế tạm tính" : "Đầy đủ",
    ]),
  ];
  const csv = `\uFEFF${rows.map((row) => row.map(quote).join(",")).join("\r\n")}`;
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = "ke-hoach-tai-chinh-2026-2030.csv";
  link.click();
  URL.revokeObjectURL(url);
});

fillForm();
render();
