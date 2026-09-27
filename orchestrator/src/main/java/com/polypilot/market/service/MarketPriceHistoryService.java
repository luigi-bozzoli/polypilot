package com.polypilot.market.service;

import com.polypilot.market.dto.view.MarketPriceHistoryView;
import com.polypilot.market.dto.view.MarketPricePointView;
import com.polypilot.market.entity.Market;
import com.polypilot.market.mapper.MarketViewMapper;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.market.repository.PriceSnapshotRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-side query for a market's probability / price history, backing the
 * dashboard chart.
 *
 * <p>Source rows come from {@code price_snapshots} (written by
 * {@code updateOpenMarkets} whenever a tracked market moves); a synthetic
 * trailing point is appended from the live {@code markets} row so the line
 * reaches "now".
 */
@Slf4j
@Service
@AllArgsConstructor
public class MarketPriceHistoryService {

    /** {@code <int><unit>}, unit h or d, case-insensitive. e.g. "12h", "7d". */
    private static final Pattern WINDOW = Pattern.compile("(\\d{1,4})\\s*([hHdD])");

    private static final Duration DEFAULT_WINDOW = Duration.ofHours(12);
    private static final String DEFAULT_WINDOW_LABEL = "12h";

    /** Server clamp — a longer look-back is silently capped at this. */
    private static final Duration MAX_WINDOW = Duration.ofDays(30);
    private static final String MAX_WINDOW_LABEL = "30d";

    private final MarketRepository marketRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final MarketViewMapper marketViewMapper;

    @Transactional(readOnly = true)
    public MarketPriceHistoryView getPriceHistory(String marketId, String windowParam) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Market not found: " + marketId));

        PriceHistoryWindow window = parseWindow(windowParam);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime since = now.minus(window.getDuration());

        List<MarketPricePointView> points = new ArrayList<>(
                priceSnapshotRepository
                        .findByMarket_IdAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(marketId, since)
                        .stream()
                        .map(marketViewMapper::toPricePoint)
                        .toList());

        // Append a synthetic "now" point from the live row so the line ends at
        // the current price. Skip it only when the market has no price at all
        // (brand-new, never synced) — then an empty history is a true empty state.
        if (market.getUpPrice() != null || market.getDownPrice() != null) {
            points.add(marketViewMapper.toPricePoint(market, now));
        }

        return MarketPriceHistoryView.builder()
                .marketId(marketId)
                .window(window.getLabel())
                .points(points)
                .build();
    }

    private PriceHistoryWindow parseWindow(String raw) {
        if (raw == null || raw.isBlank()) {
            return new PriceHistoryWindow(DEFAULT_WINDOW, DEFAULT_WINDOW_LABEL);
        }
        Matcher m = WINDOW.matcher(raw.trim());
        if (!m.matches()) {
            log.debug("Unparseable price-history window [{}], falling back to {}", raw, DEFAULT_WINDOW_LABEL);
            return new PriceHistoryWindow(DEFAULT_WINDOW, DEFAULT_WINDOW_LABEL);
        }
        long amount = Long.parseLong(m.group(1));
        boolean days = m.group(2).equalsIgnoreCase("d");
        Duration requested = days ? Duration.ofDays(amount) : Duration.ofHours(amount);

        if (requested.isZero() || requested.isNegative()) {
            return new PriceHistoryWindow(DEFAULT_WINDOW, DEFAULT_WINDOW_LABEL);
        }
        if (requested.compareTo(MAX_WINDOW) > 0) {
            return new PriceHistoryWindow(MAX_WINDOW, MAX_WINDOW_LABEL);
        }
        // Echo the caller's own normalised label (e.g. "24h", "7d").
        return new PriceHistoryWindow(requested, amount + (days ? "d" : "h"));
    }
}
