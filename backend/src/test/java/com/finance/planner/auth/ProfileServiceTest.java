package com.finance.planner.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private ProfileService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ProfileService(userRepository, encoder);
        user = new User("profile@example.com", encoder.encode("correct-horse-battery-staple"));
        user.setId(7L);
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    void readsAccountMetadataWithoutExposingPasswordHash() {
        ProfileResponse response = service.getProfile(7L);
        assertEquals("profile@example.com", response.email());
        assertEquals(user.getCreatedAt(), response.createdAt());
        assertNull(response.displayName());
    }

    @Test
    void updatesDisplayNameAndAcceptsOnlyHttpsAvatarUrl() {
        ProfileResponse response = service.updateProfile(7L,
                new UpdateProfileRequest("  Ginseng  ", "https://cdn.example.com/avatar.png"));
        assertEquals("Ginseng", response.displayName());
        assertEquals("https://cdn.example.com/avatar.png", response.avatarUrl());

        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(7L,
                new UpdateProfileRequest("Name", "http://cdn.example.com/avatar.png")));
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(7L,
                new UpdateProfileRequest("Name", "https://user:pass@cdn.example.com/avatar.png")));
    }

    @Test
    void changesPasswordOnlyAfterVerifyingCurrentPasswordAndEnforcingPolicy() {
        service.changePassword(7L, new ChangePasswordRequest("correct-horse-battery-staple", "another-secure-password"));
        assertTrue(encoder.matches("another-secure-password", user.getPasswordHash()));
        assertThrows(IllegalArgumentException.class, () -> service.changePassword(7L,
                new ChangePasswordRequest("incorrect-current", "yet-another-secure-password")));
        assertThrows(IllegalArgumentException.class, () -> service.changePassword(7L,
                new ChangePasswordRequest("another-secure-password", "short")));
        assertThrows(IllegalArgumentException.class, () -> service.changePassword(7L,
                new ChangePasswordRequest("another-secure-password", "another-secure-password")));
    }
}
