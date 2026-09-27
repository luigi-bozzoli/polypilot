package com.polypilot.market.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *
 * <p>{@code symbol} is {@code null} exactly when the market's series has no linked
 * {@code Ticker} asset — that is the chart's "unavailable" signal, distinct from a linked
 * asset with an empty {@code candles} list (no synced history yet). {@code candles} is
 * chronological, ascending by {@code openTime}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOhlcView {

    private String marketId;

    /** Binance pair backing this chart (e.g. "BTCUSDT"), or null — see class Javadoc. */
    private String symbol;

    /** Effective timeframe code used (e.g. "1h"), echoed back regardless of {@code symbol}. */
    private String timeframe;

    private List<OhlcCandleView> candles;
}
