package com.polypilot.entity;


import com.polypilot.enums.alert.ActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "strategy_id")
    private UUID strategyId;

    @Column(name = "market_id")
    private String marketId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private ActionType action;

    @Builder.Default
    @Column(name = "is_dry_run", nullable = false)
    private Boolean isDryRun = true;

    @Column(name = "signals", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String signals;

    @Column(name = "reasoning")
    private String reasoning;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}