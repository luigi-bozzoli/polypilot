package com.polypilot.market.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Body of {@code GET /market/{marketId}/decisions}. Shape matches
 * {@code MarketDecisionsResponse} in {@code contracts/market-engine-decisions.md}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDecisionsView {

    private String marketId;
    private List<EngineDecisionView> decisions;
}
