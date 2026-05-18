package com.flowguard.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "flag_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlagRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // M-6: tenant_id for direct tenant isolation without requiring a flag join
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "attribute_key", nullable = false)
    private String attributeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleOperator operator;

    @Column(name = "attribute_value", nullable = false, columnDefinition = "TEXT")
    private String attributeValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlag flag;
}
