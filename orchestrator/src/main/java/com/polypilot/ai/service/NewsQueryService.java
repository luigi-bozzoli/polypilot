package com.polypilot.ai.service;

import com.polypilot.ai.dto.view.MarketNewsSummaryView;
import com.polypilot.ai.dto.view.NewsSourceView;
import com.polypilot.entity.NewsSummary;
import com.polypilot.repository.NewsSummaryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

/**
 * Read-side query backing {@code GET /market/{marketId}/news/latest}. Contract:
 * {@code contracts/market-news-summary.md}. Pure fan-in over
 * {@link NewsSummaryRepository} — no writes (those happen in
 * {@code AiSignalListener}).
 */
@Service
@AllArgsConstructor
public class NewsQueryService {

    private final NewsSummaryRepository newsSummaryRepository;

    @Transactional(readOnly = true)
    public Optional<MarketNewsSummaryView> getLatest(String marketId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return newsSummaryRepository
                .findFirstByMarketIdAndExpiresAtAfterOrderByFetchedAtDesc(marketId, now)
                .map(this::toView);
    }

    private MarketNewsSummaryView toView(NewsSummary summary) {
        return MarketNewsSummaryView.builder()
                .id(summary.getId())
                .summary(summary.getSummary())
                .fetchedAt(summary.getFetchedAt())
                .expiresAt(summary.getExpiresAt())
                .modelUsed(summary.getModelUsed())
                .sources(summary.getArticles().stream().map(this::toSourceView).toList())
                .build();
    }

    @SuppressWarnings("unchecked")
    private NewsSourceView toSourceView(Map<String, Object> article) {
        return NewsSourceView.builder()
                .title((String) article.get("title"))
                .url((String) article.get("url"))
                .publisher((String) article.get("source"))
                .publishedAt((String) article.get("published_at"))
                .build();
    }
}
