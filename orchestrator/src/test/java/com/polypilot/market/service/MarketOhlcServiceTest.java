package com.polypilot.market.service;

import com.polypilot.market.dto.view.MarketOhlcView;
import com.polypilot.market.entity.Market;
import com.polypilot.market.entity.Series;
import com.polypilot.market.mapper.MarketViewMapper;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.ohlc.entity.OhlcCandle;
import com.polypilot.ohlc.entity.OhlcCandleId;
import com.polypilot.ohlc.repository.OhlcCandleRepository;
import com.polypilot.reference.entity.Ticker;
import com.polypilot.reference.entity.Timeframe;
import com.polypilot.reference.repository.TimeframeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Covers the service's own responsibilities — market/asset resolution, the "no linked
 * asset" empty-response case, timeframe validation/defaulting, limit clamping, and the
 * newest-first-to-ascending reversal. Real {@link MarketViewMapper}, everything else mocked.
 */
@ExtendWith(MockitoExtension.class)
class MarketOhlcServiceTest {

    private static final String MARKET_ID = "m-1";
    private static final String SYMBOL = "BTCUSDT";

    @Mock
    private MarketRepository marketRepository;
    @Mock
    private OhlcCandleRepository ohlcCandleRepository;
    @Mock
    private TimeframeRepository timeframeRepository;

    private MarketOhlcService service;

    private final MarketViewMapper mapper = new MarketViewMapper();

    private MarketOhlcService newService() {
        return new MarketOhlcService(marketRepository, ohlcCandleRepository, timeframeRepository, mapper);
    }

    @Test
    void getOhlc_unknownMarket_throwsNotFound() {
        service = newService();
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOhlc(MARKET_ID, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOhlc_seriesWithNoLinkedAsset_returnsNullSymbolAndEmptyCandles() {
        service = newService();
        Series series = Series.builder().asset(null).build();
        Market market = Market.builder().id(MARKET_ID).series(series).build();
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(market));

        MarketOhlcView result = service.getOhlc(MARKET_ID, null, null);

        assertThat(result.getMarketId()).isEqualTo(MARKET_ID);
        assertThat(result.getSymbol()).isNull();
        assertThat(result.getTimeframe()).isEqualTo("1h");
        assertThat(result.getCandles()).isEmpty();
    }

    @Test
    void getOhlc_explicitUnknownTimeframe_throwsBadRequest() {
        service = newService();
        Series series = Series.builder().asset(Ticker.builder().binanceSymbol(SYMBOL).build()).build();
        Market market = Market.builder().id(MARKET_ID).series(series).build();
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(market));
        when(timeframeRepository.findById("bogus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOhlc(MARKET_ID, "bogus", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getOhlc_explicitDisabledTimeframe_throwsBadRequest() {
        service = newService();
        Series series = Series.builder().asset(Ticker.builder().binanceSymbol(SYMBOL).build()).build();
        Market market = Market.builder().id(MARKET_ID).series(series).build();
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(market));
        when(timeframeRepository.findById("1s")).thenReturn(
                Optional.of(Timeframe.builder().code("1s").enabled(false).build()));

        assertThatThrownBy(() -> service.getOhlc(MARKET_ID, "1s", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getOhlc_linkedAsset_reversesToAscendingAndClampsLimit() {
        service = newService();
        Series series = Series.builder().asset(Ticker.builder().binanceSymbol(SYMBOL).build()).build();
        Market market = Market.builder().id(MARKET_ID).series(series).build();
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(market));
        when(timeframeRepository.findById("1h")).thenReturn(
                Optional.of(Timeframe.builder().code("1h").enabled(true).build()));

        OffsetDateTime older = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime newer = OffsetDateTime.parse("2026-01-01T01:00:00Z");
        OhlcCandle newerCandle = candle(newer, false);
        OhlcCandle olderCandle = candle(older, true);
        // Repository contract is newest-first — assert the service reverses it.
        when(ohlcCandleRepository.findRecentCandles(eq(SYMBOL), eq("1h"), any(Pageable.class)))
                .thenReturn(List.of(newerCandle, olderCandle));

        MarketOhlcView result = service.getOhlc(MARKET_ID, "1h", 5000);

        assertThat(result.getSymbol()).isEqualTo(SYMBOL);
        assertThat(result.getCandles()).hasSize(2);
        assertThat(result.getCandles().get(0).getOpenTime()).isEqualTo(older);
        assertThat(result.getCandles().get(0).isClosed()).isTrue();
        assertThat(result.getCandles().get(1).getOpenTime()).isEqualTo(newer);
        assertThat(result.getCandles().get(1).isClosed()).isFalse();
    }

    private static OhlcCandle candle(OffsetDateTime openTime, boolean closed) {
        return OhlcCandle.builder()
                .id(new OhlcCandleId(SYMBOL, "1h", openTime))
                .open(BigDecimal.ONE)
                .high(BigDecimal.ONE)
                .low(BigDecimal.ONE)
                .close(BigDecimal.ONE)
                .volume(BigDecimal.ONE)
                .closeTime(openTime.plusHours(1))
                .closed(closed)
                .updatedAt(openTime)
                .build();
    }
}
