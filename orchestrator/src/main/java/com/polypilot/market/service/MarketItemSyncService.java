package com.polypilot.market.service;

import com.polypilot.market.dto.EventResponseDto;
import com.polypilot.market.dto.MarketResponseDto;
import com.polypilot.market.entity.Market;
import com.polypilot.market.entity.PriceSnapshot;
import com.polypilot.market.entity.Series;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.mapper.MarketMapper;
import com.polypilot.market.mapper.MarketStatusResolver;
import com.polypilot.market.mapper.OutcomePricesMapper;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.market.repository.PriceSnapshotRepository;
import com.polypilot.market.repository.SeriesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * One-item unit of work for {@link MarketSyncService}. Lives in its own bean so
 * the {@code REQUIRES_NEW} boundary is a real proxy hop: a failure on one series
 * or market rolls back only that item (snapshot included) and the batch loop in
 * {@code MarketSyncService} moves on to the next.
 *
 * <p>Division of labour:
 * <ul>
 *   <li>{@link #syncSeriesEvent} — discovery only. Inserts the next market for a
 *       tracked series; never updates an existing row.</li>
 *   <li>{@link #refreshOpenMarket} — the single refresher. Re-reads every OPEN
 *       market each cycle, snapshots + updates it when something moved, and
 *       records the transition out of OPEN as just another update.</li>
 * </ul>
 */
@Slf4j
@Service
public class MarketItemSyncService {

    /** Prices are NUMERIC(6,4), volume/liquidity NUMERIC(18,4) — compare at that scale. */
    private static final int PERSISTED_SCALE = 4;

    private final RestClient restClient;
    private final MarketMapper marketMapper;
    private final MarketRepository marketRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final SeriesRepository seriesRepository;
    private final MarketStatusResolver marketStatusResolver;
    private final OutcomePricesMapper outcomePricesMapper;
    private final String gammaBaseUrl;

    public MarketItemSyncService(RestClient restClient,
                                 MarketMapper marketMapper,
                                 MarketRepository marketRepository,
                                 PriceSnapshotRepository priceSnapshotRepository,
                                 SeriesRepository seriesRepository,
                                 MarketStatusResolver marketStatusResolver,
                                 OutcomePricesMapper outcomePricesMapper,
                                 @Value("${polymarket.gamma-url}") String gammaBaseUrl) {
        this.restClient = restClient;
        this.marketMapper = marketMapper;
        this.marketRepository = marketRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.seriesRepository = seriesRepository;
        this.marketStatusResolver = marketStatusResolver;
        this.outcomePricesMapper = outcomePricesMapper;
        this.gammaBaseUrl = gammaBaseUrl;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncSeriesEvent(String seriesPolymarketId) {
        Series series = seriesRepository.getByPolymarketId(seriesPolymarketId)
                .orElseThrow(() -> new IllegalStateException(
                        "No Series found for polymarketId=" + seriesPolymarketId));

        String apiUrl = UriComponentsBuilder
                .fromUriString(gammaBaseUrl + "/events")
                .queryParam("series_id", seriesPolymarketId)
                .queryParam("closed", false)
                .queryParam("end_date_min", Instant.now().toString())
                .queryParam("order", "startDate")
                .queryParam("ascending", true)
                .queryParam("limit", 1)
                .build()
                .toUriString();

        List<EventResponseDto> events = restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (events == null || events.isEmpty()
                || events.getFirst().getMarkets() == null
                || events.getFirst().getMarkets().isEmpty()) {
            log.info("No current or upcoming market found for series [{}]", seriesPolymarketId);
            return;
        }

        MarketResponseDto marketDto = events.getFirst().getMarkets().getFirst();

        if (marketRepository.existsById(marketDto.getId())) {
            log.debug("Market [{}] for series [{}] already known; refresh is open-market-sync's job",
                    marketDto.getId(), seriesPolymarketId);
            return;
        }

        Market market = marketMapper.toEntity(marketDto);
        market.setSeries(series);
        market.setLastSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        marketRepository.save(market);
        log.info("New market saved: {} (series [{}])", market.getId(), seriesPolymarketId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshOpenMarket(String marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new IllegalStateException("Market not found: " + marketId));

        String apiUrl = UriComponentsBuilder
                .fromUriString(gammaBaseUrl + "/markets/{id}")
                .buildAndExpand(market.getId())
                .toUriString();

        MarketResponseDto marketDto = restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(MarketResponseDto.class);

        if (marketDto == null) {
            log.warn("Empty response for market [{}], skipping", marketId);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MarketStatus newStatus = marketStatusResolver.resolve(marketDto);

        boolean changed = newStatus != market.getStatus()
                || valueChanged(outcomePricesMapper.getUpPrice(marketDto.getOutcomePrices()), market.getUpPrice())
                || valueChanged(outcomePricesMapper.getDownPrice(marketDto.getOutcomePrices()), market.getDownPrice())
                || valueChanged(marketDto.getVolume24hr(), market.getVolume24h())
                || valueChanged(marketDto.getLiquidityNum(), market.getLiquidity());

        if (!changed) {
            marketRepository.updateLastSyncedAt(marketId, now);
            return;
        }

        // Snapshot the row as it stands before we overwrite it — this is how
        // price history accumulates (the snapshot captures the OLD values).
        priceSnapshotRepository.save(PriceSnapshot.builder()
                .market(market)
                .yesPrice(market.getUpPrice())
                .noPrice(market.getDownPrice())
                .volume24h(market.getVolume24h())
                .liquidity(market.getLiquidity())
                .snapshotAt(now)
                .build());

        marketMapper.updateEntity(marketDto, market);
        market.setLastSyncedAt(now);
        marketRepository.save(market);

        if (newStatus != MarketStatus.OPEN) {
            log.info("Market [{}] transitioned out of OPEN -> {}", marketId, newStatus);
        } else {
            log.debug("Market [{}] refreshed", marketId);
        }
    }

    /**
     * True when {@code fresh} carries a value that differs from what is stored,
     * compared at the DB's scale so sub-scale noise from the API isn't mistaken
     * for a move. A null {@code fresh} means "not reported this cycle" (Gamma
     * omits these once resolved) — not a change.
     */
    private static boolean valueChanged(BigDecimal fresh, BigDecimal stored) {
        if (fresh == null) {
            return false;
        }
        if (stored == null) {
            return true;
        }
        return fresh.setScale(PERSISTED_SCALE, RoundingMode.HALF_UP).compareTo(stored) != 0;
    }
}
