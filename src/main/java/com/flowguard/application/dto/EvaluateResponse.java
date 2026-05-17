package com.flowguard.application.dto;

public record EvaluateResponse(
    boolean enabled,
    String reason // DISABLED, RULE_MISMATCH, ROLLOUT
) {}
