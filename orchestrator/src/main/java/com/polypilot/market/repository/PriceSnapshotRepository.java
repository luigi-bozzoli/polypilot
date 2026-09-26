package com.polypilot.market.repository;

import com.polypilot.market.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, UUID> {

    /**
     * Snapshots for one market from {@code since} onward, oldest-first. Backs the
     * probability-history chart; served by {@code idx_price_snapshots_market_time}.
     */
    List<PriceSnapshot> findByMarket_IdAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(
            String marketId, OffsetDateTime since);
}
