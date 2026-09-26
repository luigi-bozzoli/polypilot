package com.polypilot.repository;

import java.util.UUID;

/**
 * Projection for {@link OrderRepository#countByStrategyIdIn}  — one row per strategy with its
 * total order count. Mirrors {@code SeriesMarketCountView}.
 */
public interface StrategyOrderCountView {

    UUID getStrategyId();

    long getCount();
}
