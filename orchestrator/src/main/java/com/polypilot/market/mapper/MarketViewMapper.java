package com.polypilot.market.mapper;

import com.polypilot.market.dto.view.MarketPricePointView;
import com.polypilot.market.dto.view.MarketView;
import com.polypilot.market.dto.view.OhlcCandleView;
import com.polypilot.market.dto.view.SeriesDetailView;
import com.polypilot.market.dto.view.SeriesSummaryView;
import com.polypilot.market.entity.Market;
import com.polypilot.market.entity.PriceSnapshot;
import com.polypilot.market.entity.Series;
import com.polypilot.ohlc.entity.OhlcCandle;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Entity → outbound view mapping for the dashboard read endpoints. Hand-written
 * (not MapStruct) — the mappings are trivial and this keeps the annotation
 * processor chain in {@code pom.xml} untouched.
 */
@Component
public class MarketViewMapper {

    public MarketView toMarketView(Market market) {
        if (market == null) {
            return null;
        }
        return MarketView.builder()
                .id(market.getId())
                .question(market.getQuestion())
                .category(market.getCategory())
                .status(market.getStatus())
                .upPrice(market.getUpPrice())
                .downPrice(market.getDownPrice())
                .volume24h(market.getVolume24h())
                .liquidity(market.getLiquidity())
                .resolutionDate(market.getResolutionDate())
                .outcome(market.getOutcome())
                .lastSyncedAt(market.getLastSyncedAt())
                .build();
    }

    /** A stored snapshot row → history point. */
    public MarketPricePointView toPricePoint(PriceSnapshot snapshot) {
        return MarketPricePointView.builder()
                .at(snapshot.getSnapshotAt())
                .upPrice(snapshot.getYesPrice())
                .downPrice(snapshot.getNoPrice())
                .volume24h(snapshot.getVolume24h())
                .liquidity(snapshot.getLiquidity())
                .build();
    }

    /**
     * Synthetic trailing point built from the live {@code markets} row so the
     * chart line reaches "now" rather than stopping at the last snapshot.
     */
    public MarketPricePointView toPricePoint(Market market, OffsetDateTime at) {
        return MarketPricePointView.builder()
                .at(at)
                .upPrice(market.getUpPrice())
                .downPrice(market.getDownPrice())
                .volume24h(market.getVolume24h())
                .liquidity(market.getLiquidity())
                .build();
    }

    /** A stored candle row → chart candle. */
    public OhlcCandleView toCandleView(OhlcCandle candle) {
        return OhlcCandleView.builder()
                .openTime(candle.getId().getOpenTime())
                .open(candle.getOpen())
                .high(candle.getHigh())
                .low(candle.getLow())
                .close(candle.getClose())
                .volume(candle.getVolume())
                .closed(candle.isClosed())
                .build();
    }

    public SeriesSummaryView toSummary(Series series, Market currentMarket, long openCount, long resolvedCount) {
        return SeriesSummaryView.builder()
                .id(series.getId())
                .polymarketId(series.getPolymarketId())
                .ticker(series.getTicker())
                .slug(series.getSlug())
                .title(series.getTitle())
                .recurrence(series.getRecurrence())
                .seriesType(series.getSeriesType())
                .openMarketCount(openCount)
                .resolvedMarketCount(resolvedCount)
                .currentMarket(toMarketView(currentMarket))
                .build();
    }

    public SeriesDetailView toDetail(Series series, List<Market> markets) {
        return SeriesDetailView.builder()
                .id(series.getId())
                .polymarketId(series.getPolymarketId())
                .ticker(series.getTicker())
                .slug(series.getSlug())
                .title(series.getTitle())
                .recurrence(series.getRecurrence())
                .seriesType(series.getSeriesType())
                .markets(markets.stream().map(this::toMarketView).toList())
                .build();
    }
}
