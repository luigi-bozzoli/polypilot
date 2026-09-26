package com.polypilot.market.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response of {@code GET /market/{marketId}/price-history}. Contract:
 * {@code contracts/market-price-history.md}.
 *
 * <p>{@code points} is chronological, ascending by {@code at}; an empty list
 * means the market has no snapshots yet (and no live price to synthesise from) —
 * the chart renders that as an empty state, not an error.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceHistoryView {

    private String marketId;

    /** Effective look-back window the server used, echoed back (e.g. "12h"). */
    private String window;

    private List<MarketPricePointView> points;
}
