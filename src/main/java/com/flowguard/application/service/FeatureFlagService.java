package com.flowguard.application.service;

import com.flowguard.application.dto.CreateFeatureFlagCommand;
import com.flowguard.application.dto.FeatureFlagDto;
import com.flowguard.application.dto.UpdateFeatureFlagCommand;
import com.flowguard.domain.exception.ConflictException;
import com.flowguard.domain.exception.ResourceNotFoundException;
import com.flowguard.domain.model.AuditLog;
import com.flowguard.domain.model.FeatureFlag;
import com.flowguard.domain.model.TenantContext;
import com.flowguard.domain.repository.AuditLogRepository;
import com.flowguard.domain.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final AuditLogRepository auditLogRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository,
                              AuditLogRepository auditLogRepository) {
        this.featureFlagRepository = featureFlagRepository;
        this.auditLogRepository = auditLogRepository;
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
        return FeatureFlagDto.fromEntity(flag);
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
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));
        return FeatureFlagDto.fromEntity(flag);
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
        return FeatureFlagDto.fromEntity(flag);
    }

    @Transactional
    public void deleteFeatureFlag(String key) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));
        featureFlagRepository.delete(flag);
    }

    @Transactional
    public FeatureFlagDto toggleFeatureFlag(String key, String performedBy) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        boolean oldStatus = flag.isEnabled();
        flag.setEnabled(!oldStatus);
        flag = featureFlagRepository.save(flag);

        // Audit log mandatory on activation/deactivation
        AuditLog auditLog = AuditLog.builder()
                .tenantId(tenantId)
                .flagId(flag.getId())
                .action(flag.isEnabled() ? "ACTIVATED" : "DEACTIVATED")
                .performedBy(performedBy)
                .build();
        auditLogRepository.save(auditLog);

        return FeatureFlagDto.fromEntity(flag);
    }
}
