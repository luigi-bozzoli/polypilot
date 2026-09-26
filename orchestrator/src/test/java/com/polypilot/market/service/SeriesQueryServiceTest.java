package com.polypilot.market.service;

import com.polypilot.market.dto.view.SeriesDetailView;
import com.polypilot.market.dto.view.SeriesSummaryView;
import com.polypilot.market.entity.Market;
import com.polypilot.market.entity.Series;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.mapper.MarketViewMapper;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.market.repository.SeriesMarketCountView;
import com.polypilot.market.repository.SeriesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeriesQueryServiceTest {

    @Mock SeriesRepository seriesRepository;
    @Mock MarketRepository marketRepository;

    // Real mapper — it is pure translation, nothing worth stubbing.
    SeriesQueryService service() {
        return new SeriesQueryService(seriesRepository, marketRepository, new MarketViewMapper());
    }

    private static Series series(UUID id, String ticker) {
        return Series.builder().id(id).polymarketId("p-" + ticker).ticker(ticker)
                .slug(ticker).title(ticker.toUpperCase()).recurrence("daily").build();
    }

    private static Market market(String id, Series series, MarketStatus status) {
        Market m = Market.builder().id(id).question("Q " + id).status(status).build();
        m.setSeries(series);
        return m;
    }

    private static SeriesMarketCountView count(UUID seriesId, MarketStatus status, long n) {
        return new SeriesMarketCountView() {
            public UUID getSeriesId() { return seriesId; }
            public MarketStatus getStatus() { return status; }
            public long getCount() { return n; }
        };
    }

    @Test
    void listSeries_attachesOpenMarketAndTalliesCounts() {
        UUID withOpen = UUID.randomUUID();
        UUID idle = UUID.randomUUID();
        Series a = series(withOpen, "btc-daily");
        Series b = series(idle, "eth-daily");

        when(seriesRepository.findAll()).thenReturn(List.of(a, b));
        when(marketRepository.findAllByStatus(MarketStatus.OPEN))
                .thenReturn(List.of(market("m-open", a, MarketStatus.OPEN)));
        when(marketRepository.countMarketsBySeriesAndStatus()).thenReturn(List.of(
                count(withOpen, MarketStatus.OPEN, 1),
                count(withOpen, MarketStatus.RESOLVED, 4),
                count(withOpen, MarketStatus.CLOSED, 1)));

        List<SeriesSummaryView> result = service().listSeries();

        assertThat(result).hasSize(2);
        SeriesSummaryView first = result.get(0);
        assertThat(first.getId()).isEqualTo(withOpen);
        assertThat(first.getCurrentMarket()).isNotNull();
        assertThat(first.getCurrentMarket().getId()).isEqualTo("m-open");
        assertThat(first.getOpenMarketCount()).isEqualTo(1);
        assertThat(first.getResolvedMarketCount()).isEqualTo(5); // RESOLVED + CLOSED

        SeriesSummaryView second = result.get(1);
        assertThat(second.getCurrentMarket()).isNull();
        assertThat(second.getOpenMarketCount()).isZero();
        assertThat(second.getResolvedMarketCount()).isZero();
    }

    @Test
    void getSeriesDetail_unknownId_throws404() {
        UUID missing = UUID.randomUUID();
        when(seriesRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getSeriesDetail(missing))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void getSeriesDetail_mapsMarketsInRepositoryOrder() {
        UUID id = UUID.randomUUID();
        Series s = series(id, "btc-daily");
        when(seriesRepository.findById(id)).thenReturn(Optional.of(s));
        when(marketRepository.findBySeriesIdOrderByCreatedAtDesc(id)).thenReturn(List.of(
                market("newest", s, MarketStatus.OPEN),
                market("older", s, MarketStatus.RESOLVED)));

        SeriesDetailView detail = service().getSeriesDetail(id);

        assertThat(detail.getTicker()).isEqualTo("btc-daily");
        assertThat(detail.getMarkets()).extracting("id").containsExactly("newest", "older");
    }
}
