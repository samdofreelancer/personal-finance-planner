package com.finance.planner.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterHttpRequest(
        @NotBlank @Size(max = 256) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @Size(max = 128) String invitationToken) {
}
