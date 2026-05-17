package com.flowguard.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record EvaluateRequest(
    @NotBlank(message = "userId is required") String userId,
    Map<String, String> attributes
) {}
