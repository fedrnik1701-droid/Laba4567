package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReaderRequest {
    @NotBlank
    private String fullName;
    @Email
    private String email;
    private String phone;
}
