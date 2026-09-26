package com.polypilot.market.dto.view;

import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.enums.MarketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Outbound view of a single {@code Market} row for the dashboard. Distinct from
 * {@code MarketResponseDto}, which is the inbound shape from Polymarket Gamma.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketView {

    private String id;
    private String question;
    private String category;
    private MarketStatus status;

    private BigDecimal upPrice;
    private BigDecimal downPrice;

    private BigDecimal volume24h;
    private BigDecimal liquidity;

    private OffsetDateTime resolutionDate;
    private MarketOutcome outcome;

    private OffsetDateTime lastSyncedAt;
}
