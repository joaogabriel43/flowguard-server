package com.flowguard.application.service;

import com.flowguard.application.dto.*;
import com.flowguard.domain.exception.ConflictException;
import com.flowguard.domain.exception.ResourceNotFoundException;
import com.flowguard.domain.model.AuditLog;
import com.flowguard.domain.model.FeatureFlag;
import com.flowguard.domain.model.FlagRule;
import com.flowguard.domain.model.TenantContext;
import com.flowguard.domain.repository.AuditLogRepository;
import com.flowguard.domain.repository.FeatureFlagRepository;
import com.flowguard.domain.service.RuleEvaluator;
import com.flowguard.domain.util.RolloutEvaluator;
import com.flowguard.infrastructure.messaging.FlagChangePublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final RuleEvaluator ruleEvaluator;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository,
                              AuditLogRepository auditLogRepository,
                              FlagCacheService flagCacheService,
                              FlagChangePublisher flagChangePublisher,
                              RuleEvaluator ruleEvaluator) {
        this.featureFlagRepository = featureFlagRepository;
        this.auditLogRepository = auditLogRepository;
        this.flagCacheService = flagCacheService;
        this.flagChangePublisher = flagChangePublisher;
        this.ruleEvaluator = ruleEvaluator;
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not initialized");
        }
        return tenantId;
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private static String flagState(FeatureFlag flag) {
        return "enabled=" + flag.isEnabled() + ",rollout=" + flag.getRolloutPercentage();
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

        // A-3: audit log for flag creation
        auditLogRepository.save(AuditLog.builder()
                .tenantId(tenantId)
                .flagId(flag.getId())
                .action("CREATED")
                .performedBy(getCurrentUser())
                .previousState(null)
                .newState(flagState(flag))
                .build());

        flagCacheService.evictFlag(tenantId, command.key());
        flagChangePublisher.publish(new FlagChangeEvent(
                dto.key(), tenantId, "CREATED", Instant.now().toString()
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

        return flagCacheService.getFlag(tenantId, key)
                .orElseGet(() -> {
                    FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                            .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));
                    FeatureFlagDto dto = FeatureFlagDto.fromEntity(flag);
                    flagCacheService.putFlag(tenantId, key, dto);
                    return dto;
                });
    }

    @Transactional
    public FeatureFlagDto updateFeatureFlag(String key, UpdateFeatureFlagCommand command) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        // A-3: capture state before mutation for audit trail
        String previousState = flagState(flag);

        flag.setName(command.name());
        flag.setDescription(command.description());
        flag.setEnabled(command.enabled());
        flag.setRolloutPercentage(command.rolloutPercentage());

        flag = featureFlagRepository.save(flag);
        FeatureFlagDto dto = FeatureFlagDto.fromEntity(flag);

        auditLogRepository.save(AuditLog.builder()
                .tenantId(tenantId)
                .flagId(flag.getId())
                .action("UPDATED")
                .performedBy(getCurrentUser())
                .previousState(previousState)
                .newState(flagState(flag))
                .build());

        flagCacheService.evictFlag(tenantId, key);
        flagChangePublisher.publish(new FlagChangeEvent(
                dto.key(), tenantId, "UPDATED", Instant.now().toString()
        ));

        return dto;
    }

    @Transactional
    public void deleteFeatureFlag(String key) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        // A-3: capture state before deletion; audit log saved first so it survives flag deletion.
        //      FK is ON DELETE SET NULL (V6 migration) so this entry is preserved after flag is removed.
        String previousState = flagState(flag);
        UUID flagId = flag.getId();

        auditLogRepository.save(AuditLog.builder()
                .tenantId(tenantId)
                .flagId(flagId)
                .action("DELETED")
                .performedBy(getCurrentUser())
                .previousState(previousState)
                .newState(null)
                .build());

        featureFlagRepository.delete(flag);

        flagCacheService.evictFlag(tenantId, key);
        flagChangePublisher.publish(new FlagChangeEvent(
                key, tenantId, "DELETED", Instant.now().toString()
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

        auditLogRepository.save(AuditLog.builder()
                .tenantId(tenantId)
                .flagId(flag.getId())
                .action(flag.isEnabled() ? "ACTIVATED" : "DEACTIVATED")
                .performedBy(performedBy)
                .build());

        flagCacheService.evictFlag(tenantId, key);
        flagChangePublisher.publish(new FlagChangeEvent(
                key, tenantId, "TOGGLED", Instant.now().toString()
        ));

        return dto;
    }

    @Transactional
    public FlagRuleDto addRule(String key, CreateFlagRuleCommand command) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        // A-3: count before adding
        String previousState = "rules=" + flag.getRules().size();

        FlagRule rule = FlagRule.builder()
                .tenantId(tenantId)
                .attributeKey(command.attributeKey())
                .operator(command.operator())
                .attributeValue(command.attributeValue())
                .flag(flag)
                .build();

        flag.getRules().add(rule);
        flag = featureFlagRepository.save(flag);

        FlagRule savedRule = flag.getRules().stream()
                .filter(r -> r.getAttributeKey().equals(command.attributeKey()) &&
                             r.getOperator() == command.operator() &&
                             r.getAttributeValue().equals(command.attributeValue()))
                .findFirst()
                .orElse(rule);

        FlagRuleDto ruleDto = FlagRuleDto.fromEntity(savedRule);

        auditLogRepository.save(AuditLog.builder()
                .tenantId(tenantId)
                .flagId(flag.getId())
                .action("RULE_ADDED")
                .performedBy(getCurrentUser())
                .previousState(previousState)
                .newState("rules=" + flag.getRules().size())
                .build());

        flagCacheService.evictFlag(tenantId, key);

        FeatureFlagDto fullFlagDto = FeatureFlagDto.fromEntity(flag);
        flagChangePublisher.publish(new FlagChangeEvent(
                key, tenantId, "UPDATED", Instant.now().toString(), fullFlagDto
        ));

        return ruleDto;
    }

    @Transactional(readOnly = true)
    public List<FlagRuleDto> listRules(String key) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        return flag.getRules().stream()
                .map(FlagRuleDto::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteRule(String key, UUID ruleId) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        // A-3: count before removal
        String previousState = "rules=" + flag.getRules().size();

        boolean removed = flag.getRules().removeIf(r -> r.getId().equals(ruleId));
        if (!removed) {
            throw new ResourceNotFoundException("Flag rule with ID '" + ruleId + "' not found on flag '" + key + "'");
        }

        flag = featureFlagRepository.save(flag);

        auditLogRepository.save(AuditLog.builder()
                .tenantId(tenantId)
                .flagId(flag.getId())
                .action("RULE_DELETED")
                .performedBy(getCurrentUser())
                .previousState(previousState)
                .newState("rules=" + flag.getRules().size())
                .build());

        flagCacheService.evictFlag(tenantId, key);

        FeatureFlagDto fullFlagDto = FeatureFlagDto.fromEntity(flag);
        flagChangePublisher.publish(new FlagChangeEvent(
                key, tenantId, "UPDATED", Instant.now().toString(), fullFlagDto
        ));
    }

    @Transactional(readOnly = true)
    public EvaluateResponse evaluateFlag(String key, EvaluateRequest request) {
        UUID tenantId = getRequiredTenantId();
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag with key '" + key + "' not found"));

        if (!flag.isEnabled()) {
            return new EvaluateResponse(false, "DISABLED");
        }

        if (!ruleEvaluator.evaluate(flag.getRules(), request.attributes())) {
            return new EvaluateResponse(false, "RULE_MISMATCH");
        }

        boolean inRollout = RolloutEvaluator.isUserInRollout(flag.getKey(), request.userId(), flag.getRolloutPercentage());
        return new EvaluateResponse(inRollout, "ROLLOUT");
    }
}
