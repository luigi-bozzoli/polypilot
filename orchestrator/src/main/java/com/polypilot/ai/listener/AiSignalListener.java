package com.polypilot.ai.listener;

import com.polypilot.ai.dto.AiSignalMessage;
import com.polypilot.ai.dto.NewsArticle;
import com.polypilot.entity.NewsSummary;
import com.polypilot.entity.SentimentScore;
import com.polypilot.enums.SentimentType;
import com.polypilot.market.entity.Market;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.NewsSummaryRepository;
import com.polypilot.repository.SentimentScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consumes {@code ai.signals} and writes both the {@code news_summaries} and
 * {@code sentiment_scores} rows from a single message — matching "same
 * LangGraph run" at the transport level, per {@code docs/news_summary.MD} §4.5.
 * (No {@code audit_logs} row: see the comment in {@link #onSignal} for why.)
 *
 * <p>A malformed/unparseable message, or an unknown {@code market_id}, is
 * rejected without requeue by {@code RabbitConfig}'s retry interceptor after
 * a bounded number of attempts, routing it to {@code ai.signals.dlq} rather
 * than crashing this listener or looping forever.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiSignalListener {

    private final SentimentScoreRepository sentimentScoreRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final MarketRepository marketRepository;

    @Value("${polypilot.ai.news-ttl:15m}")
    private Duration newsTtl;

    @RabbitListener(queues = "${polypilot.ai.signals-queue:ai.signals}")
    @Transactional
    public void onSignal(AiSignalMessage message) {
        Market market = marketRepository.findById(message.getMarketId())
                .orElseThrow(() -> new IllegalStateException(
                        "AiSignalMessage for unknown market [" + message.getMarketId() + "]"));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        sentimentScoreRepository.save(SentimentScore.builder()
                .market(market)
                .sentiment(SentimentType.valueOf(message.getSentiment()))
                .confidence(BigDecimal.valueOf(message.getConfidence()))
                .reasoning(message.getReasoning())
                .modelUsed(message.getModelUsed())
                .articleCount(message.getArticleCount())
                .rawResponse(message.getRawResponse())
                .scoredAt(now)
                .build());

        newsSummaryRepository.save(NewsSummary.builder()
                .marketId(message.getMarketId())
                .summary(message.getSummary())
                .articles(toArticleMaps(message.getArticles()))
                .modelUsed(message.getModelUsed())
                .fetchedAt(now)
                .expiresAt(now.plus(newsTtl))
                .build());

        log.info("Recorded ai.signals result for market [{}]: {} (confidence={})",
                message.getMarketId(), message.getSentiment(), message.getConfidence());
    }

    private List<Map<String, Object>> toArticleMaps(List<NewsArticle> articles) {
        return articles.stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("title", a.getTitle());
            map.put("url", a.getUrl());
            map.put("source", a.getSource());
            map.put("published_at", a.getPublishedAt());
            return map;
        }).toList();
    }
}
