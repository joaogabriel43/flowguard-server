package com.flowguard.domain.repository;

import com.flowguard.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findAllByTenantId(UUID tenantId);
    List<AuditLog> findAllByTenantIdAndFlagId(UUID tenantId, UUID flagId);
}
