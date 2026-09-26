package com.polypilot.ai.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response of {@code GET /market/{marketId}/news/latest}. Contract:
 * {@code contracts/market-news-summary.md}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketNewsSummaryView {
    private UUID id;
    private String summary;
    private OffsetDateTime fetchedAt;
    private OffsetDateTime expiresAt;
    private String modelUsed;
    private List<NewsSourceView> sources;
}
