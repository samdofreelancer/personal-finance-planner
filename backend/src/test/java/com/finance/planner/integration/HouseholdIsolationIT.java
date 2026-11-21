package com.finance.planner.integration;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finance.planner.auth.RegisterRequest;
import com.finance.planner.auth.User;
import com.finance.planner.auth.UserRepository;
import com.finance.planner.household.BootstrapLock;
import com.finance.planner.household.BootstrapLockRepository;
import com.finance.planner.household.Household;
import com.finance.planner.household.HouseholdInvitationRepository;
import com.finance.planner.household.HouseholdMember;
import com.finance.planner.household.HouseholdMemberRepository;
import com.finance.planner.household.HouseholdRepository;
import com.finance.planner.household.HouseholdRole;
import com.finance.planner.repository.AssumptionRepository;
import com.finance.planner.repository.ExpenseCategoryRepository;
import com.finance.planner.repository.LoanRepository;
import com.finance.planner.repository.IncomeAmountChangeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:planner-household-isolation;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class HouseholdIsolationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private HouseholdRepository householdRepository;
    @Autowired private HouseholdMemberRepository householdMemberRepository;
    @Autowired private HouseholdInvitationRepository invitationRepository;
    @Autowired private BootstrapLockRepository bootstrapLockRepository;
    @Autowired private AssumptionRepository assumptionRepository;
    @Autowired private IncomeAmountChangeRepository incomeAmountChangeRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private ExpenseCategoryRepository expenseCategoryRepository;

    @BeforeEach
    void resetSyntheticHouseholdData() {
        invitationRepository.deleteAll();
        householdMemberRepository.deleteAll();
        userRepository.deleteAll();
        loanRepository.deleteAll();
        expenseCategoryRepository.deleteAll();
        incomeAmountChangeRepository.deleteAll();
        assumptionRepository.deleteAll();
        householdRepository.deleteAll();
        bootstrapLockRepository.deleteAll();
        bootstrapLockRepository.save(new BootstrapLock(1L));
        householdRepository.save(new Household("Household One", true));
    }

    @Test
    void assumptionsExpensesLoansForecastAndCsvAreIsolatedByHousehold() throws Exception {
        MockHttpSession firstSession = registerInitialOwner("owner-one@example.com");
        MockHttpSession secondSession = createAndLoginSecondHouseholdOwner();

        saveAssumptions(firstSession, 1_000L, "Food One", 100L);
        saveAssumptions(secondSession, 2_000L, "Food Two", 200L);
        saveLoans(firstSession, "Loan One", 1_000L, 100L, 10L);
        saveLoans(secondSession, "Loan Two", 2_000L, 200L, 20L);

        mockMvc.perform(get("/api/assumptions").session(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeChanges[0].amount").value(1_000))
                .andExpect(jsonPath("$.expenses[0].name").value("Food One"));
        mockMvc.perform(get("/api/assumptions").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeChanges[0].amount").value(2_000))
                .andExpect(jsonPath("$.expenses[0].name").value("Food Two"));

        MvcResult firstLoans = mockMvc.perform(get("/api/loans").session(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Loan One"))
                .andReturn();
        mockMvc.perform(get("/api/loans").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Loan Two"));

        String firstAssumptions = assumptionsJson(1_000L, "Food One", 100L);
        String secondAssumptions = assumptionsJson(2_000L, "Food Two", 200L);
        mockMvc.perform(post("/api/forecast").session(firstSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(firstAssumptions))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[0].loanPayment").value(110));
        mockMvc.perform(post("/api/forecast").session(secondSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(secondAssumptions))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[0].loanPayment").value(220));

        String firstCsv = mockMvc.perform(post("/api/export/csv").session(firstSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(firstAssumptions))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String secondCsv = mockMvc.perform(post("/api/export/csv").session(secondSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(secondAssumptions))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(firstCsv.contains(",110,"));
        org.junit.jupiter.api.Assertions.assertTrue(secondCsv.contains(",220,"));
        assertNotEquals(firstCsv, secondCsv);

        org.junit.jupiter.api.Assertions.assertEquals(2, assumptionRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(2, loanRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(2, expenseCategoryRepository.count());
    }

    @Test
    void foreignLoanIdsAreRejectedAndSeedResetDoesNotTouchAnotherHousehold() throws Exception {
        MockHttpSession firstSession = registerInitialOwner("owner-one@example.com");
        MockHttpSession secondSession = createAndLoginSecondHouseholdOwner();
        MvcResult firstLoanResult = saveLoans(firstSession, "Loan One", 1_000L, 100L, 10L);
        MvcResult secondLoanResult = saveLoans(secondSession, "Loan Two", 2_000L, 200L, 20L);
        long firstLoanId = objectMapper.readTree(firstLoanResult.getResponse().getContentAsString()).get(0).get("id").asLong();
        JsonNode secondLoan = objectMapper.readTree(secondLoanResult.getResponse().getContentAsString()).get(0);
        long foreignLoanId = secondLoan.get("id").asLong();
        long foreignRepaymentId = secondLoan.get("repayments").get(0).get("id").asLong();

        mockMvc.perform(put("/api/loans").session(firstSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":" + foreignLoanId
                                + ",\"name\":\"Attempted takeover\",\"openingPrincipal\":1,\"repayments\":[]}]"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/loans").session(firstSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":" + firstLoanId
                                + ",\"name\":\"Loan One\",\"openingPrincipal\":1000,\"repayments\":[{\"id\":"
                                + foreignRepaymentId
                                + ",\"dueDate\":\"2026-11-30\",\"principalAmount\":100,\"interestAmount\":10}]}]"))
                .andExpect(status().isBadRequest());

        saveAssumptions(firstSession, 1_000L, "Food One", 100L);
        saveAssumptions(secondSession, 2_000L, "Food Two", 200L);
        mockMvc.perform(post("/api/assumptions/seed").session(firstSession).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/assumptions").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeChanges[0].amount").value(2_000))
                .andExpect(jsonPath("$.expenses[0].name").value("Food Two"));
        mockMvc.perform(get("/api/loans").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Loan Two"));
    }

    private MockHttpSession registerInitialOwner(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email, "correct-horse-battery-staple"))))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private MockHttpSession createAndLoginSecondHouseholdOwner() throws Exception {
        Household household = householdRepository.save(new Household("Household Two", false));
        User user = userRepository.save(new User("owner-two@example.com",
                passwordEncoder.encode("correct-horse-battery-staple")));
        householdMemberRepository.save(new HouseholdMember(household, user, HouseholdRole.OWNER));

        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner-two@example.com","password":"correct-horse-battery-staple"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private void saveAssumptions(MockHttpSession session, long salary, String expenseName, long amount) throws Exception {
        mockMvc.perform(put("/api/assumptions").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assumptionsJson(salary, expenseName, amount)))
                .andExpect(status().isOk());
    }

    private String assumptionsJson(long salary, String expenseName, long amount) {
        return """
                {"householdId":999,"incomeChanges":[{"effectiveMonth":"2026-11","amount":%d}],"forecastYears":4,"januaryMultiplier":1,"startingSavings":0,
                 "expenses":[{"name":"%s","changes":[{"effectiveMonth":"2026-11","amount":%d}]}]}
                """.formatted(salary, expenseName, amount);
    }

    private MvcResult saveLoans(MockHttpSession session, String name, long principal,
                                long principalPayment, long interestPayment) throws Exception {
        return mockMvc.perform(put("/api/loans").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"householdId\":999,\"name\":\"" + name + "\",\"openingPrincipal\":" + principal
                                + ",\"repayments\":[{\"dueDate\":\"2026-11-30\",\"principalAmount\":"
                                + principalPayment + ",\"interestAmount\":" + interestPayment + "}]}]"))
                .andExpect(status().isOk())
                .andReturn();
    }
}
