package com.polypilot.repository;

import com.polypilot.entity.AuditLog;
import com.polypilot.enums.alert.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    boolean existsByStrategyId(UUID strategyId);

    Optional<AuditLog> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Filtered, paginated audit feed for one user — every filter param is
     * optional (null skips that clause), backing both the general
     * {@code GET /audit-logs} list and the market-/strategy-scoped slices
     * consumed by {@code EngineDecisionsCard}/{@code StrategyDecisionsCard}.
     */
    @Query("""
            select a from AuditLog a
            where a.userId = :userId
              and (:action is null or a.action = :action)
              and (:strategyId is null or a.strategyId = :strategyId)
              and (:marketId is null or a.marketId = :marketId)
              and a.createdAt >= coalesce(:since, a.createdAt)
            """)
    Page<AuditLog> search(@Param("userId") UUID userId,
                           @Param("action") ActionType action,
                           @Param("strategyId") UUID strategyId,
                           @Param("marketId") String marketId,
                           @Param("since") OffsetDateTime since,
                           Pageable pageable);
}
