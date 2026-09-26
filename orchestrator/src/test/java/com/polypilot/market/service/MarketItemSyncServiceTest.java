package com.polypilot.market.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class MarketItemSyncServiceTest {

    private static final String GAMMA = "https://gamma.test";

    @Mock MarketMapper marketMapper;
    @Mock MarketRepository marketRepository;
    @Mock PriceSnapshotRepository priceSnapshotRepository;
    @Mock SeriesRepository seriesRepository;

    private MockRestServiceServer server;
    private MarketItemSyncService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new MarketItemSyncService(
                builder.build(),
                marketMapper,
                marketRepository,
                priceSnapshotRepository,
                seriesRepository,
                new MarketStatusResolver(),
                new OutcomePricesMapper(JsonMapper.builder().build()),
                GAMMA);
    }

    // --- refreshOpenMarket ---------------------------------------------------

    @Test
    void openMarket_withNothingChanged_onlyBumpsLastSyncedAt() {
        Market stored = openMarket();
        when(marketRepository.findById("mkt-1")).thenReturn(Optional.of(stored));
        server.expect(requestTo(GAMMA + "/markets/mkt-1")).andRespond(withSuccess(
                gammaMarket("mkt-1", "[\"0.505\", \"0.495\"]",
                        ",\"volume24hr\":28.960783,\"liquidityNum\":10023.6243,\"closed\":false"),
                MediaType.APPLICATION_JSON));

        service.refreshOpenMarket("mkt-1");

        server.verify();
        verify(marketRepository).updateLastSyncedAt(eq("mkt-1"), any(OffsetDateTime.class));
        verify(priceSnapshotRepository, never()).save(any());
        verify(marketMapper, never()).updateEntity(any(), any());
        verify(marketRepository, never()).save(any());
    }

    @Test
    void openMarket_withMovedPrice_snapshotsOldValuesThenUpdates() {
        Market stored = openMarket();
        when(marketRepository.findById("mkt-1")).thenReturn(Optional.of(stored));
        server.expect(requestTo(GAMMA + "/markets/mkt-1")).andRespond(withSuccess(
                gammaMarket("mkt-1", "[\"0.61\", \"0.39\"]",
                        ",\"volume24hr\":28.960783,\"liquidityNum\":10023.6243,\"closed\":false"),
                MediaType.APPLICATION_JSON));

        service.refreshOpenMarket("mkt-1");

        ArgumentCaptor<PriceSnapshot> snap = ArgumentCaptor.forClass(PriceSnapshot.class);
        verify(priceSnapshotRepository).save(snap.capture());
        assertThat(snap.getValue().getYesPrice()).isEqualByComparingTo("0.5050");
        assertThat(snap.getValue().getNoPrice()).isEqualByComparingTo("0.4950");
        assertThat(snap.getValue().getVolume24h()).isEqualByComparingTo("28.9608");
        assertThat(snap.getValue().getSnapshotAt()).isNotNull();

        verify(marketMapper).updateEntity(any(), eq(stored));
        verify(marketRepository).save(stored);
        assertThat(stored.getLastSyncedAt()).isNotNull();
        verify(marketRepository, never()).updateLastSyncedAt(any(), any());
    }

    @Test
    void openMarket_thatResolved_isTreatedAsAChangeAndUpdated() {
        Market stored = openMarket();
        when(marketRepository.findById("mkt-1")).thenReturn(Optional.of(stored));
        server.expect(requestTo(GAMMA + "/markets/mkt-1")).andRespond(withSuccess(
                gammaMarket("mkt-1", "[\"1\", \"0\"]",
                        ",\"closed\":true,\"umaResolutionStatus\":\"resolved\""
                                + ",\"closedTime\":\"2026-08-30 14:55:52+00\""
                                + ",\"endDate\":\"2026-08-30T14:55:00Z\""),
                MediaType.APPLICATION_JSON));

        service.refreshOpenMarket("mkt-1");

        verify(priceSnapshotRepository).save(any(PriceSnapshot.class));
        verify(marketMapper).updateEntity(any(), eq(stored));
        verify(marketRepository).save(stored);
        verify(marketRepository, never()).updateLastSyncedAt(any(), any());
    }

    // --- syncSeriesEvent ---------------------------------------------------

    @Test
    void newMarket_isInsertedWithSeriesAndSyncStamp() {
        Series series = series();
        when(seriesRepository.getByPolymarketId("10684")).thenReturn(Optional.of(series));
        when(marketRepository.existsById("mkt-new")).thenReturn(false);
        when(marketMapper.toEntity(any())).thenReturn(Market.builder().id("mkt-new").build());
        server.expect(requestTo(containsString(GAMMA + "/events?series_id=10684")))
                .andRespond(withSuccess(eventWithMarket("mkt-new"), MediaType.APPLICATION_JSON));

        service.syncSeriesEvent("10684");

        ArgumentCaptor<Market> saved = ArgumentCaptor.forClass(Market.class);
        verify(marketRepository).save(saved.capture());
        assertThat(saved.getValue().getSeries()).isSameAs(series);
        assertThat(saved.getValue().getLastSyncedAt()).isNotNull();
    }

    @Test
    void knownMarket_isLeftForTheRefreshPass() {
        when(seriesRepository.getByPolymarketId("10684")).thenReturn(Optional.of(series()));
        when(marketRepository.existsById("mkt-new")).thenReturn(true);
        server.expect(requestTo(containsString(GAMMA + "/events?series_id=10684")))
                .andRespond(withSuccess(eventWithMarket("mkt-new"), MediaType.APPLICATION_JSON));

        service.syncSeriesEvent("10684");

        verify(marketMapper, never()).toEntity(any());
        verify(marketRepository, never()).save(any());
    }

    @Test
    void noUpcomingEvent_isANoOp() {
        when(seriesRepository.getByPolymarketId("10684")).thenReturn(Optional.of(series()));
        server.expect(requestTo(containsString(GAMMA + "/events?series_id=10684")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        service.syncSeriesEvent("10684");

        verify(marketRepository, never()).existsById(any());
        verify(marketRepository, never()).save(any());
    }

    // --- fixtures --------------------------------------------------------------

    private static Market openMarket() {
        return Market.builder()
                .id("mkt-1")
                .status(MarketStatus.OPEN)
                .upPrice(new BigDecimal("0.5050"))
                .downPrice(new BigDecimal("0.4950"))
                .volume24h(new BigDecimal("28.9608"))
                .liquidity(new BigDecimal("10023.6243"))
                .build();
    }

    private static Series series() {
        return Series.builder()
                .id(UUID.randomUUID())
                .polymarketId("10684")
                .ticker("btc-up-or-down-5m")
                .slug("btc-up-or-down-5m")
                .title("BTC Up or Down 5m")
                .recurrence("5m")
                .build();
    }

    private static String gammaMarket(String id, String outcomePricesArray, String moreFields) {
        return "{"
                + "\"id\":\"" + id + "\","
                + "\"question\":\"Q " + id + "\","
                + "\"conditionId\":\"0xcond-" + id + "\","
                + "\"outcomePrices\":\"" + outcomePricesArray.replace("\"", "\\\"") + "\""
                + moreFields
                + "}";
    }

    private static String eventWithMarket(String marketId) {
        return "[{\"id\":\"evt-1\",\"slug\":\"btc-updown\",\"markets\":["
                + gammaMarket(marketId, "[\"0.5\", \"0.5\"]",
                        ",\"volume24hr\":10,\"liquidityNum\":100,\"closed\":false,\"endDate\":\"2026-09-06T14:05:00Z\"")
                + "]}]";
    }
}
