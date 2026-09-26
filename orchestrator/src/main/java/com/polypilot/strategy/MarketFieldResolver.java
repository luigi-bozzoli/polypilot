package com.polypilot.strategy;

import com.polypilot.entity.SentimentScore;
import com.polypilot.market.entity.Market;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.SentimentScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Resolves a {@link MarketFieldSnapshot} for one market — the market-field counterpart to
 * {@link com.polypilot.indicator.service.IndicatorCalculationService}, deliberately kept separate
 * from it since market fields are not indicators (constraint from the strategy-node-market-fields
 * design). Called at most once per {@link StrategyEvaluationService#evaluate}.
 */
@Service
@RequiredArgsConstructor
public class MarketFieldResolver {

    private final MarketRepository marketRepository;
    private final SentimentScoreRepository sentimentScoreRepository;

    @Transactional(readOnly = true)
    public MarketFieldSnapshot resolve(String marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Market not found: " + marketId));

        Optional<SentimentScore> latestSentiment =
                sentimentScoreRepository.findFirstByMarketIdOrderByScoredAtDesc(marketId);

        return new MarketFieldSnapshot(
                market.getUpPrice(),
                market.getDownPrice(),
                market.getVolume24h(),
                market.getLiquidity(),
                latestSentiment.map(SentimentScore::getSentiment).orElse(null),
                latestSentiment.map(SentimentScore::getConfidence).orElse(null));
    }
}
