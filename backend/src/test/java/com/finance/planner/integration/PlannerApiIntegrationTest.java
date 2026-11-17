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
import com.finance.planner.repository.AssumptionRepository;
import java.math.BigDecimal;
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
        mockMvc.perform(post("/api/assumptions/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseSalary").value(42_000_000))
                .andExpect(jsonPath("$.raises.2027").value(0.08));

        mockMvc.perform(get("/api/assumptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.living").value(4_250_000))
                .andExpect(jsonPath("$.loanPrincipal").value(120_000_000))
                .andExpect(jsonPath("$.raises.2030").value(0.08));

        org.junit.jupiter.api.Assertions.assertEquals(1, assumptionRepository.count());
    }

    @Test
    void putUpdatesDatabaseAndGetReadsBackTheUpdatedValues() throws Exception {
        mockMvc.perform(post("/api/assumptions/seed"))
                .andExpect(status().isOk());

        AssumptionDTO updated = validAssumptions();
        updated.setBaseSalary(55_000_000L);
        updated.setLiving(6_000_000L);
        updated.setRentFromFeb2027(7_000_000L);
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
                .andExpect(jsonPath("$.living").value(6_000_000))
                .andExpect(jsonPath("$.rentFromFeb2027").value(7_000_000))
                .andExpect(jsonPath("$.raises.2028").value(0.06));

        org.junit.jupiter.api.Assertions.assertEquals(1, assumptionRepository.count());
    }

    @Test
    void rejectsInvalidPutAndLeavesPersistedAssumptionsUnchanged() throws Exception {
        mockMvc.perform(post("/api/assumptions/seed"))
                .andExpect(status().isOk());

        AssumptionDTO invalid = validAssumptions();
        invalid.setLiving(-1L);

        mockMvc.perform(put("/api/assumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("living phải là số VND nguyên không âm."));

        mockMvc.perform(get("/api/assumptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.living").value(4_250_000));
    }

    @Test
    void forecastEndpointReturnsFiftyMonthsAndAnnualSummaries() throws Exception {
        mockMvc.perform(get("/api/forecast")
                        .param("baseSalary", "42000000")
                        .param("januaryMultiplier", "2.0")
                        .param("living", "4250000")
                        .param("childThrough2026", "2500000")
                        .param("childQ12027", "2750000")
                        .param("childFromApr2027", "3250000")
                        .param("rentThroughJan2027", "3750000")
                        .param("rentFromFeb2027", "5750000")
                        .param("loanPrincipal", "120000000")
                        .param("fixedPayment", "10000000")
                        .param("novInterest", "1200000")
                        .param("decInterest", "1100000")
                        .param("payoffInterest", "1000000")
                        .param("payoffInterestConfirmed", "false")
                        .param("startingSavings", "30000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months", hasSize(50)))
                .andExpect(jsonPath("$.months[0].year").value(2026))
                .andExpect(jsonPath("$.months[0].month").value(11))
                .andExpect(jsonPath("$.annual", hasSize(5)));
    }

    @Test
    void csvExportIncludesDownloadHeadersAndForecastSections() throws Exception {
        mockMvc.perform(get("/api/export/csv")
                        .param("baseSalary", "42000000")
                        .param("januaryMultiplier", "2.0")
                        .param("living", "4250000")
                        .param("childThrough2026", "2500000")
                        .param("childQ12027", "2750000")
                        .param("childFromApr2027", "3250000")
                        .param("rentThroughJan2027", "3750000")
                        .param("rentFromFeb2027", "5750000")
                        .param("loanPrincipal", "120000000")
                        .param("fixedPayment", "10000000")
                        .param("novInterest", "1200000")
                        .param("decInterest", "1100000")
                        .param("payoffInterest", "1000000")
                        .param("payoffInterestConfirmed", "false")
                        .param("startingSavings", "30000000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ke-hoach-tai-chinh")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dự báo theo tháng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tổng hợp theo năm")));
    }

    private AssumptionDTO validAssumptions() {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setBaseSalary(42_000_000L);
        dto.setJanuaryMultiplier(new BigDecimal("2.0"));
        dto.setLiving(4_250_000L);
        dto.setChildThrough2026(2_500_000L);
        dto.setChildQ12027(2_750_000L);
        dto.setChildFromApr2027(3_250_000L);
        dto.setRentThroughJan2027(3_750_000L);
        dto.setRentFromFeb2027(5_750_000L);
        dto.setLoanPrincipal(120_000_000L);
        dto.setFixedPayment(10_000_000L);
        dto.setNovInterest(1_200_000L);
        dto.setDecInterest(1_100_000L);
        dto.setPayoffInterest(1_000_000L);
        dto.setPayoffInterestConfirmed(false);
        dto.setStartingSavings(30_000_000L);
        dto.setRaises(Map.of(
                2027, new BigDecimal("0.08"),
                2028, new BigDecimal("0.08"),
                2029, new BigDecimal("0.08"),
                2030, new BigDecimal("0.08")));
        return dto;
    }
}
