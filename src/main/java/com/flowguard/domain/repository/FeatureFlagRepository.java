package com.flowguard.domain.repository;

import com.flowguard.domain.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {
    
    @Query("select distinct f from FeatureFlag f left join fetch f.rules where f.tenantId = :tenantId")
    List<FeatureFlag> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Query("select f from FeatureFlag f left join fetch f.rules where f.tenantId = :tenantId and f.key = :key")
    Optional<FeatureFlag> findByTenantIdAndKey(@Param("tenantId") UUID tenantId, @Param("key") String key);
    boolean existsByTenantIdAndKey(UUID tenantId, String key);
    void deleteByTenantIdAndKey(UUID tenantId, String key);
}
