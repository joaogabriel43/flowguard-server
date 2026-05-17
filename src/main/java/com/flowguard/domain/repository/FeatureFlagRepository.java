package com.flowguard.domain.repository;

import com.flowguard.domain.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {
    List<FeatureFlag> findAllByTenantId(UUID tenantId);
    Optional<FeatureFlag> findByTenantIdAndKey(UUID tenantId, String key);
    boolean existsByTenantIdAndKey(UUID tenantId, String key);
    void deleteByTenantIdAndKey(UUID tenantId, String key);
}
