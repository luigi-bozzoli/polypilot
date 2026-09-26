package com.polypilot.reference.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One selectable candle interval, in display order. Backs the dashboard's OHLC chart
 * timeframe selector so it stays driven by the DB-backed {@code timeframe} table rather than a
 * hardcoded list — same "enable/disable with a DB row" philosophy as the rest of
 * {@code reference/}. Contract: {@code contracts/market-ohlc.md}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeframeView {

    /** Binance interval code, e.g. "1h" — the value to send back as the {@code timeframe} param. */
    private String code;

    private String label;
}
