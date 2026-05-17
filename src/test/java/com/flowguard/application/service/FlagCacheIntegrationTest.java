package com.flowguard.application.service;

import com.flowguard.BaseIntegrationTest;
import com.flowguard.application.dto.CreateFeatureFlagCommand;
import com.flowguard.application.dto.FeatureFlagDto;
import com.flowguard.domain.model.FeatureFlag;
import com.flowguard.domain.model.Tenant;
import com.flowguard.domain.model.TenantContext;
import com.flowguard.domain.repository.FeatureFlagRepository;
import com.flowguard.domain.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlagCacheIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private FlagCacheService flagCacheService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        featureFlagRepository.deleteAll();
        tenantRepository.deleteAll();

        // Let Hibernate generate the Tenant UUID to prevent overwriting
        Tenant tenant = Tenant.builder()
                .name("Tenant Test")
                .apiKey("api-key-test")
                .build();
        tenant = tenantRepository.saveAndFlush(tenant);
        
        tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldPerformCacheAsideAndInvalidateOnMutation() {
        // 1. Create a feature flag via service
        CreateFeatureFlagCommand createCmd = new CreateFeatureFlagCommand("flag-1", "Flag 1", "Desc", true, 100);
        FeatureFlagDto created = featureFlagService.createFeatureFlag(createCmd);
        
        // 2. Fetch it to populate cache
        FeatureFlagDto fetched1 = featureFlagService.getFeatureFlagByKey("flag-1");
        assertEquals("Flag 1", fetched1.name());

        // 3. Bypass service: Update flag directly in database
        FeatureFlag flagInDb = featureFlagRepository.findByTenantIdAndKey(tenantId, "flag-1").orElseThrow();
        flagInDb.setName("Updated in DB");
        featureFlagRepository.saveAndFlush(flagInDb);

        // 4. Fetch via service again: Should hit Redis and return cached value (old name)
        FeatureFlagDto fetched2 = featureFlagService.getFeatureFlagByKey("flag-1");
        assertEquals("Flag 1", fetched2.name()); // Hit cache!

        // 5. Invalidate: Toggle/update via service, which triggers cache eviction
        featureFlagService.toggleFeatureFlag("flag-1", "user-test");

        // 6. Fetch via service again: Cache is evicted, should hit DB and get the new name and toggled status
        FeatureFlagDto fetched3 = featureFlagService.getFeatureFlagByKey("flag-1");
        assertEquals("Updated in DB", fetched3.name());
        assertFalse(fetched3.enabled()); // was true, now false due to toggle
    }
}
