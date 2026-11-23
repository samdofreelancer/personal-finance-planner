package com.finance.planner.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginHttpRequest(
        @NotBlank @Size(max = 256) String email,
        @NotBlank @Size(max = 72) String password) {
}
