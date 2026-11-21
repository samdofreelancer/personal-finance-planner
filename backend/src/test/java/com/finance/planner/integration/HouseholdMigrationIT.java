package com.finance.planner.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class HouseholdMigrationIT {

    @Test
    void addsHouseholdOwnershipWithoutChangingExistingFinancialRows() throws Exception {
        String url = "jdbc:h2:mem:household-migration-preserve;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            migrateLegacySchemaThroughV4(connection);
            seedAdditionalLegacyFinancialRows(connection);
            executeMigration(connection, "db/migration/V5__configurable_forecast_horizon_and_annual_raises.sql");
            executeMigration(connection, "db/migration/V6__replace_salary_raises_with_income_schedule.sql");

            List<List<Object>> assumptionsBefore = rows(connection, """
                    SELECT id, january_multiplier, starting_savings, forecast_years, created_at, updated_at
                    FROM assumptions ORDER BY id
                    """);
            List<List<Object>> loansBefore = rows(connection,
                    "SELECT id, name, opening_principal, created_at, updated_at FROM loans ORDER BY id");
            List<List<Object>> repaymentsBefore = rows(connection,
                    "SELECT id, loan_id, due_date, principal_amount, interest_amount FROM loan_repayments ORDER BY id");
            List<List<Object>> categoriesBefore = rows(connection,
                    "SELECT id, name, created_at, updated_at FROM expense_categories ORDER BY id");
            List<List<Object>> changesBefore = rows(connection,
                    "SELECT id, expense_category_id, effective_month, amount FROM expense_amount_changes ORDER BY id");

            executeMigration(connection, "db/migration/V7__add_household_authentication.sql");

            assertEquals(assumptionsBefore, rows(connection, """
                    SELECT id, january_multiplier, starting_savings, forecast_years, created_at, updated_at
                    FROM assumptions ORDER BY id
                    """));
            assertEquals(loansBefore, rows(connection,
                    "SELECT id, name, opening_principal, created_at, updated_at FROM loans ORDER BY id"));
            assertEquals(repaymentsBefore, rows(connection,
                    "SELECT id, loan_id, due_date, principal_amount, interest_amount FROM loan_repayments ORDER BY id"));
            assertEquals(categoriesBefore, rows(connection,
                    "SELECT id, name, created_at, updated_at FROM expense_categories ORDER BY id"));
            assertEquals(changesBefore, rows(connection,
                    "SELECT id, expense_category_id, effective_month, amount FROM expense_amount_changes ORDER BY id"));

            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM households"));
            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM bootstrap_lock"));
            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM assumptions WHERE household_id = 1"));
            assertEquals(2, scalar(connection, "SELECT COUNT(*) FROM loans WHERE household_id = 1"));
            assertEquals(4, scalar(connection, "SELECT COUNT(*) FROM expense_categories WHERE household_id = 1"));
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM users"));
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM household_members"));
        }
    }

    @Test
    void enforcesUniqueIdentityMembershipAndInvitationKeysAndHouseholdForeignKeys() throws Exception {
        String url = "jdbc:h2:mem:household-migration-constraints;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            migrateLegacySchemaThroughV4(connection);
            seedAdditionalLegacyFinancialRows(connection);
            executeMigration(connection, "db/migration/V5__configurable_forecast_horizon_and_annual_raises.sql");
            executeMigration(connection, "db/migration/V6__replace_salary_raises_with_income_schedule.sql");
            executeMigration(connection, "db/migration/V7__add_household_authentication.sql");

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO users (email, password_hash, created_at, updated_at)
                        VALUES ('owner@example.com', 'synthetic-hash', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                statement.executeUpdate("""
                        INSERT INTO household_members (household_id, user_id, role, created_at)
                        VALUES (1, 1, 'OWNER', CURRENT_TIMESTAMP)
                        """);
                statement.executeUpdate("""
                        INSERT INTO households (name, initial_household, initial_household_key, created_at)
                        VALUES ('Household 2', FALSE, NULL, CURRENT_TIMESTAMP)
                        """);

                assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO households (name, initial_household, initial_household_key, created_at)
                        VALUES ('Duplicate initial household', TRUE, 1, CURRENT_TIMESTAMP)
                        """));
                assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO assumptions
                            (household_id, january_multiplier, starting_savings, forecast_years, created_at, updated_at) VALUES (1, 1, 0, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """));
                assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO users (email, password_hash, created_at, updated_at)
                        VALUES ('owner@example.com', 'another-hash', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """));
                assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO users (email, password_hash, created_at, updated_at)
                        VALUES ('OWNER@EXAMPLE.COM', 'uppercase-hash', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """));
                assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO household_members (household_id, user_id, role, created_at)
                        VALUES (2, 1, 'MEMBER', CURRENT_TIMESTAMP)
                        """));
                statement.executeUpdate("""
                        INSERT INTO household_invitations
                            (household_id, created_by_user_id, token_hash, expires_at, created_at)
                        VALUES (1, 1, REPEAT('a', 64), CURRENT_TIMESTAMP + INTERVAL '7' DAY, CURRENT_TIMESTAMP)
                        """);
                assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO household_invitations
                            (household_id, created_by_user_id, token_hash, expires_at, created_at)
                        VALUES (1, 1, REPEAT('a', 64), CURRENT_TIMESTAMP + INTERVAL '7' DAY, CURRENT_TIMESTAMP)
                        """));
                assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO loans (name, opening_principal, created_at, updated_at, household_id)
                        VALUES ('Invalid household', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 999)
                        """));
            }
        }
    }

    private void migrateLegacySchemaThroughV4(Connection connection) throws SQLException {
        executeMigration(connection, "db/migration/V1__baseline_assumptions.sql");
        // H2 cannot parse V2's PostgreSQL data-modifying CTE; build its resulting legacy schema and rows.
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE loans (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        name VARCHAR(120) NOT NULL,
                        opening_principal BIGINT NOT NULL CHECK (opening_principal >= 0),
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE loan_repayments (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        loan_id BIGINT NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
                        due_date DATE NOT NULL,
                        principal_amount BIGINT NOT NULL CHECK (principal_amount >= 0),
                        interest_amount BIGINT NOT NULL CHECK (interest_amount >= 0)
                    )
                    """);
            statement.executeUpdate("INSERT INTO loans (name, opening_principal, created_at, updated_at) VALUES ('Legacy loan A', 120000000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
            statement.executeUpdate("""
                    INSERT INTO loan_repayments (loan_id, due_date, principal_amount, interest_amount)
                    VALUES (1, DATE '2026-11-30', 8800000, 1200000),
                           (1, DATE '2026-12-31', 8900000, 1100000),
                           (1, DATE '2027-01-31', 112300000, 1000000)
                    """);
        }
        // H2 does not support V3's multi-column DROP syntax; apply the same schema transition individually.
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE assumptions DROP COLUMN loan_principal");
            statement.executeUpdate("ALTER TABLE assumptions DROP COLUMN fixed_payment");
            statement.executeUpdate("ALTER TABLE assumptions DROP COLUMN nov_interest");
            statement.executeUpdate("ALTER TABLE assumptions DROP COLUMN dec_interest");
            statement.executeUpdate("ALTER TABLE assumptions DROP COLUMN payoff_interest");
            statement.executeUpdate("ALTER TABLE assumptions DROP COLUMN payoff_interest_confirmed");
        }
        executeMigration(connection, "db/migration/V4__create_flexible_expense_schedules.sql");
    }

    private void seedAdditionalLegacyFinancialRows(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE assumptions SET base_salary = 42123456, starting_savings = 30002222, raise_2027 = 0.0912");
            statement.executeUpdate("UPDATE loans SET name = 'Legacy loan A', opening_principal = 123456789 WHERE id = 1");
            statement.executeUpdate("UPDATE loan_repayments SET principal_amount = 7654321, interest_amount = 123456 WHERE loan_id = 1 AND due_date = DATE '2026-11-30'");
            statement.executeUpdate("""
                    INSERT INTO loans (name, opening_principal, created_at, updated_at)
                    VALUES ('Legacy loan B', 987654321, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO loan_repayments (loan_id, due_date, principal_amount, interest_amount)
                    SELECT id, DATE '2027-02-15', 456789, 12345 FROM loans WHERE name = 'Legacy loan B'
                    """);
            statement.executeUpdate("UPDATE expense_amount_changes SET amount = amount + 111 WHERE effective_month = DATE '2026-11-01'");
            statement.executeUpdate("""
                    INSERT INTO expense_categories (name, created_at, updated_at)
                    VALUES ('Custom category', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO expense_amount_changes (expense_category_id, effective_month, amount)
                    SELECT id, DATE '2026-11-01', 987654 FROM expense_categories WHERE name = 'Custom category'
                    """);
        }
    }

    private void executeMigration(Connection connection, String path) throws SQLException {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource(path));
    }

    private List<List<Object>> rows(Connection connection, String sql) throws SQLException {
        List<List<Object>> result = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            int columns = rows.getMetaData().getColumnCount();
            while (rows.next()) {
                List<Object> row = new ArrayList<>();
                for (int column = 1; column <= columns; column++) {
                    row.add(rows.getObject(column));
                }
                result.add(row);
            }
        }
        return result;
    }

    private long scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
