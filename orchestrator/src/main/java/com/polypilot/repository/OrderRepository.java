package com.polypilot.repository;

import com.polypilot.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByStrategyId(UUID strategyId);

    /** Backs the strategy-scoped recent-orders feed; matches {@code idx_orders_strategy_date}. */
    Page<Order> findByUserIdAndStrategyId(UUID userId, UUID strategyId, Pageable pageable);

    /** Backs the market-scoped recent-orders feed; matches {@code idx_orders_market}. */
    Page<Order> findByUserIdAndMarketId(UUID userId, String marketId, Pageable pageable);

    /** Backs the Overview stat row's "dry orders · today" figure. */
    long countByUserIdAndPlacedAtAfter(UUID userId, OffsetDateTime placedAtAfter);

    /**
     * Order counts grouped by strategy, in a single pass — avoids an N+1 over the strategy list
     * page when rendering each strategy's trade count. Same shape as
     * {@code MarketRepository.countMarketsBySeriesAndStatus}.
     */
    @Query("""
            select o.strategyId as strategyId, count(o) as count
            from Order o
            where o.strategyId in :strategyIds
            group by o.strategyId
            """)
    List<StrategyOrderCountView> countByStrategyIdIn(@Param("strategyIds") Collection<UUID> strategyIds);
}
