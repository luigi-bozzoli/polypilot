package com.polypilot.ai.service;

import com.polypilot.ai.dto.view.MarketSentimentView;
import com.polypilot.entity.SentimentScore;
import com.polypilot.repository.SentimentScoreRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Read-side query backing {@code GET /market/{marketId}/sentiment/latest}. Reuses the same
 * {@link SentimentScoreRepository#findFirstByMarketIdOrderByScoredAtDesc}
 * method {@code MarketFieldResolver} already calls for strategy evaluation.
 */
@Service
@AllArgsConstructor
public class SentimentQueryService {

    private final SentimentScoreRepository sentimentScoreRepository;

    @Transactional(readOnly = true)
    public Optional<MarketSentimentView> getLatest(String marketId) {
        return sentimentScoreRepository.findFirstByMarketIdOrderByScoredAtDesc(marketId)
                .map(this::toView);
    }

    private MarketSentimentView toView(SentimentScore score) {
        return MarketSentimentView.builder()
                .id(score.getId())
                .label(score.getSentiment().name())
                .confidence(score.getConfidence())
                .score(null) // no signed scalar produced by this pipeline — see MarketSentimentView Javadoc
                .articleCount(score.getArticleCount())
                .reasoning(score.getReasoning())
                .modelUsed(score.getModelUsed())
                .scoredAt(score.getScoredAt())
                .build();
    }
}
