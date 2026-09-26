package com.polypilot.market.dto.view;

import com.polypilot.series.enums.SeriesType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * One row on the series list page: the series metadata, its market counts, and
 * a summary of the currently-open market (nullable — a series can sit between
 * markets).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesSummaryView {

    private UUID id;
    private String polymarketId;
    private String ticker;
    private String slug;
    private String title;
    private String recurrence;
    private SeriesType seriesType;

    private long openMarketCount;
    /** Everything not OPEN — RESOLVED, CLOSED and CANCELLED lumped together. */
    private long resolvedMarketCount;

    private MarketView currentMarket;
}
