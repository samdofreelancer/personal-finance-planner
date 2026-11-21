package com.finance.planner.service;

import com.finance.planner.dto.IncomeChangeDTO;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IncomeScheduleService {

    public long amountAt(List<IncomeChangeDTO> changes, YearMonth period) {
        return changes.stream()
                .filter(change -> !YearMonth.parse(change.effectiveMonth()).isAfter(period))
                .max(Comparator.comparing(change -> YearMonth.parse(change.effectiveMonth())))
                .map(IncomeChangeDTO::amount)
                .orElseThrow(() -> new IllegalArgumentException("Không có mức thu nhập hiệu lực cho tháng " + period + "."));
    }
}
