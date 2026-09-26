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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-side queries backing the dashboard's series/market views. Pure fan-in
 * over the existing repositories — no external calls, no writes.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SeriesQueryService {

    private final SeriesRepository seriesRepository;
    private final MarketRepository marketRepository;
    private final MarketViewMapper marketViewMapper;

    @Transactional(readOnly = true)
    public List<SeriesSummaryView> listSeries() {
        List<Series> allSeries = seriesRepository.findAll();

        // One OPEN market per series is the norm; if more than one slipped
        // through, keep the first and move on.
        Map<UUID, Market> openBySeries = marketRepository.findAllByStatus(MarketStatus.OPEN).stream()
                .filter(m -> m.getSeries() != null)
                .collect(Collectors.toMap(
                        m -> m.getSeries().getId(),
                        Function.identity(),
                        (first, dup) -> first));

        Map<UUID, long[]> countsBySeries = tallyCounts(marketRepository.countMarketsBySeriesAndStatus());

        return allSeries.stream()
                .map(series -> {
                    long[] counts = countsBySeries.getOrDefault(series.getId(), new long[2]);
                    return marketViewMapper.toSummary(
                            series,
                            openBySeries.get(series.getId()),
                            counts[0],
                            counts[1]);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SeriesDetailView getSeriesDetail(UUID seriesId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Series not found: " + seriesId));

        List<Market> markets = marketRepository.findBySeriesIdOrderByCreatedAtDesc(seriesId);
        return marketViewMapper.toDetail(series, markets);
    }

    /** seriesId → [openCount, nonOpenCount]. */
    private Map<UUID, long[]> tallyCounts(List<SeriesMarketCountView> rows) {
        Map<UUID, long[]> bySeries = new java.util.HashMap<>();
        for (SeriesMarketCountView row : rows) {
            long[] counts = bySeries.computeIfAbsent(row.getSeriesId(), k -> new long[2]);
            if (row.getStatus() == MarketStatus.OPEN) {
                counts[0] += row.getCount();
            } else {
                counts[1] += row.getCount();
            }
        }
        return bySeries;
    }
}
