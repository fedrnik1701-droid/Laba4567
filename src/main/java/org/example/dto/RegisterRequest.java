package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "(?s)(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).+",
                message = "Password must contain lowercase and uppercase Latin letters and a digit")
        String password) {
}
