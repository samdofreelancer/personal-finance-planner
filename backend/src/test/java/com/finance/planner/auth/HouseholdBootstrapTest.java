package com.finance.planner.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.finance.planner.household.BootstrapLock;
import com.finance.planner.household.BootstrapLockRepository;
import com.finance.planner.household.Household;
import com.finance.planner.household.HouseholdInvitation;
import com.finance.planner.household.HouseholdInvitationRepository;
import com.finance.planner.household.HouseholdMemberRepository;
import com.finance.planner.household.HouseholdRepository;
import com.finance.planner.household.HouseholdContext;
import com.finance.planner.household.HouseholdRole;
import com.finance.planner.household.HouseholdMember;
import com.finance.planner.household.InvitationService;
import com.finance.planner.household.InvitationToken;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:planner-household-bootstrap;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
class HouseholdBootstrapTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private HouseholdRepository householdRepository;
    @Autowired private HouseholdMemberRepository householdMemberRepository;
    @Autowired private HouseholdInvitationRepository invitationRepository;
    @Autowired private InvitationService invitationService;
    @Autowired private BootstrapLockRepository bootstrapLockRepository;

    @BeforeEach
    void provisionInitialHouseholdAndBootstrapLock() {
        householdMemberRepository.deleteAll();
        invitationRepository.deleteAll();
        userRepository.deleteAll();
        householdRepository.deleteAll();
        bootstrapLockRepository.deleteAll();
        bootstrapLockRepository.save(new BootstrapLock(1L));
        householdRepository.save(new Household("Gia đình", true));
    }

    @Test
    void concurrentInitialRegistrationsCreateExactlyOneOwnerAndMembership() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> attemptInitialRegistration(
                    "first@example.com", ready, start));
            Future<Boolean> second = executor.submit(() -> attemptInitialRegistration(
                    "second@example.com", ready, start));

            ready.await();
            start.countDown();

            assertNotEquals(first.get(), second.get(), "Exactly one concurrent registration must win");
            assertEquals(1, userRepository.count());
            assertEquals(1, householdMemberRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentInvitationConsumptionCreatesExactlyOneMember() throws Exception {
        Household household = householdRepository.findFirstByInitialHouseholdTrue().orElseThrow();
        User owner = userRepository.save(new User("owner@example.com", "existing-password-hash"));
        householdMemberRepository.save(new com.finance.planner.household.HouseholdMember(
                household, owner, com.finance.planner.household.HouseholdRole.OWNER));
        String token = "concurrent-invitation-token";
        invitationRepository.save(new HouseholdInvitation(household, owner, InvitationToken.hash(token),
                Instant.now().plusSeconds(60)));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> attemptInvitationRegistration(
                    "first-member@example.com", token, ready, start));
            Future<Boolean> second = executor.submit(() -> attemptInvitationRegistration(
                    "second-member@example.com", token, ready, start));

            ready.await();
            start.countDown();

            assertNotEquals(first.get(), second.get(), "Exactly one invitation consumer must win");
            assertEquals(2, userRepository.count(), "The owner and one invited user should exist");
            assertEquals(2, householdMemberRepository.count());
            assertNotEquals(null, invitationRepository.findAll().get(0).getConsumedAt());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentInvitationRevocationAndConsumptionCannotBothWin() throws Exception {
        Household household = householdRepository.findFirstByInitialHouseholdTrue().orElseThrow();
        User owner = userRepository.save(new User("owner@example.com", "existing-password-hash"));
        householdMemberRepository.save(new HouseholdMember(household, owner, HouseholdRole.OWNER));
        String token = "revoke-consume-race-token";
        HouseholdInvitation invitation = invitationRepository.save(new HouseholdInvitation(
                household, owner, InvitationToken.hash(token), Instant.now().plusSeconds(60)));
        HouseholdContext ownerContext = new HouseholdContext(household.getId(), owner.getId(), HouseholdRole.OWNER);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> revoke = executor.submit(() -> attemptRevoke(invitation.getId(), ownerContext, ready, start));
            Future<Boolean> consume = executor.submit(() -> attemptInvitationRegistration(
                    "race-member@example.com", token, ready, start));
            ready.await();
            start.countDown();

            assertNotEquals(revoke.get(), consume.get(), "Exactly one of revoke or consume must win");
            HouseholdInvitation result = invitationRepository.findById(invitation.getId()).orElseThrow();
            boolean revoked = result.getRevokedAt() != null;
            boolean consumed = result.getConsumedAt() != null;
            org.junit.jupiter.api.Assertions.assertNotEquals(revoked, consumed,
                    "An invitation cannot end up both revoked and consumed, or neither");
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean attemptInitialRegistration(String email, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        start.await();
        try {
            authService.registerInitial(new RegisterRequest(email, "correct-horse-battery-staple"));
            return true;
        } catch (IllegalStateException expectedForTheLosingRequest) {
            return false;
        }
    }

    private boolean attemptInvitationRegistration(String email, String token, CountDownLatch ready,
                                                  CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            authService.registerWithInvitation(new RegisterRequest(email, "correct-horse-battery-staple"), token);
            return true;
        } catch (IllegalArgumentException expectedForTheLosingRequest) {
            return false;
        }
    }

    private boolean attemptRevoke(Long invitationId, HouseholdContext context, CountDownLatch ready,
                                   CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            invitationService.revoke(context, invitationId);
            return true;
        } catch (IllegalStateException expectedForTheLosingRequest) {
            return false;
        }
    }
}
