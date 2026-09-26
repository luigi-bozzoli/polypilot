package com.polypilot.market.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Body of {@code GET /market/{marketId}/orders}. Shape matches
 * {@code MarketOrdersResponse} in {@code contracts/market-recent-orders.md}; wrapped the
 * same way as {@link MarketDecisionsView} rather than a bare list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOrdersView {

    private String marketId;
    private List<MarketOrderView> orders;
}
