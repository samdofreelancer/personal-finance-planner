package com.finance.planner.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal AuthenticatedUser principal) {
        return profileService.getProfile(principal.userId());
    }

    @PutMapping
    public ProfileResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                                  @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(principal.userId(), request);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(principal.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
