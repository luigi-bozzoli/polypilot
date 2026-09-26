package com.polypilot.market.service;

import lombok.Value;

import java.time.Duration;

/**
 * A resolved price-history look-back: the effective {@link Duration} to query
 * and the normalised label echoed back to the caller (e.g. {@code "24h"},
 * {@code "7d"}). Built by {@link MarketPriceHistoryService}.
 */
@Value
class PriceHistoryWindow {
    Duration duration;
    String label;
}
