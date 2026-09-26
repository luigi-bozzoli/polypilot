package com.polypilot.market.repository;

import com.polypilot.market.enums.MarketStatus;

import java.util.UUID;

/**
 * Projection for {@link MarketRepository#countMarketsBySeriesAndStatus()} — one
 * row per (series, status) pair with the number of markets in it.
 */
public interface SeriesMarketCountView {

    UUID getSeriesId();

    MarketStatus getStatus();

    long getCount();
}
