package com.finance.planner.controller;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ForecastResponse;
import com.finance.planner.service.ForecastService;
import com.finance.planner.service.ValidationService;
import com.finance.planner.service.LoanScheduleService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ForecastController {

    private final ForecastService forecastService;
    private final ValidationService validationService;
    private final LoanScheduleService loanScheduleService;

    public ForecastController(ForecastService forecastService, ValidationService validationService,
                              LoanScheduleService loanScheduleService) {
        this.forecastService = forecastService;
        this.validationService = validationService;
        this.loanScheduleService = loanScheduleService;
    }

    @PostMapping("/forecast")
    public ResponseEntity<ForecastResponse> simulate(@RequestBody AssumptionDTO assumptions) {
        validationService.validate(assumptions);
        return ResponseEntity.ok(forecastService.buildForecast(assumptions, loanScheduleService.getLoans()));
    }

    @PostMapping("/export/csv")
    public ResponseEntity<ByteArrayResource> exportCsvSimulation(@RequestBody AssumptionDTO assumptions) {
        validationService.validate(assumptions);
        return csvResponse(assumptions);
    }

    private ResponseEntity<ByteArrayResource> csvResponse(AssumptionDTO assumptions) {
        ForecastResponse response = forecastService.buildForecast(assumptions, loanScheduleService.getLoans());
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // UTF-8 BOM
        csv.append("Tổng hợp theo năm\n");
        csv.append("Năm,Giai đoạn,Tổng thu nhập (VND),Tổng chi (VND),Dòng tiền ròng (VND),Tiết kiệm cuối năm (VND),Trạng thái\n");
        for (var y : response.getAnnual()) {
            csv.append(y.getYear()).append(",");
            csv.append(y.isPartial() ? "Tháng 11–12 (năm không đầy đủ)" : "Cả năm").append(",");
            csv.append(y.getSalary()).append(",").append(y.getExpenses()).append(",");
            csv.append(y.getNetCashFlow()).append(",").append(y.getEndingSavings()).append(",");
            csv.append(y.isProvisional() || y.isEndingSavingsProvisional() ? "Có mục tạm tính" : "Đầy đủ").append("\n");
        }
        csv.append("\nDự báo theo tháng\n");
        csv.append("Tháng,Lương,Chi phí định kỳ,Chi tiết khoản chi,Gốc đầu kỳ,Gốc trả,Lãi trả,Tổng trả nợ,Tổng chi,Dòng tiền ròng,Tiết kiệm lũy kế,Trạng thái\n");
        for (var m : response.getMonths()) {
            csv.append(String.format("%02d/%d", m.getMonth(), m.getYear())).append(",");
            csv.append(m.getSalary()).append(",").append(m.getRecurringExpenses()).append(",");
            String details = m.getExpenseBreakdown().stream().map(item -> item.name() + ": " + item.amount())
                    .collect(java.util.stream.Collectors.joining("; "));
            csv.append('"').append(details.replace("\"", "\"\"")).append("\",");
            csv.append(m.getOpeningPrincipal()).append(",");
            csv.append(m.getPrincipalPaid()).append(",");
            csv.append(m.getInterestPaid()).append(",");
            csv.append(m.getLoanPayment()).append(",").append(m.getTotalExpenses()).append(",");
            csv.append(m.getNetCashFlow()).append(",").append(m.getCumulativeSavings()).append(",");
            csv.append(m.isProvisional() ? "Dòng tiền tạm tính"
                    : m.isSavingsProvisional() ? "Tiết kiệm lũy kế tạm tính" : "Đầy đủ").append("\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ke-hoach-tai-chinh-2026-" + (2026 + assumptions.getForecastYears()) + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=utf-8"))
                .contentLength(bytes.length)
                .body(resource);
    }
}
