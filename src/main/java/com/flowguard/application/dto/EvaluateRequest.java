package com.flowguard.application.dto;

import com.flowguard.application.validation.ValidAttributeMap;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record EvaluateRequest(
    @NotBlank(message = "userId is required") String userId,
    // M-3: limit entries to 50, keys to 50 chars, values to 200 chars to prevent DoS via large payloads
    @ValidAttributeMap(maxEntries = 50, maxKeyLength = 50, maxValueLength = 200)
    Map<String, String> attributes
) {}
