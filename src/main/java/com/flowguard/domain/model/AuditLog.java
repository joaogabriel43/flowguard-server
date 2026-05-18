package com.flowguard.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // M-7: nullable because the flag may be deleted after the audit entry is created
    @Column(name = "flag_id")
    private UUID flagId;

    @Column(nullable = false)
    private String action;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "performed_at", updatable = false)
    private OffsetDateTime performedAt;

    // M-7: record what the flag looked like before and after the change
    @Column(name = "previous_state")
    private String previousState;

    @Column(name = "new_state")
    private String newState;
}
