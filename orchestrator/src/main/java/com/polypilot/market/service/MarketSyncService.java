package com.polypilot.market.service;

import com.polypilot.market.entity.Market;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.market.repository.SeriesRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Batch driver for market sync. Each item is delegated to
 * {@link MarketItemSyncService} so it runs in its own {@code REQUIRES_NEW}
 * transaction — one bad series or market is logged and skipped, the rest of the
 * batch continues untouched.
 */
@Slf4j
@Service
@AllArgsConstructor
public class MarketSyncService {

    private final SeriesRepository seriesRepository;
    private final MarketRepository marketRepository;
    private final MarketItemSyncService marketItemSyncService;

    /**
     * Discovery pass: for each tracked series, pull the next open/upcoming market
     * and insert it if we don't have it yet. Existing rows are left for
     * {@link #updateOpenMarkets()} to refresh.
     */
    public void syncAllSeries() {
        List<String> seriesIds = seriesRepository.findTrackedPolymarketIds();

        for (String seriesId : seriesIds) {
            try {
                marketItemSyncService.syncSeriesEvent(seriesId);
            } catch (Exception ex) {
                log.error("Failed to sync series [{}], skipping", seriesId, ex);
            }
        }
    }

    /**
     * Refresh pass: re-read every currently-OPEN market from Polymarket. A moved
     * price/volume/liquidity, or a status change, gets a snapshot of the old
     * values plus an in-place update; an unchanged market only has its
     * {@code last_synced_at} bumped.
     */
    public void updateOpenMarkets() {
        List<Market> openMarkets = marketRepository.findAllByStatus(MarketStatus.OPEN);

        for (Market market : openMarkets) {
            try {
                marketItemSyncService.refreshOpenMarket(market.getId());
            } catch (Exception ex) {
                log.error("Failed to update market [{}], skipping", market.getId(), ex);
            }
        }
    }
}
