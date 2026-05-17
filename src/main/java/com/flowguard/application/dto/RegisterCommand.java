package com.flowguard.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCommand(
    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
    String tenantName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    String password
) {}
