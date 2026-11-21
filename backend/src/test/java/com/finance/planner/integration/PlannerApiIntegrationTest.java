package com.finance.planner.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ExpenseDTO;
import com.finance.planner.dto.ExpenseChangeDTO;
import com.finance.planner.repository.AssumptionRepository;
import com.finance.planner.service.ExpenseScheduleService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:planner-integration;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class PlannerApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AssumptionRepository assumptionRepository;

    @Test
    void seedThenGetReturnsPersistedDefaultAssumptions() throws Exception {
        mockMvc.perform(put("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Dữ liệu cũ\",\"openingPrincipal\":999,\"repayments\":[]}]"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assumptions/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseSalary").value(42_000_000))
                .andExpect(jsonPath("$.raises.2027").value(0.08));

        mockMvc.perform(get("/api/assumptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses[0].changes[0].amount").value(4_250_000))
                .andExpect(jsonPath("$.raises.2030").value(0.08));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].openingPrincipal").value(120_000_000))
                .andExpect(jsonPath("$[0].repayments", hasSize(3)))
                .andExpect(jsonPath("$[0].repayments[0].principalAmount").value(8_800_000))
                .andExpect(jsonPath("$[0].repayments[2].interestAmount").value(1_000_000));

        org.junit.jupiter.api.Assertions.assertEquals(1, assumptionRepository.count());
    }

    @Test
    void putUpdatesDatabaseAndGetReadsBackTheUpdatedValues() throws Exception {
        mockMvc.perform(post("/api/assumptions/seed"))
                .andExpect(status().isOk());

        AssumptionDTO updated = validAssumptions();
        updated.setBaseSalary(55_000_000L);
        updated.setExpenses(List.of(
                new ExpenseDTO("Sinh hoạt phí", List.of(new ExpenseChangeDTO("2026-11", 6_000_000L))),
                new ExpenseDTO("Tiền nhà", List.of(
                        new ExpenseChangeDTO("2026-11", 3_750_000L),
                        new ExpenseChangeDTO("2027-02", 7_000_000L)))));
        updated.setRaises(Map.of(
                2027, new BigDecimal("0.05"),
                2028, new BigDecimal("0.06"),
                2029, new BigDecimal("0.07"),
                2030, new BigDecimal("0.08")));

        mockMvc.perform(put("/api/assumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseSalary").value(55_000_000))
                .andExpect(jsonPath("$.raises.2029").value(0.07));

        mockMvc.perform(get("/api/assumptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseSalary").value(55_000_000))
                .andExpect(jsonPath("$.expenses[0].changes[0].amount").value(6_000_000))
                .andExpect(jsonPath("$.expenses[1].changes[1].amount").value(7_000_000))
                .andExpect(jsonPath("$.raises.2028").value(0.06));

        org.junit.jupiter.api.Assertions.assertEquals(1, assumptionRepository.count());
    }

    @Test
    void rejectsInvalidPutAndLeavesPersistedAssumptionsUnchanged() throws Exception {
        mockMvc.perform(post("/api/assumptions/seed"))
                .andExpect(status().isOk());

        AssumptionDTO invalid = validAssumptions();
        invalid.setExpenses(List.of(new ExpenseDTO("Sinh hoạt phí", List.of(
                new ExpenseChangeDTO("2026-11", -1L)))));

        mockMvc.perform(put("/api/assumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mốc chi phí phải có tháng hiệu lực và số VND nguyên không âm."));

        mockMvc.perform(get("/api/assumptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses[0].changes[0].amount").value(4_250_000));
    }

    @Test
    void forecastEndpointReturnsFiftyMonthsAndAnnualSummaries() throws Exception {
        mockMvc.perform(post("/api/forecast").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validAssumptions())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months", hasSize(50)))
                .andExpect(jsonPath("$.months[0].year").value(2026))
                .andExpect(jsonPath("$.months[0].month").value(11))
                .andExpect(jsonPath("$.annual", hasSize(5)));
    }

    @Test
    void csvExportIncludesDownloadHeadersAndForecastSections() throws Exception {
        mockMvc.perform(put("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Khoản vay CSV\",\"openingPrincipal\":1000,\"repayments\":["
                                + "{\"dueDate\":\"2026-11-10\",\"principalAmount\":100,\"interestAmount\":10}]}]"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/export/csv").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validAssumptions())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ke-hoach-tai-chinh")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dự báo theo tháng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tổng hợp theo năm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("42000000,10500000,")));
    }

    @Test
    void persistsUserDefinedExpenseCategoriesAndForecastsEffectiveMonthlyAmounts() throws Exception {
        String assumptions = """
                {
                  "baseSalary": 1000,
                  "januaryMultiplier": 2.0,
                  "startingSavings": 0,
                  "raises": {"2027":0,"2028":0,"2029":0,"2030":0},
                  "expenses": [
                    {"name":"Học phí","changes":[
                      {"effectiveMonth":"2026-11","amount":100},
                      {"effectiveMonth":"2027-01","amount":200}
                    ]},
                    {"name":"Bảo hiểm","changes":[
                      {"effectiveMonth":"2026-11","amount":50}
                    ]}
                  ]
                }
                """;

        mockMvc.perform(put("/api/assumptions").contentType(MediaType.APPLICATION_JSON).content(assumptions))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses", hasSize(2)))
                .andExpect(jsonPath("$.expenses[0].name").value("Học phí"))
                .andExpect(jsonPath("$.expenses[0].changes[1].effectiveMonth").value("2027-01"));

        mockMvc.perform(get("/api/assumptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses", hasSize(2)))
                .andExpect(jsonPath("$.expenses[1].changes[0].amount").value(50));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/forecast")
                        .contentType(MediaType.APPLICATION_JSON).content(assumptions))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[0].recurringExpenses").value(150))
                .andExpect(jsonPath("$.months[0].totalExpenses").value(150))
                .andExpect(jsonPath("$.months[2].recurringExpenses").value(250))
                .andExpect(jsonPath("$.months[2].expenseBreakdown", hasSize(2)));
    }

    @Test
    void rejectsInvalidExpenseScheduleWithoutOverwritingExistingCategories() throws Exception {
        String valid = """
                {"baseSalary":1,"januaryMultiplier":1,"startingSavings":0,
                 "raises":{"2027":0,"2028":0,"2029":0,"2030":0},
                 "expenses":[{"name":"Học phí","changes":[{"effectiveMonth":"2026-11","amount":100}]}]}
                """;
        mockMvc.perform(put("/api/assumptions").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isOk());

        String invalid = valid.replace("2026-11", "2026-13");
        mockMvc.perform(put("/api/assumptions").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/assumptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses[0].changes[0].effectiveMonth").value("2026-11"));
    }

    @Test
    void savesAndReadsMultipleLoansWithIndependentRepaymentEntries() throws Exception {
        String loans = """
                [
                  {"name":"Vay mua nhà","openingPrincipal":1000,"repayments":[
                    {"dueDate":"2026-11-10","principalAmount":100,"interestAmount":10},
                    {"dueDate":"2026-11-25","principalAmount":200,"interestAmount":20},
                    {"dueDate":"2026-12-05","principalAmount":50,"interestAmount":5}
                  ]},
                  {"name":"Vay cá nhân","openingPrincipal":500,"repayments":[
                    {"dueDate":"2026-11-20","principalAmount":50,"interestAmount":8}
                  ]}
                ]
                """;

        mockMvc.perform(put("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loans))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].repayments", hasSize(3)))
                .andExpect(jsonPath("$[1].name").value("Vay cá nhân"));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].repayments[1].principalAmount").value(200));
    }

    @Test
    void rejectsNegativeRepaymentAmountsWithoutChangingSavedLoans() throws Exception {
        String valid = "[{\"name\":\"Khoản vay\",\"openingPrincipal\":1000,\"repayments\":["
                + "{\"dueDate\":\"2026-11-10\",\"principalAmount\":100,\"interestAmount\":10}]}]";
        mockMvc.perform(put("/api/loans").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isOk());

        String invalid = "[{\"name\":\"Khoản vay\",\"openingPrincipal\":1000,\"repayments\":["
                + "{\"dueDate\":\"2026-11-10\",\"principalAmount\":-1,\"interestAmount\":10}]}]";
        mockMvc.perform(put("/api/loans").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repayments[0].principalAmount").value(100));
    }

    @Test
    void updatesExistingLoanAndRepaymentWithoutChangingTheirIdentityAndSupportsDeletion() throws Exception {
        String initial = "[{\"name\":\"Khoản vay cũ\",\"openingPrincipal\":1000,\"repayments\":["
                + "{\"dueDate\":\"2026-11-10\",\"principalAmount\":100,\"interestAmount\":10}]}]";
        String createdBody = mockMvc.perform(put("/api/loans").contentType(MediaType.APPLICATION_JSON).content(initial))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var created = objectMapper.readTree(createdBody).get(0);
        long loanId = created.get("id").asLong();
        long repaymentId = created.get("repayments").get(0).get("id").asLong();

        String updated = "[{\"id\":" + loanId + ",\"name\":\"Khoản vay đã sửa\",\"openingPrincipal\":900,\"repayments\":["
                + "{\"id\":" + repaymentId + ",\"dueDate\":\"2026-11-15\",\"principalAmount\":200,\"interestAmount\":15}]}]";
        mockMvc.perform(put("/api/loans").contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(loanId))
                .andExpect(jsonPath("$[0].name").value("Khoản vay đã sửa"))
                .andExpect(jsonPath("$[0].repayments[0].id").value(repaymentId))
                .andExpect(jsonPath("$[0].repayments[0].principalAmount").value(200));

        mockMvc.perform(put("/api/loans").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void forecastAggregatesSavedRepaymentsByMonthAcrossLoans() throws Exception {
        String loans = "[{\"name\":\"Khoản vay A\",\"openingPrincipal\":250,\"repayments\":["
                + "{\"dueDate\":\"2026-11-10\",\"principalAmount\":100,\"interestAmount\":10},"
                + "{\"dueDate\":\"2026-11-25\",\"principalAmount\":200,\"interestAmount\":20},"
                + "{\"dueDate\":\"2026-12-05\",\"principalAmount\":50,\"interestAmount\":5}]} ,"
                + "{\"name\":\"Khoản vay B\",\"openingPrincipal\":500,\"repayments\":["
                + "{\"dueDate\":\"2026-11-20\",\"principalAmount\":50,\"interestAmount\":8}]}]";
        mockMvc.perform(put("/api/loans").contentType(MediaType.APPLICATION_JSON).content(loans))
                .andExpect(status().isOk());

        AssumptionDTO forecastInput = validAssumptions();
        forecastInput.setBaseSalary(1_000L);
        forecastInput.setStartingSavings(0L);
        forecastInput.setExpenses(List.of(new ExpenseDTO("Sinh hoạt", List.of(
                new ExpenseChangeDTO("2026-11", 100L)))));
        forecastInput.setRaises(Map.of(2027, BigDecimal.ZERO, 2028, BigDecimal.ZERO,
                2029, BigDecimal.ZERO, 2030, BigDecimal.ZERO));
        mockMvc.perform(post("/api/forecast").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(forecastInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[0].openingPrincipal").value(750))
                .andExpect(jsonPath("$.months[0].principalPaid").value(300))
                .andExpect(jsonPath("$.months[0].interestPaid").value(38))
                .andExpect(jsonPath("$.months[0].loanPayment").value(338))
                .andExpect(jsonPath("$.months[1].principalPaid").value(0))
                .andExpect(jsonPath("$.months[1].interestPaid").value(5));
    }

    private AssumptionDTO validAssumptions() {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setBaseSalary(42_000_000L);
        dto.setJanuaryMultiplier(new BigDecimal("2.0"));
        dto.setExpenses(ExpenseScheduleService.defaultExpenses());
        dto.setStartingSavings(30_000_000L);
        dto.setRaises(Map.of(
                2027, new BigDecimal("0.08"),
                2028, new BigDecimal("0.08"),
                2029, new BigDecimal("0.08"),
                2030, new BigDecimal("0.08")));
        return dto;
    }
}
