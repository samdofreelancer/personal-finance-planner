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
import com.finance.planner.config.TestSessionConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;
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
        "server.servlet.session.cookie.secure=true",
        "spring.session.store-type=none"
})
@AutoConfigureMockMvc
@Import(TestSessionConfiguration.class)
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private HouseholdRepository householdRepository;
    @Autowired private HouseholdMemberRepository householdMemberRepository;
    @Autowired private HouseholdInvitationRepository invitationRepository;
    @Autowired private BootstrapLockRepository bootstrapLockRepository;
    @Autowired private SessionRepository<MapSession> sessionRepository;

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
    void bootstrapKeepsSelfRegistrationAvailableAfterMultipleAccountsExist() throws Exception {
        mockMvc.perform(get("/api/auth/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationAvailable").value(true));

        MvcResult first = registerInitial("owner@example.com");

        mockMvc.perform(get("/api/auth/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationAvailable").value(true))
                .andExpect(jsonPath("$.users").doesNotExist());

        MvcResult second = registerInitial("second-owner@example.com");
        JsonNode firstUser = objectMapper.readTree(first.getResponse().getContentAsString());
        JsonNode secondUser = objectMapper.readTree(second.getResponse().getContentAsString());
        long legacyHouseholdId = householdRepository.findFirstByInitialHouseholdTrue().orElseThrow().getId();
        org.junit.jupiter.api.Assertions.assertNotEquals(firstUser.get("householdId").asLong(), secondUser.get("householdId").asLong());
        assertNotEquals(legacyHouseholdId, firstUser.get("householdId").asLong());
        assertNotEquals(legacyHouseholdId, secondUser.get("householdId").asLong());
        org.junit.jupiter.api.Assertions.assertEquals(3, householdRepository.count());
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
        Cookie sessionCookie = sessionCookie(registration);
        String sessionId = decodeSessionId(sessionCookie);
        MapSession session = sessionRepository.findById(sessionId);
        assertNotNull(session);
        org.junit.jupiter.api.Assertions.assertEquals(Duration.ofMinutes(30), session.getMaxInactiveInterval());

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/logout").cookie(sessionCookie).with(csrf()))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertNull(sessionRepository.findById(sessionId));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionExpiresAfterThirtyMinutesOfInactivity() throws Exception {
        MvcResult registration = registerInitial("idle-timeout@example.com");
        Cookie sessionCookie = sessionCookie(registration);
        String sessionId = decodeSessionId(sessionCookie);
        MapSession session = sessionRepository.findById(sessionId);
        session.setLastAccessedTime(Instant.now().minus(Duration.ofMinutes(31)));
        sessionRepository.save(session);

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedActivityRefreshesTheSessionIdleExpiry() throws Exception {
        MvcResult registration = registerInitial("active-session@example.com");
        Cookie sessionCookie = sessionCookie(registration);
        String sessionId = decodeSessionId(sessionCookie);
        MapSession session = sessionRepository.findById(sessionId);
        Instant lastAccessBeforeActivity = Instant.now().minus(Duration.ofMinutes(20));
        session.setLastAccessedTime(lastAccessBeforeActivity);
        sessionRepository.save(session);

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk());

        Instant lastAccessAfterActivity = sessionRepository.findById(sessionId).getLastAccessedTime();
        org.junit.jupiter.api.Assertions.assertTrue(lastAccessAfterActivity.isAfter(lastAccessBeforeActivity));
    }

    @Test
    void authenticatedUserCanManageOwnProfileAndChangePassword() throws Exception {
        Cookie sessionCookie = sessionCookie(registerInitial("profile@example.com"));

        mockMvc.perform(get("/api/profile").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/profile")
                        .cookie(sessionCookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Planwise User\",\"avatarUrl\":\"https://cdn.example.com/avatar.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Planwise User"))
                .andExpect(jsonPath("$.avatarUrl").value("https://cdn.example.com/avatar.png"));

        Cookie otherUserSessionCookie = sessionCookie(registerInitial("other-profile@example.com"));
        mockMvc.perform(get("/api/profile").cookie(otherUserSessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("other-profile@example.com"))
                .andExpect(jsonPath("$.displayName").doesNotExist());

        mockMvc.perform(post("/api/profile/password").cookie(sessionCookie).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"correct-horse-battery-staple\",\"newPassword\":\"another-secure-password\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"profile@example.com\",\"password\":\"correct-horse-battery-staple\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"profile@example.com\",\"password\":\"another-secure-password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void profileEndpointsRequireAuthenticationAndRejectInvalidAvatarUrls() throws Exception {
        mockMvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        Cookie sessionCookie = sessionCookie(registerInitial("profile@example.com"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/profile")
                        .cookie(sessionCookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"User\",\"avatarUrl\":\"http://example.com/avatar.png\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordChangeRejectsAnIncorrectCurrentPasswordWithAnActionableError() throws Exception {
        Cookie sessionCookie = sessionCookie(registerInitial("profile@example.com"));
        mockMvc.perform(post("/api/profile/password").cookie(sessionCookie).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"incorrect-current-password\",\"newPassword\":\"another-secure-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu hiện tại không chính xác."));
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

        MapSession existingSession = sessionRepository.createSession();
        existingSession.setAttribute("pre-authentication", "preserve-unrelated-session-state");
        sessionRepository.save(existingSession);
        String previousSessionId = existingSession.getId();
        Cookie existingSessionCookie = new Cookie("SESSION", Base64.getUrlEncoder().withoutPadding()
                .encodeToString(previousSessionId.getBytes(StandardCharsets.UTF_8)));
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(existingSessionCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":" OWNER@EXAMPLE.COM ","password":"correct-horse-battery-staple"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authenticatedSessionCookie = sessionCookie(login);
        String authenticatedSessionId = decodeSessionId(authenticatedSessionCookie);
        assertNotEquals(previousSessionId, authenticatedSessionId, "Login must rotate the session ID");
        org.junit.jupiter.api.Assertions.assertEquals("preserve-unrelated-session-state",
                sessionRepository.findById(authenticatedSessionId).getAttribute("pre-authentication"));
        mockMvc.perform(get("/api/auth/me").cookie(authenticatedSessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void invitationIsSingleUseAndOnlyOwnerCanCreateOrRevokeInvitations() throws Exception {
        Cookie ownerSessionCookie = sessionCookie(registerInitial("owner@example.com"));
        MvcResult created = createInvitation(ownerSessionCookie);
        JsonNode invite = objectMapper.readTree(created.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String token = invite.get("token").asText();
        long invitationId = invite.get("id").asLong();
        mockMvc.perform(get("/api/household/invitations").cookie(ownerSessionCookie))
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
        Cookie memberSessionCookie = sessionCookie(memberRegistration);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InvitationRegistrationRequest(
                                "second-member@example.com", "correct-horse-battery-staple", token))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/household/invitations").cookie(memberSessionCookie).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"validityDays\":7}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/household/invitations").cookie(memberSessionCookie))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/household/invitations/{id}", invitationId).cookie(memberSessionCookie).with(csrf()))
                .andExpect(status().isForbidden());

        MvcResult secondInvite = createInvitation(ownerSessionCookie);
        long secondInvitationId = objectMapper.readTree(secondInvite.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(delete("/api/household/invitations/{id}", secondInvitationId).cookie(ownerSessionCookie).with(csrf()))
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

    private MvcResult createInvitation(Cookie ownerSessionCookie) throws Exception {
        return mockMvc.perform(post("/api/household/invitations").cookie(ownerSessionCookie).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validityDays\":7}"))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private Cookie sessionCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie, "Successful authentication must return a session cookie");
        return cookie;
    }

    private String decodeSessionId(Cookie cookie) {
        return new String(Base64.getUrlDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
    }

    private record InvitationRegistrationRequest(String email, String password, String invitationToken) {
    }
}
