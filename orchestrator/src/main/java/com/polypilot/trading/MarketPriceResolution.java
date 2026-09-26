package com.polypilot.trading;

import com.polypilot.market.entity.Market;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Result of {@link OpenTradeService#resolveFillPrice} — either a resolved {@link Market} plus the
 * strategy's reference price, or a reason to skip opening the trade at all. Package-private:
 * internal collaboration between {@code OpenTradeService} and its test, not part of the
 * {@code trading/} package's public API ({@link OpenTradeResult} is).
 */
@Value
class MarketPriceResolution {
    Market market;
    BigDecimal price;
    String skipReason;

    static MarketPriceResolution resolved(Market market, BigDecimal price) {
        return new MarketPriceResolution(market, price, null);
    }

    static MarketPriceResolution skip(String reason) {
        return new MarketPriceResolution(null, null, reason);
    }

    boolean isSkipped() {
        return skipReason != null;
    }
}
