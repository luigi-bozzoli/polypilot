package com.polypilot.entity;

import com.polypilot.market.entity.Series;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.enums.order.OrderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "strategies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Strategy {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "enabled", nullable = false)

    private Boolean enabled = true;

    @Column(name = "dry_run", nullable = false)
    
    private Boolean dryRun = true;

    @Column(name = "cron_expression", nullable = false, length = 100)

    private String cronExpression;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_side", nullable = false, length = 3)
    
    private MarketOutcome tokenSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    
    private OrderType orderType;

    @Column(name = "max_bet_size", nullable = false, precision = 18, scale = 4)
    
    private BigDecimal maxBetSize;

    @Column(name = "max_daily_exposure", nullable = false, precision = 18, scale = 4)
    
    private BigDecimal maxDailyExposure;

    @Column(name = "stop_loss_threshold", precision = 6, scale = 4)
    private BigDecimal stopLossThreshold;

    @Column(name = "rule_tree", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String ruleTree;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
