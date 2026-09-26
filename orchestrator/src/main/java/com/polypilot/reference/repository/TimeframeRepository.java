package com.polypilot.reference.repository;

import com.polypilot.reference.entity.Timeframe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeframeRepository extends JpaRepository<Timeframe, String> {

    /** Enabled timeframes in display order — the intervals the {@code ohlc-sync} job pulls. */
    List<Timeframe> findByEnabledTrueOrderBySortOrderAsc();
}
