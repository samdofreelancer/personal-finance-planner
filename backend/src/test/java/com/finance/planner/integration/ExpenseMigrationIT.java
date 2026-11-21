package com.finance.planner.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.DriverManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class ExpenseMigrationIT {

    @Test
    void migratesLegacyExpenseValuesToEquivalentEffectiveMonthSchedules() throws Exception {
        String url = "jdbc:h2:mem:expense-migration;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__baseline_assumptions.sql"));
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        UPDATE assumptions SET living = 4250111, child_through_2026 = 2500222,
                        child_q1_2027 = 2750333, child_from_apr_2027 = 3250444,
                        rent_through_jan_2027 = 3750555, rent_from_feb_2027 = 5750666
                        """);
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V4__create_flexible_expense_schedules.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V5__configurable_forecast_horizon_and_annual_raises.sql"));
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT raise_year, raise_rate FROM salary_raise_rates ORDER BY raise_year")) {
            Map<Integer, java.math.BigDecimal> raises = new java.util.LinkedHashMap<>();
            while (rows.next()) raises.put(rows.getInt("raise_year"), rows.getBigDecimal("raise_rate"));
            assertEquals(Map.of(
                    2027, new java.math.BigDecimal("0.0800"),
                    2028, new java.math.BigDecimal("0.0800"),
                    2029, new java.math.BigDecimal("0.0800"),
                    2030, new java.math.BigDecimal("0.0800"),
                    2031, new java.math.BigDecimal("0.0000"),
                    2032, new java.math.BigDecimal("0.0000")), raises);
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery("""
                     SELECT c.name, ch.effective_month, ch.amount
                     FROM expense_categories c
                     JOIN expense_amount_changes ch ON ch.expense_category_id = c.id
                     ORDER BY c.name, ch.effective_month
                     """)) {
            Map<String, Long> migrated = new java.util.LinkedHashMap<>();
            while (rows.next()) {
                migrated.put(rows.getString("name") + "@" + rows.getDate("effective_month").toLocalDate().toString().substring(0, 7), rows.getLong("amount"));
            }

            assertEquals(Map.of(
                    "Sinh hoạt phí@2026-11", 4_250_111L,
                    "Quỹ nuôi con@2026-11", 2_500_222L,
                    "Quỹ nuôi con@2027-01", 2_750_333L,
                    "Quỹ nuôi con@2027-04", 3_250_444L,
                    "Tiền nhà@2026-11", 3_750_555L,
                    "Tiền nhà@2027-02", 5_750_666L), migrated);
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var columns = statement.executeQuery("""
                     SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                     WHERE TABLE_NAME = 'ASSUMPTIONS' AND COLUMN_NAME IN
                     ('LIVING', 'CHILD_THROUGH_2026', 'CHILD_Q1_2027', 'CHILD_FROM_APR_2027',
                      'RENT_THROUGH_JAN_2027', 'RENT_FROM_FEB_2027')
                     """)) {
            columns.next();
            assertEquals(0, columns.getInt(1));
        }
    }
}
