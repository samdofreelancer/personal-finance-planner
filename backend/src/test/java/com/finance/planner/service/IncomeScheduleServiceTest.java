package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.finance.planner.dto.IncomeChangeDTO;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class IncomeScheduleServiceTest {

    private final IncomeScheduleService service = new IncomeScheduleService();

    @Test
    void usesLatestEffectiveIncomeChangeRegardlessOfInputOrder() {
        List<IncomeChangeDTO> changes = List.of(
                new IncomeChangeDTO("2028-01", 65_000_000L),
                new IncomeChangeDTO("2026-11", 50_000_000L),
                new IncomeChangeDTO("2027-04", 60_000_000L));

        assertEquals(50_000_000L, service.amountAt(changes, YearMonth.of(2027, 3)));
        assertEquals(60_000_000L, service.amountAt(changes, YearMonth.of(2027, 4)));
        assertEquals(60_000_000L, service.amountAt(changes, YearMonth.of(2027, 12)));
        assertEquals(65_000_000L, service.amountAt(changes, YearMonth.of(2028, 1)));
    }

    @Test
    void usesPreviousIncomeAfterFutureChangeIsRemoved() {
        List<IncomeChangeDTO> changes = List.of(new IncomeChangeDTO("2026-11", 50_000_000L));

        assertEquals(50_000_000L, service.amountAt(changes, YearMonth.of(2028, 1)));
    }
}
