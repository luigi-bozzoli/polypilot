package com.polypilot.indicator.service;

import com.polypilot.indicator.calc.IndicatorCalculatorRegistry;
import com.polypilot.indicator.calc.OhlcBarSeriesFactory;
import com.polypilot.indicator.entity.Indicator;
import com.polypilot.indicator.entity.IndicatorParameter;
import com.polypilot.indicator.entity.UniversalParameter;
import com.polypilot.indicator.enums.IndicatorCategory;
import com.polypilot.indicator.enums.ParameterDataType;
import com.polypilot.indicator.repository.IndicatorRepository;
import com.polypilot.ohlc.entity.OhlcCandle;
import com.polypilot.ohlc.entity.OhlcCandleId;
import com.polypilot.ohlc.repository.OhlcCandleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Covers the service's own responsibilities — parameter resolution/validation,
 * lookback sizing, and the insufficient-history guard — using the real
 * {@link OhlcBarSeriesFactory} and {@link IndicatorCalculatorRegistry} so the
 * success path also proves the whole pipeline produces a correct value, not
 * just that it calls through. Only the repositories are mocked.
 */
@ExtendWith(MockitoExtension.class)
class IndicatorCalculationServiceTest {

    private static final String SYMBOL = "BTCUSDT";
    private static final String TIMEFRAME = "1m";

    @Mock
    private IndicatorRepository indicatorRepository;
    @Mock
    private OhlcCandleRepository ohlcCandleRepository;

    private IndicatorCalculationService service;

    @BeforeEach
    void setUp() {
        service = new IndicatorCalculationService(
                indicatorRepository, ohlcCandleRepository, new OhlcBarSeriesFactory(), new IndicatorCalculatorRegistry());
    }

    @Test
    void calculate_unknownIndicator_throwsNotFound() {
        when(indicatorRepository.findByKey("sma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(SYMBOL, "sma", Map.of()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void calculate_disabledIndicator_throwsNotFound() {
        Indicator disabled = smaIndicator();
        disabled.setEnabled(false);
        when(indicatorRepository.findByKey("sma")).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.calculate(SYMBOL, "sma", Map.of()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void calculate_missingRequiredParamWithNoDefault_throwsBadRequest() {
        when(indicatorRepository.findByKey("sma")).thenReturn(Optional.of(smaIndicator()));

        // "timeframe" is required and has no universal default (by design), so
        // omitting it must fail fast rather than NPE deeper in the pipeline.
        assertThatThrownBy(() -> service.calculate(SYMBOL, "sma", Map.of("period", "5")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void calculate_notEnoughClosedCandles_throwsUnprocessableContent() {
        when(indicatorRepository.findByKey("sma")).thenReturn(Optional.of(smaIndicator()));
        when(ohlcCandleRepository.findRecentClosedCandles(eq(SYMBOL), eq(TIMEFRAME), any(Pageable.class)))
                .thenReturn(candles(10, BigDecimal.valueOf(100)));

        assertThatThrownBy(() -> service.calculate(SYMBOL, "sma", Map.of("period", "5", "timeframe", TIMEFRAME)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void calculate_appliesDefaultsAndReturnsCorrectValue() {
        when(indicatorRepository.findByKey("sma")).thenReturn(Optional.of(smaIndicator()));
        // period defaults to 30 -> lookback = max(50, 30*3) = 90 candles required.
        when(ohlcCandleRepository.findRecentClosedCandles(eq(SYMBOL), eq(TIMEFRAME), any(Pageable.class)))
                .thenReturn(candles(90, BigDecimal.valueOf(100)));

        // "source" and "period" both omitted from rawParams: source falls back to
        // its universal default (close), period falls back to its local default (30).
        Map<String, BigDecimal> result = service.calculate(SYMBOL, "sma", Map.of("timeframe", TIMEFRAME));

        assertThat(result.get("value")).isEqualByComparingTo("100");
    }

    @Test
    void calculate_explicitParamOverridesDefault() {
        when(indicatorRepository.findByKey("sma")).thenReturn(Optional.of(smaIndicator()));
        when(ohlcCandleRepository.findRecentClosedCandles(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(candles(50, BigDecimal.valueOf(42)));

        Map<String, BigDecimal> result = service.calculate(
                SYMBOL, "sma", Map.of("timeframe", TIMEFRAME, "period", "10", "source", "close"));

        assertThat(result.get("value")).isEqualByComparingTo("42");
    }

    /** Mirrors the seeded `sma` row: period (local default 30), source + timeframe (universal). */
    private static Indicator smaIndicator() {
        Indicator indicator = Indicator.builder()
                .key("sma")
                .name("Simple Moving Average")
                .abbreviation("SMA")
                .description("test")
                .category(IndicatorCategory.TREND)
                .enabled(true)
                .build();

        UniversalParameter source = UniversalParameter.builder()
                .key("source")
                .name("Source")
                .description("test")
                .dataType(ParameterDataType.ENUM)
                .allowedValues(List.of("open", "high", "low", "close", "hl2", "hlc3", "ohlc4"))
                .defaultValue("close")
                .build();

        UniversalParameter timeframe = UniversalParameter.builder()
                .key("timeframe")
                .name("Timeframe")
                .description("test")
                .dataType(ParameterDataType.ENUM)
                .allowedValues(List.of("1m", "5m", "1h"))
                .defaultValue(null)
                .build();

        Set<IndicatorParameter> parameters = new LinkedHashSet<>();
        parameters.add(IndicatorParameter.builder()
                .indicator(indicator).key("period").name("Period").description("test")
                .dataType(ParameterDataType.INTEGER).defaultValue("30").required(true).displayOrder(1)
                .build());
        parameters.add(IndicatorParameter.builder()
                .indicator(indicator).key("source").name("Source").description("test")
                .dataType(ParameterDataType.ENUM).universal(source).required(true).displayOrder(2)
                .build());
        parameters.add(IndicatorParameter.builder()
                .indicator(indicator).key("timeframe").name("Timeframe").description("test")
                .dataType(ParameterDataType.ENUM).universal(timeframe).required(true).displayOrder(3)
                .build());
        indicator.setParameters(parameters);

        return indicator;
    }

    private static List<OhlcCandle> candles(int count, BigDecimal close) {
        List<OhlcCandle> candles = new ArrayList<>();
        OffsetDateTime openTime = OffsetDateTime.now();
        for (int i = 0; i < count; i++) {
            candles.add(OhlcCandle.builder()
                    .id(new OhlcCandleId(SYMBOL, TIMEFRAME, openTime.minusMinutes(i)))
                    .open(close).high(close).low(close).close(close)
                    .volume(BigDecimal.TEN)
                    .closeTime(openTime.minusMinutes(i).plusMinutes(1))
                    .closed(true)
                    .updatedAt(openTime)
                    .build());
        }
        // repository contract: newest-first
        candles.sort((a, b) -> b.getId().getOpenTime().compareTo(a.getId().getOpenTime()));
        return Collections.unmodifiableList(candles);
    }
}
