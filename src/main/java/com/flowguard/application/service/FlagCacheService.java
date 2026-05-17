package com.flowguard.application.service;

import com.flowguard.application.dto.FeatureFlagDto;

import java.util.Optional;
import java.util.UUID;

public interface FlagCacheService {
    Optional<FeatureFlagDto> getFlag(UUID tenantId, String key);
    void putFlag(UUID tenantId, String key, FeatureFlagDto flag);
    void evictFlag(UUID tenantId, String key);
}
