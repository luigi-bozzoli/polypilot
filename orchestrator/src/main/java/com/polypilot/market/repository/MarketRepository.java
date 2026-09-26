package com.polypilot.market.repository;

import com.polypilot.market.entity.Market;
import com.polypilot.market.enums.MarketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketRepository extends JpaRepository<Market, String> {

    List<Market> findAllByStatus(MarketStatus status);

    /**
     * Every market for one series, newest first. Ordered by {@code createdAt}
     * rather than {@code resolutionDate}: the latter now holds the scheduled
     * settlement time, which is not the order markets were discovered in.
     */
    List<Market> findBySeriesIdOrderByCreatedAtDesc(UUID seriesId);

    /**
     * The series's current market in the given status, if any — newest first in case more than
     * one slipped through. Used to resolve a strategy's evaluation target dynamically from its
     * series (see {@code StrategyService.runEvaluation}) rather than a marketId snapshotted at
     * creation time, so a series's market rollover is picked up automatically. Mirrors the same
     * "one OPEN market per series is the norm" assumption {@code SeriesQueryService} documents.
     */
    Optional<Market> findFirstBySeriesIdAndStatusOrderByCreatedAtDesc(UUID seriesId, MarketStatus status);

    /**
     * Bump only {@code last_synced_at}, without touching price columns or the
     * {@code @UpdateTimestamp} on {@code updated_at}. Used when a sync fetched a
     * market and found nothing changed.
     */
    @Modifying
    @Query("update Market m set m.lastSyncedAt = :ts where m.id = :id")
    void updateLastSyncedAt(@Param("id") String id, @Param("ts") OffsetDateTime ts);

    /**
     * Market counts grouped by series and status, in a single pass — avoids an
     * N+1 over series on the list page.
     */
    @Query("""
            select m.series.id as seriesId, m.status as status, count(m) as count
            from Market m
            group by m.series.id, m.status
            """)
    List<SeriesMarketCountView> countMarketsBySeriesAndStatus();
}
