package com.flowguard.application.service;

import com.flowguard.application.dto.CreateFeatureFlagCommand;
import com.flowguard.application.dto.FeatureFlagDto;
import com.flowguard.application.dto.FlagChangeEvent;
import com.flowguard.application.dto.UpdateFeatureFlagCommand;
import com.flowguard.domain.exception.ConflictException;
import com.flowguard.domain.exception.ResourceNotFoundException;
import com.flowguard.domain.model.AuditLog;
import com.flowguard.domain.model.FeatureFlag;
import com.flowguard.domain.model.TenantContext;
import com.flowguard.domain.repository.AuditLogRepository;
import com.flowguard.domain.repository.FeatureFlagRepository;
import com.flowguard.infrastructure.messaging.FlagChangePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final AuditLogRepository auditLogRepository;
    private final FlagCacheService flagCacheService;
    private final FlagChangePublisher flagChangePublisher;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository,
                              AuditLogRepository auditLogRepository,
                              FlagCacheService flagCacheService,
                              FlagChangePublisher flagChangePublisher) {
        this.featureFlagRepository = featureFlagRepository;
        this.auditLogRepository = auditLogRepository;
        this.flagCacheService = flagCacheService;
        this.flagChangePublisher = flagChangePublisher;
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not initialized");
        }
        return tenantId;
    }

    @Transactional
    public FeatureFlagDto createFeatureFlag(CreateFeatureFlagCommand command) {
        UUID tenantId = getRequiredTenantId();

        if (featureFlagRepository.existsByTenantIdAndKey(tenantId, command.key())) {
            throw new ConflictException("Feature flag key '" + command.key() + "' already exists for this tenant");
        }

        FeatureFlag flag = FeatureFlag.builder()
                .tenantId(tenantId)
                .key(command.key())
                .name(command.name())
                .description(command.description())
                .enabled(command.enabled())
                .rolloutPercentage(command.rolloutPercentage())
                .build();

        flag = featureFlagRepository.save(flag);
        FeatureFlagDto dto = FeatureFlagDto.fromEntity(flag);

        // Evict from cache
        flagCacheService.evictFlag(tenantId, command.key());

        // Publish event
        flagChangePublisher.publish(new FlagChangeEvent(
                dto.key(),
                tenantId,
                "CREATED",
                Instant.now().toString()
        ));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagDto> listFeatureFlags() {
        UUID tenantId = getRequiredTenantId();
        return featureFlagRepository.findAllByTenantId(tenantId)
                .stream()
                .map(FeatureFlagDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeatureFlagDto getFeatureFlagByKey(String key) {
        UUID tenantId = getRequiredTenantId();

        // Cache-aside pattern
        return flagCacheService.getFlag(tenantId, key)
                .orElseGet(() -> {
                    FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                            .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));
                    FeatureFlagDto dto = FeatureFlagDto.fromEntity(flag);
                    // Update cache on the way back
                    flagCacheService.putFlag(tenantId, key, dto);
                    return dto;
                });
    }

    @Transactional
    public FeatureFlagDto updateFeatureFlag(String key, UpdateFeatureFlagCommand command) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        flag.setName(command.name());
        flag.setDescription(command.description());
        flag.setEnabled(command.enabled());
        flag.setRolloutPercentage(command.rolloutPercentage());

        flag = featureFlagRepository.save(flag);
        FeatureFlagDto dto = FeatureFlagDto.fromEntity(flag);

        // Evict from cache
        flagCacheService.evictFlag(tenantId, key);

        // Publish event
        flagChangePublisher.publish(new FlagChangeEvent(
                dto.key(),
                tenantId,
                "UPDATED",
                Instant.now().toString()
        ));

        return dto;
    }

    @Transactional
    public void deleteFeatureFlag(String key) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));
        
        featureFlagRepository.delete(flag);

        // Evict from cache
        flagCacheService.evictFlag(tenantId, key);

        // Publish event
        flagChangePublisher.publish(new FlagChangeEvent(
                key,
                tenantId,
                "DELETED",
                Instant.now().toString()
        ));
    }

    @Transactional
    public FeatureFlagDto toggleFeatureFlag(String key, String performedBy) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        boolean oldStatus = flag.isEnabled();
        flag.setEnabled(!oldStatus);
        flag = featureFlagRepository.save(flag);
        FeatureFlagDto dto = FeatureFlagDto.fromEntity(flag);

        // Audit log mandatory on activation/deactivation
        AuditLog auditLog = AuditLog.builder()
                .tenantId(tenantId)
                .flagId(flag.getId())
                .action(flag.isEnabled() ? "ACTIVATED" : "DEACTIVATED")
                .performedBy(performedBy)
                .build();
        auditLogRepository.save(auditLog);

        // Evict from cache
        flagCacheService.evictFlag(tenantId, key);

        // Publish event
        flagChangePublisher.publish(new FlagChangeEvent(
                key,
                tenantId,
                "TOGGLED",
                Instant.now().toString()
        ));

        return dto;
    }
}
