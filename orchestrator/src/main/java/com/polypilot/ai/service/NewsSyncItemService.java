package com.polypilot.ai.service;

import com.polypilot.ai.client.AiAgentClient;
import com.polypilot.ai.dto.AnalyzeRequest;
import com.polypilot.market.entity.Market;
import com.polypilot.market.entity.Series;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.reference.entity.Ticker;
import com.polypilot.repository.NewsSummaryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * One-item unit of work for {@link NewsSyncService}. Lives in its own bean so
 * the {@code REQUIRES_NEW} boundary is a real proxy hop — same pattern as
 * {@code MarketItemSyncService}: one bad market never rolls back or blocks
 * another's.
 *
 * <p>This service only decides <i>whether to ask</i> ai-agent to run and fires
 * the trigger request — it never itself writes {@code news_summaries} /
 * {@code sentiment_scores}. Those rows are written later by
 * {@code AiSignalListener} once ai-agent's background task actually finishes
 * and publishes to {@code ai.signals}, consistent with the fire-and-forget
 * async boundary.
 */
@Slf4j
@Service
@AllArgsConstructor
public class NewsSyncItemService {

    private final NewsSummaryRepository newsSummaryRepository;
    private final MarketRepository marketRepository;
    private final AiAgentClient aiAgentClient;

    /**
     * Takes a {@code marketId}, not a {@code Market}, and re-reads it inside
     * this method's own transaction — same reason as
     * {@code MarketItemSyncService.refreshOpenMarket}: a {@code Market} handed
     * in from the caller's (different) transaction is detached, and its lazy
     * {@code series}/{@code series.asset} associations would throw
     * {@code LazyInitializationException} outside their original session.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void syncOne(String marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new IllegalStateException("Market not found: " + marketId));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        boolean hasFreshSummary = newsSummaryRepository
                .findFirstByMarketIdAndExpiresAtAfterOrderByFetchedAtDesc(market.getId(), now)
                .isPresent();
        if (hasFreshSummary) {
            log.debug("Market [{}] has a non-expired news summary, skipping", market.getId());
            return;
        }

        Series series = market.getSeries();
        Ticker asset = series != null ? series.getAsset() : null;
        if (asset == null) {
            log.warn("Series [{}] for market [{}] has no linked asset (ticker_id), skipping news-sync",
                    series != null ? series.getId() : null, market.getId());
            return;
        }

        AnalyzeRequest request = new AnalyzeRequest(
                market.getId(), asset.getSymbol(), asset.getDisplayName(), market.getQuestion());

        try {
            aiAgentClient.analyze(request);
            log.debug("Triggered ai-agent analysis for market [{}]", market.getId());
        } catch (RestClientException ex) {
            // ai-agent unreachable/erroring — log and move on. No row is written by
            // this path either way, so the next news-sync tick retries naturally
            // (no fresh news_summaries row exists to skip on).
            log.error("Failed to trigger ai-agent analysis for market [{}], skipping", market.getId(), ex);
        }
    }
}
