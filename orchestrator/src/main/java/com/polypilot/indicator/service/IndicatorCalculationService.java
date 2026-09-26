package com.polypilot.indicator.service;

import com.polypilot.indicator.calc.IndicatorCalculator;
import com.polypilot.indicator.calc.IndicatorCalculatorRegistry;
import com.polypilot.indicator.calc.IndicatorParams;
import com.polypilot.indicator.calc.OhlcBarSeriesFactory;
import com.polypilot.indicator.entity.Indicator;
import com.polypilot.indicator.entity.IndicatorParameter;
import com.polypilot.indicator.enums.ParameterDataType;
import com.polypilot.indicator.repository.IndicatorRepository;
import com.polypilot.ohlc.entity.OhlcCandle;
import com.polypilot.ohlc.repository.OhlcCandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Synchronous, on-the-fly technical-indicator calculation over TA4J. Values
 * are computed fresh on every call and never persisted — see the design note
 * this package's architecture was chosen against in the roadmap discussion:
 * a config-driven {@link IndicatorCalculatorRegistry} keyed by the same
 * {@code indicators.key} the catalog already uses, rather than one class per
 * indicator.
 *
 * <p>End-to-end flow of {@link #calculate}:
 * <ol>
 *   <li>Look up the {@link Indicator} row (must
 *       be enabled) — its {@code parameters} supply defaults and drive
 *       validation.</li>
 *   <li>Merge caller-supplied {@code rawParams} with those defaults into an
 *       {@link IndicatorParams}; a required parameter with no value from
 *       either source is a 400.</li>
 *   <li>Read the resolved {@code timeframe} parameter to know which
 *       {@code (symbol, timeframe)} candle stream to pull — this is why every
 *       seeded indicator carries a {@code timeframe} parameter with no
 *       universal default (see {@code 002_seed_data.sql} 6a): it must come
 *       from the caller for every calculation.</li>
 *   <li>Fetch enough trailing <em>closed</em> candles
 *       ({@link OhlcCandleRepository#findRecentClosedCandles}) to give the
 *       indicator a stable reading, build a {@link BarSeries}
 *       ({@link OhlcBarSeriesFactory}), and delegate to the registered
 *       {@link IndicatorCalculator}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorCalculationService {

    /**
     * TA4J's moving averages (EMA/RSI/MACD's EMAs) are infinite-impulse-response
     * — they never fully "forget" bars before the window, only decay toward
     * negligible weight. This multiplier over the largest configured period is
     * a standard rule-of-thumb buffer for a visually/numerically stable reading,
     * not a mathematical guarantee. {@link #MIN_LOOKBACK} is the floor for
     * small periods (e.g. MACD's default signal_period of 9).
     */
    private static final int STABILITY_MULTIPLIER = 3;
    private static final int MIN_LOOKBACK = 50;

    private final IndicatorRepository indicatorRepository;
    private final OhlcCandleRepository ohlcCandleRepository;
    private final OhlcBarSeriesFactory barSeriesFactory;
    private final IndicatorCalculatorRegistry calculatorRegistry;

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> calculate(String binanceSymbol, String indicatorKey, Map<String, String> rawParams) {
        Indicator indicator = indicatorRepository.findByKey(indicatorKey)
                .filter(Indicator::getEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown indicator: " + indicatorKey));

        IndicatorParams params = resolveParams(indicator, rawParams);
        String timeframe = params.getString("timeframe");

        int lookback = requiredBarCount(indicator, params);
        List<OhlcCandle> candlesDesc = ohlcCandleRepository.findRecentClosedCandles(
                binanceSymbol, timeframe, PageRequest.of(0, lookback));
        if (candlesDesc.size() < lookback) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Not enough closed candles for %s %s [%s]: need %d, have %d"
                            .formatted(binanceSymbol, timeframe, indicatorKey, lookback, candlesDesc.size()));
        }
        // Reverse into a fresh list rather than in place: the repository result
        // isn't guaranteed mutable, and mutating it would also be surprising to
        // any caller that reuses the same list.
        List<OhlcCandle> candlesAsc = new ArrayList<>(candlesDesc);
        Collections.reverse(candlesAsc);

        BarSeries series = barSeriesFactory.build(binanceSymbol, timeframe, candlesAsc);
        IndicatorCalculator calculator = calculatorRegistry.get(indicatorKey);
        return calculator.calculate(series, params);
    }

    /**
     * Merges {@code rawParams} with each {@link IndicatorParameter}'s default
     * (local, or inherited from its {@code universal} parameter — e.g. {@code
     * source} defaults to {@code close}). {@code timeframe} has no universal
     * default by design, so it must always come from {@code rawParams}.
     */
    private IndicatorParams resolveParams(Indicator indicator, Map<String, String> rawParams) {
        Map<String, String> resolved = new HashMap<>();
        for (IndicatorParameter parameter : indicator.getParameters()) {
            String value = rawParams.get(parameter.getKey());
            if (value == null) {
                value = parameter.getDefaultValue();
            }
            if (value == null && parameter.getUniversal() != null) {
                value = parameter.getUniversal().getDefaultValue();
            }
            if (value == null && Boolean.TRUE.equals(parameter.getRequired())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Missing required parameter [%s] for indicator [%s]"
                                .formatted(parameter.getKey(), indicator.getKey()));
            }
            if (value != null) {
                resolved.put(parameter.getKey(), value);
            }
        }
        return new IndicatorParams(resolved);
    }

    private int requiredBarCount(Indicator indicator, IndicatorParams params) {
        int largestPeriod = indicator.getParameters().stream()
                .filter(p -> p.getDataType() == ParameterDataType.INTEGER)
                .mapToInt(p -> params.getInt(p.getKey()))
                .max()
                .orElse(MIN_LOOKBACK);
        return Math.max(MIN_LOOKBACK, largestPeriod * STABILITY_MULTIPLIER);
    }
}
