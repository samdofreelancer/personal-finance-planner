package com.finance.planner.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.planner.household.BootstrapLock;
import com.finance.planner.household.BootstrapLockRepository;
import com.finance.planner.household.Household;
import com.finance.planner.household.HouseholdInvitationRepository;
import com.finance.planner.household.HouseholdMemberRepository;
import com.finance.planner.household.HouseholdRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:planner-auth-api;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "server.servlet.session.cookie.secure=true"
})
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private HouseholdRepository householdRepository;
    @Autowired private HouseholdMemberRepository householdMemberRepository;
    @Autowired private HouseholdInvitationRepository invitationRepository;
    @Autowired private BootstrapLockRepository bootstrapLockRepository;

    @Value("${server.servlet.session.cookie.http-only}")
    private boolean sessionCookieHttpOnly;

    @Value("${server.servlet.session.cookie.same-site}")
    private String sessionCookieSameSite;

    @Value("${server.servlet.session.cookie.secure}")
    private boolean sessionCookieSecure;

    @BeforeEach
    void resetSyntheticIdentityData() {
        invitationRepository.deleteAll();
        householdMemberRepository.deleteAll();
        userRepository.deleteAll();
        householdRepository.deleteAll();
        bootstrapLockRepository.deleteAll();
        bootstrapLockRepository.save(new BootstrapLock(1L));
        householdRepository.save(new Household("Gia đình", true));
    }

    @Test
    void bootstrapIsPublicAndOnlyReportsWhetherInitialRegistrationIsAvailable() throws Exception {
        mockMvc.perform(get("/api/auth/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationAvailable").value(true));

        registerInitial("owner@example.com");

        mockMvc.perform(get("/api/auth/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationAvailable").value(false))
                .andExpect(jsonPath("$.users").doesNotExist());

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "second-owner@example.com", "correct-horse-battery-staple"))))
                .andExpect(status().isConflict());
    }

    @Test
    void apiDoesNotEnableWildcardCrossOriginAccess() throws Exception {
        mockMvc.perform(get("/api/auth/bootstrap").header("Origin", "https://untrusted.example"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void registrationCreatesOwnerSessionAndLogoutInvalidatesIt() throws Exception {
        MvcResult registration = registerInitial("owner@example.com");
        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> session.getAttribute("SPRING_SECURITY_CONTEXT"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsWrongPasswordAndRotatesAnExistingSessionId() throws Exception {
        registerInitial("owner@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());

        MockHttpSession existingSession = new MockHttpSession();
        existingSession.setAttribute("pre-authentication", "preserve-unrelated-session-state");
        String previousSessionId = existingSession.getId();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .session(existingSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":" OWNER@EXAMPLE.COM ","password":"correct-horse-battery-staple"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession authenticatedSession = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(authenticatedSession);
        assertNotEquals(previousSessionId, authenticatedSession.getId(), "Login must rotate the session ID");
        org.junit.jupiter.api.Assertions.assertEquals("preserve-unrelated-session-state",
                authenticatedSession.getAttribute("pre-authentication"));
        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void invitationIsSingleUseAndOnlyOwnerCanCreateOrRevokeInvitations() throws Exception {
        MockHttpSession ownerSession = (MockHttpSession) registerInitial("owner@example.com")
                .getRequest().getSession(false);
        MvcResult created = createInvitation(ownerSession);
        JsonNode invite = objectMapper.readTree(created.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String token = invite.get("token").asText();
        long invitationId = invite.get("id").asLong();
        mockMvc.perform(get("/api/household/invitations").session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].tokenHash").doesNotExist());

        MvcResult memberRegistration = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InvitationRegistrationRequest(
                                "member@example.com", "correct-horse-battery-staple", token))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andReturn();
        MockHttpSession memberSession = (MockHttpSession) memberRegistration.getRequest().getSession(false);
        assertNotNull(memberSession);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InvitationRegistrationRequest(
                                "second-member@example.com", "correct-horse-battery-staple", token))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/household/invitations").session(memberSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"validityDays\":7}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/household/invitations").session(memberSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/household/invitations/{id}", invitationId).session(memberSession).with(csrf()))
                .andExpect(status().isForbidden());

        MvcResult secondInvite = createInvitation(ownerSession);
        long secondInvitationId = objectMapper.readTree(secondInvite.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(delete("/api/household/invitations/{id}", secondInvitationId).session(ownerSession).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void throttlesRepeatedFailedLoginsForTheSameIdentifierAndClient() throws Exception {
        String body = """
                {"email":"rate-limit@example.com","password":"wrong-password"}
                """;
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void sessionCookieConfigurationIsHttpOnlyAndSameSiteLax() {
        org.junit.jupiter.api.Assertions.assertTrue(sessionCookieHttpOnly);
        org.junit.jupiter.api.Assertions.assertEquals("lax", sessionCookieSameSite);
        org.junit.jupiter.api.Assertions.assertTrue(sessionCookieSecure);
    }

    @Test
    void csrfCookieIsReadableSecureAndSameSiteLax() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        jakarta.servlet.http.Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        org.junit.jupiter.api.Assertions.assertFalse(csrfCookie.isHttpOnly());
        org.junit.jupiter.api.Assertions.assertTrue(csrfCookie.getSecure());
        org.junit.jupiter.api.Assertions.assertEquals("/", csrfCookie.getPath());
        org.junit.jupiter.api.Assertions.assertEquals("Lax", csrfCookie.getAttribute("SameSite"));
    }

    private MvcResult registerInitial(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email, "correct-horse-battery-staple"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn();
    }

    private MvcResult createInvitation(MockHttpSession ownerSession) throws Exception {
        return mockMvc.perform(post("/api/household/invitations").session(ownerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validityDays\":7}"))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private record InvitationRegistrationRequest(String email, String password, String invitationToken) {
    }
}
