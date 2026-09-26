package com.polypilot.repository;

import com.polypilot.entity.NewsSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NewsSummaryRepository extends JpaRepository<NewsSummary, UUID> {

    /**
     * Latest non-expired summary for one market, if any — backs both the
     * {@code news-sync} scheduler's cost-control skip check and
     * {@code GET /market/{id}/news/latest}.
     */
    Optional<NewsSummary> findFirstByMarketIdAndExpiresAtAfterOrderByFetchedAtDesc(
            String marketId, OffsetDateTime now);
}
