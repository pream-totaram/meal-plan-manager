package com.acedigital.meal_plan_manager.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "username may only contain letters, digits, '.', '_', and '-'")
    String username,

    @NotBlank
    @Email
    @Size(max = 254)
    String email,

    // Min 12 chars is a reasonable baseline for NIST SP 800-63B.
    // Bcrypt hashes anything over 72 bytes to the same input, so cap at 72.
    @NotBlank
    @Size(min = 12, max = 72, message = "password must be between 12 and 72 characters")
    String password) {
}
