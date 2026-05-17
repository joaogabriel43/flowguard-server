package com.flowguard.application.dto;

import java.util.UUID;

public record FlagChangeEvent(
    String flagKey,
    UUID tenantId,
    String action, // CREATED, UPDATED, DELETED, TOGGLED
    String timestamp,
    FeatureFlagDto featureFlag
) {
    public FlagChangeEvent(String flagKey, UUID tenantId, String action, String timestamp) {
        this(flagKey, tenantId, action, timestamp, null);
    }
}
