package com.polypilot.strategy.service;

import com.polypilot.indicator.dto.view.IndicatorCatalogView;
import com.polypilot.indicator.dto.view.IndicatorOutputView;
import com.polypilot.indicator.dto.view.IndicatorParameterView;
import com.polypilot.indicator.dto.view.IndicatorView;
import com.polypilot.indicator.enums.IndicatorCategory;
import com.polypilot.indicator.enums.ParameterDataType;
import com.polypilot.indicator.service.IndicatorCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicatorConditionValidatorTest {

    @Mock IndicatorCatalogService indicatorCatalogService;

    IndicatorConditionValidator validator() {
        return new IndicatorConditionValidator(indicatorCatalogService);
    }

    private static IndicatorCatalogView rsiCatalog() {
        return IndicatorCatalogView.builder()
                .indicators(List.of(IndicatorView.builder()
                        .key("rsi")
                        .name("Relative Strength Index")
                        .abbreviation("RSI")
                        .category(IndicatorCategory.MOMENTUM)
                        .parameters(List.of(
                                IndicatorParameterView.builder()
                                        .key("period").name("Period")
                                        .dataType(ParameterDataType.INTEGER)
                                        .required(true)
                                        .build(),
                                IndicatorParameterView.builder()
                                        .key("timeframe").name("Timeframe")
                                        .dataType(ParameterDataType.ENUM)
                                        .required(true)
                                        .build()))
                        .outputs(List.of(IndicatorOutputView.builder()
                                .key("value").name("RSI value")
                                .build()))
                        .build()))
                .build();
    }

    @Test
    void unknownIndicatorKey_throws422() {
        when(indicatorCatalogService.getCatalog()).thenReturn(rsiCatalog());

        assertThatThrownBy(() -> validator().validate("macd", Map.of(), "value"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void missingRequiredParameter_throws422() {
        when(indicatorCatalogService.getCatalog()).thenReturn(rsiCatalog());

        assertThatThrownBy(() -> validator().validate("rsi", Map.of("period", "14"), "value"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void unknownOutputField_throws422() {
        when(indicatorCatalogService.getCatalog()).thenReturn(rsiCatalog());

        assertThatThrownBy(() -> validator().validate(
                "rsi", Map.of("period", "14", "timeframe", "1h"), "signal"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void everyRequiredParameterPresentAndOutputFieldReal_passes() {
        when(indicatorCatalogService.getCatalog()).thenReturn(rsiCatalog());

        assertThatCode(() -> validator().validate(
                "rsi", Map.of("period", "14", "timeframe", "1h"), "value"))
                .doesNotThrowAnyException();
    }
}
