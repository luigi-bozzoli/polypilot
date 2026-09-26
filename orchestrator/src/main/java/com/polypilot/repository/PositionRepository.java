package com.polypilot.repository;

import com.polypilot.entity.Position;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.market.enums.MarketOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {

    Optional<Position> findByUserIdAndMarketIdAndTokenSideAndIsDryRunAndStatus(
            UUID userId, String marketId, MarketOutcome tokenSide, Boolean isDryRun, PositionStatus status);

    List<Position> findAllByStatus(PositionStatus status);

    /** Backs the Overview "Open positions" widget and the portfolio-summary aggregation. */
    List<Position> findAllByUserIdAndStatusOrderByOpenedAtDesc(UUID userId, PositionStatus status);

    /** Backs the portfolio-summary "realized P&L · 7d" figure. */
    List<Position> findAllByUserIdAndStatusAndClosedAtAfter(UUID userId, PositionStatus status, OffsetDateTime closedAtAfter);
}
