package com.finance.planner.controller;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ForecastResponse;
import com.finance.planner.service.ForecastService;
import com.finance.planner.service.ValidationService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;
    private final ValidationService validationService;

    public ForecastController(ForecastService forecastService, ValidationService validationService) {
        this.forecastService = forecastService;
        this.validationService = validationService;
    }

    @GetMapping
    public ResponseEntity<ForecastResponse> get(@ModelAttribute AssumptionDTO assumptions) {
        validationService.validate(assumptions);
        return ResponseEntity.ok(forecastService.buildForecast(assumptions));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<ByteArrayResource> exportCsv(@ModelAttribute AssumptionDTO assumptions) {
        validationService.validate(assumptions);
        ForecastResponse response = forecastService.buildForecast(assumptions);
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // UTF-8 BOM
        csv.append("Tổng hợp theo năm\n");
        csv.append("Năm,Giai đoạn,Tổng thu nhập (VND),Tổng chi (VND),Dòng tiền ròng (VND),Tiết kiệm cuối năm (VND),Trạng thái\n");
        for (var y : response.getAnnual()) {
            csv.append(y.getYear()).append(",");
            csv.append(y.isPartial() ? "Tháng 11–12 (năm không đầy đủ)" : "Cả năm").append(",");
            csv.append(y.getSalary()).append(",").append(y.getExpenses()).append(",");
            csv.append(y.getNetCashFlow()).append(",").append(y.getEndingSavings()).append(",");
            csv.append(y.isProvisional() ? "Có mục tạm tính" : "Đầy đủ").append("\n");
        }
        csv.append("\nDự báo theo tháng\n");
        csv.append("Tháng,Lương,Sinh hoạt,Nuôi con,Tiền nhà,Gốc đầu kỳ,Gốc trả,Lãi trả,Tổng trả nợ,Tổng chi,Dòng tiền ròng,Tiết kiệm lũy kế,Trạng thái\n");
        for (var m : response.getMonths()) {
            csv.append(String.format("%02d/%d", m.getMonth(), m.getYear())).append(",");
            csv.append(m.getSalary()).append(",").append(m.getLiving()).append(",").append(m.getChild()).append(",");
            csv.append(m.getRent()).append(",").append(m.getOpeningPrincipal()).append(",");
            csv.append(m.getPrincipalPaid()).append(",").append(m.getInterestPaid()).append(",");
            csv.append(m.getLoanPayment()).append(",").append(m.getTotalExpenses()).append(",");
            csv.append(m.getNetCashFlow()).append(",").append(m.getCumulativeSavings()).append(",");
            csv.append(m.isProvisional() ? "Dòng tiền tạm tính" : "Đầy đủ").append("\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ke-hoach-tai-chinh-2026-2030.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=utf-8"))
                .contentLength(bytes.length)
                .body(resource);
    }
}