package com.flowguard.application.dto;

import com.flowguard.domain.model.FeatureFlag;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FeatureFlagDto(
    UUID id,
    UUID tenantId,
    String key,
    String name,
    String description,
    boolean enabled,
    int rolloutPercentage,
    List<FlagRuleDto> rules,
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
            flag.getRules() != null 
                ? flag.getRules().stream().map(FlagRuleDto::fromEntity).toList() 
                : List.of(),
            flag.getCreatedAt(),
            flag.getUpdatedAt()
        );
    }
}
