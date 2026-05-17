package com.flowguard.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateFeatureFlagCommand(
    @NotBlank(message = "Flag name is required")
    String name,

    String description,

    boolean enabled,

    @Min(value = 0, message = "Rollout percentage must be at least 0")
    @Max(value = 100, message = "Rollout percentage cannot exceed 100")
    int rolloutPercentage
) {}
