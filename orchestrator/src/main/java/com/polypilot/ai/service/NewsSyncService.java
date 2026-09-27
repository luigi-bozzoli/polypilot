package com.polypilot.ai.service;

import com.polypilot.market.entity.Market;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.repository.MarketRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Batch driver for the {@code news-sync} scheduled job. Same shape as
 * {@code MarketSyncService}: every currently-OPEN market is a candidate
 * (matching that class's own {@code updateOpenMarkets} scope, rather than
 * narrowing to markets with an enabled strategy attached — sentiment/news
 * data should stay available on the dashboard independently of whether a
 * strategy happens to be attached, each
 * delegated to {@link NewsSyncItemService} in its own {@code REQUIRES_NEW}
 * transaction so one bad market never blocks the rest.
 */
@Slf4j
@Service
@AllArgsConstructor
public class NewsSyncService {

    private final MarketRepository marketRepository;
    private final NewsSyncItemService newsSyncItemService;

    public void syncAll() {
        List<Market> openMarkets = marketRepository.findAllByStatus(MarketStatus.OPEN);

        for (Market market : openMarkets) {
            try {
                newsSyncItemService.syncOne(market.getId());
            } catch (Exception ex) {
                log.error("Failed to sync news for market [{}], skipping", market.getId(), ex);
            }
        }
    }
}
