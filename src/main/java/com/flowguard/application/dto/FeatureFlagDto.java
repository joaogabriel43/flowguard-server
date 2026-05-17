package com.flowguard.application.dto;

import com.flowguard.domain.model.FeatureFlag;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeatureFlagDto(
    UUID id,
    UUID tenantId,
    String key,
    String name,
    String description,
    boolean enabled,
    int rolloutPercentage,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static FeatureFlagDto fromEntity(FeatureFlag flag) {
        return new FeatureFlagDto(
            flag.getId(),
            flag.getTenantId(),
            flag.getKey(),
            flag.getName(),
            flag.getDescription(),
            flag.isEnabled(),
            flag.getRolloutPercentage(),
            flag.getCreatedAt(),
            flag.getUpdatedAt()
        );
    }
}
