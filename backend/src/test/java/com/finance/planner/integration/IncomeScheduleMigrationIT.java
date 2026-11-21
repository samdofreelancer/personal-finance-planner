package com.finance.planner.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class IncomeScheduleMigrationIT {

    @Test
    void migratesBaseSalaryAndCompoundedRaiseRatesToEffectiveMonthAmounts() throws Exception {
        String url = "jdbc:h2:mem:income-schedule-migration;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__baseline_assumptions.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V4__create_flexible_expense_schedules.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V5__configurable_forecast_horizon_and_annual_raises.sql"));
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE assumptions SET base_salary = 50000001, january_multiplier = 1.75");
                statement.executeUpdate("UPDATE salary_raise_rates SET raise_rate = CASE raise_year "
                        + "WHEN 2027 THEN 0.0800 WHEN 2028 THEN 0.1000 WHEN 2029 THEN 0.0500 "
                        + "WHEN 2030 THEN 0.0000 WHEN 2031 THEN 0.0200 WHEN 2032 THEN 0.0300 END");
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V6__replace_salary_raises_with_income_schedule.sql"));
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT effective_month, amount FROM income_amount_changes ORDER BY effective_month")) {
            Map<String, Long> actual = new LinkedHashMap<>();
            while (rows.next()) actual.put(rows.getDate("effective_month").toLocalDate().toString().substring(0, 7), rows.getLong("amount"));

            BigDecimal amount = new BigDecimal("50000001");
            Map<String, Long> expected = new LinkedHashMap<>();
            expected.put("2026-11", 50_000_001L);
            BigDecimal factor = BigDecimal.ONE;
            Map<Integer, BigDecimal> rates = Map.of(
                    2027, new BigDecimal("0.08"), 2028, new BigDecimal("0.10"),
                    2029, new BigDecimal("0.05"), 2030, BigDecimal.ZERO,
                    2031, new BigDecimal("0.02"), 2032, new BigDecimal("0.03"));
            for (int year = 2027; year <= 2032; year++) {
                factor = factor.multiply(BigDecimal.ONE.add(rates.get(year)));
                expected.put(year + "-04", amount.multiply(factor).setScale(0, RoundingMode.HALF_UP).longValueExact());
            }
            assertEquals(expected, actual);
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT january_multiplier, starting_savings, forecast_years FROM assumptions")) {
            rows.next();
            assertEquals(new BigDecimal("1.75"), rows.getBigDecimal("january_multiplier"));
            assertEquals(30_000_000L, rows.getLong("starting_savings"));
            assertEquals(4, rows.getInt("forecast_years"));
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery("""
                     SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                     WHERE TABLE_NAME = 'ASSUMPTIONS' AND COLUMN_NAME = 'BASE_SALARY'
                     """)) {
            rows.next();
            assertEquals(0, rows.getInt(1));
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery("""
                     SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                     WHERE TABLE_NAME = 'SALARY_RAISE_RATES'
                     """)) {
            rows.next();
            assertEquals(0, rows.getInt(1));
        }
    }
}
