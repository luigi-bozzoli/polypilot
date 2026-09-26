package com.polypilot.reference.repository;

import com.polypilot.reference.entity.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TickerRepository extends JpaRepository<Ticker, UUID> {

    /** Enabled tickers in display order — the streams the {@code ohlc-sync} job pulls. */
    List<Ticker> findByEnabledTrueOrderBySortOrderAsc();
}
