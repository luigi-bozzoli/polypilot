package com.polypilot.repository;

import com.polypilot.entity.Order;
import com.polypilot.entity.SentimentScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SentimentScoreRepository extends JpaRepository<SentimentScore, UUID> {

    /** Latest sentiment score for one market, if any — used by {@code MarketFieldResolver}. */
    Optional<SentimentScore> findFirstByMarketIdOrderByScoredAtDesc(String marketId);
}
