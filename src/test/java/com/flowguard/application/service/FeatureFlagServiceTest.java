package com.flowguard.application.service;

import com.flowguard.application.dto.CreateFeatureFlagCommand;
import com.flowguard.application.dto.UpdateFeatureFlagCommand;
import com.flowguard.domain.exception.ConflictException;
import com.flowguard.domain.exception.ResourceNotFoundException;
import com.flowguard.domain.model.FeatureFlag;
import com.flowguard.domain.model.TenantContext;
import com.flowguard.domain.repository.AuditLogRepository;
import com.flowguard.domain.repository.FeatureFlagRepository;
import com.flowguard.infrastructure.messaging.FlagChangePublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private FlagCacheService flagCacheService;

    @Mock
    private FlagChangePublisher flagChangePublisher;

    @InjectMocks
    private FeatureFlagService featureFlagService;

    private UUID tenantAId;
    private UUID tenantBId;

    @BeforeEach
    void setUp() {
        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldPreventTenantAFromAccessingTenantBFlag() {
        // Given: Set Context to Tenant A
        TenantContext.setTenantId(tenantAId);

        // When we search for key "flag-x", the repository returns empty because it's filtered by tenantAId
        when(featureFlagRepository.findByTenantIdAndKey(tenantAId, "flag-x"))
                .thenReturn(Optional.empty());

        // Then: Querying flag-x under Tenant A throws ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> {
            featureFlagService.getFeatureFlagByKey("flag-x");
        });

        verify(featureFlagRepository).findByTenantIdAndKey(tenantAId, "flag-x");
        verify(featureFlagRepository, never()).findByTenantIdAndKey(eq(tenantBId), any());
    }

    @Test
    void shouldPreventTenantAFromUpdatingTenantBFlag() {
        // Given: Set Context to Tenant A
        TenantContext.setTenantId(tenantAId);
        UpdateFeatureFlagCommand command = new UpdateFeatureFlagCommand("New Name", "Desc", true, 50);

        // When searching, returns empty
        when(featureFlagRepository.findByTenantIdAndKey(tenantAId, "flag-x"))
                .thenReturn(Optional.empty());

        // Then: Updating throws ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> {
            featureFlagService.updateFeatureFlag("flag-x", command);
        });

        verify(featureFlagRepository, never()).save(any());
    }

    @Test
    void shouldPreventTenantAFromDeletingTenantBFlag() {
        // Given: Set Context to Tenant A
        TenantContext.setTenantId(tenantAId);

        // When searching, returns empty
        when(featureFlagRepository.findByTenantIdAndKey(tenantAId, "flag-x"))
                .thenReturn(Optional.empty());

        // Then: Deleting throws ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> {
            featureFlagService.deleteFeatureFlag("flag-x");
        });

        verify(featureFlagRepository, never()).delete(any());
    }

    @Test
    void shouldPreventTenantAFromTogglingTenantBFlag() {
        // Given: Set Context to Tenant A
        TenantContext.setTenantId(tenantAId);

        // When searching, returns empty
        when(featureFlagRepository.findByTenantIdAndKey(tenantAId, "flag-x"))
                .thenReturn(Optional.empty());

        // Then: Toggling throws ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> {
            featureFlagService.toggleFeatureFlag("flag-x", "admin@tenantA.com");
        });

        verify(featureFlagRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void shouldPreventCreatingDuplicateFlagKeyWithinTenant() {
        // Given: Set Context to Tenant A
        TenantContext.setTenantId(tenantAId);
        CreateFeatureFlagCommand command = new CreateFeatureFlagCommand("flag-dup", "Flag", "Desc", true, 0);

        when(featureFlagRepository.existsByTenantIdAndKey(tenantAId, "flag-dup")).thenReturn(true);

        // Then: Creating duplicate throws ConflictException
        assertThrows(ConflictException.class, () -> {
            featureFlagService.createFeatureFlag(command);
        });

        verify(featureFlagRepository, never()).save(any());
    }
}
