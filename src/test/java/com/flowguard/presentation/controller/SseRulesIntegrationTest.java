package com.flowguard.presentation.controller;

import com.flowguard.BaseIntegrationTest;
import com.flowguard.application.dto.*;
import com.flowguard.application.service.FeatureFlagService;
import com.flowguard.application.service.FlagCacheService;
import com.flowguard.domain.model.RuleOperator;
import com.flowguard.domain.model.Tenant;
import com.flowguard.domain.model.TenantContext;
import com.flowguard.domain.repository.FeatureFlagRepository;
import com.flowguard.domain.repository.TenantRepository;
import com.flowguard.infrastructure.sse.SseEmitterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class SseRulesIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private FlagCacheService flagCacheService;

    @SpyBean
    private SseEmitterRegistry sseEmitterRegistry;

    private UUID tenantId;
    private String flagKey;

    @BeforeEach
    void setUp() throws IOException {
        featureFlagRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = Tenant.builder()
                .name("Tenant Test")
                .apiKey("test-key")
                .build();
        tenant = tenantRepository.saveAndFlush(tenant);
        tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);

        flagKey = "test-rules-flag-" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldEvictCacheAndBroadcastFullPayloadOnRuleMutations() {
        // 1. Create a Feature Flag
        CreateFeatureFlagCommand createCommand = new CreateFeatureFlagCommand(
                flagKey, "Rules Flag", "Flag for SSE testing", true, 100
        );
        FeatureFlagDto flagDto = featureFlagService.createFeatureFlag(createCommand);

        // 2. Put into Redis Cache to verify cache-aside invalidation
        flagCacheService.putFlag(tenantId, flagKey, flagDto);
        assertTrue(flagCacheService.getFlag(tenantId, flagKey).isPresent(), "Flag should be cached in Redis");

        // 3. Add Rule - This must invalidate cache and broadcast a flag-updated SSE event with full payload
        CreateFlagRuleCommand addRuleCommand = new CreateFlagRuleCommand("plan", RuleOperator.EQUALS, "premium");
        FlagRuleDto ruleDto = featureFlagService.addRule(flagKey, addRuleCommand);
        assertNotNull(ruleDto.id());

        // 4. Assert Redis Cache Eviction
        assertFalse(flagCacheService.getFlag(tenantId, flagKey).isPresent(), "Flag cache should be evicted from Redis after adding rule");

        // 5. Assert Asynchronous SSE Broadcast with full payload
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);
            verify(sseEmitterRegistry, atLeastOnce()).broadcast(eq(tenantId), eq("flag-updated"), dataCaptor.capture());
            
            Object capturedData = dataCaptor.getValue();
            assertTrue(capturedData instanceof FeatureFlagDto, "SSE broadcasted data should be the full FeatureFlagDto payload");
            FeatureFlagDto capturedFlag = (FeatureFlagDto) capturedData;
            assertEquals(flagKey, capturedFlag.key());
            assertEquals(1, capturedFlag.rules().size());
            assertEquals("plan", capturedFlag.rules().get(0).attributeKey());
            assertEquals(RuleOperator.EQUALS, capturedFlag.rules().get(0).operator());
            assertEquals("premium", capturedFlag.rules().get(0).attributeValue());
        });

        // 6. Refresh cached flag
        flagDto = featureFlagService.getFeatureFlagByKey(flagKey); // Re-caches it
        flagCacheService.putFlag(tenantId, flagKey, flagDto);
        assertTrue(flagCacheService.getFlag(tenantId, flagKey).isPresent(), "Flag should be cached in Redis again");

        // 7. Delete Rule - This must also invalidate cache and broadcast SSE update
        featureFlagService.deleteRule(flagKey, ruleDto.id());

        // 8. Assert Cache Evicted again
        assertFalse(flagCacheService.getFlag(tenantId, flagKey).isPresent(), "Flag cache should be evicted from Redis after deleting rule");

        // 9. Assert Asynchronous SSE Broadcast after deletion has empty rules list
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);
            verify(sseEmitterRegistry, atLeastOnce()).broadcast(eq(tenantId), eq("flag-updated"), dataCaptor.capture());
            
            Object capturedData = dataCaptor.getValue();
            assertTrue(capturedData instanceof FeatureFlagDto);
            FeatureFlagDto capturedFlag = (FeatureFlagDto) capturedData;
            assertEquals(flagKey, capturedFlag.key());
            assertTrue(capturedFlag.rules().isEmpty(), "Flag rule list should be empty after deletion");
        });
    }
}
