package com.codepalace.project.model.dto.client;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLogin (
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
