package com.polypilot.strategy.service;

import com.polypilot.strategy.dto.view.MarketFieldCatalogEntryView;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import com.polypilot.strategy.enums.MarketFieldDataType;
import com.polypilot.strategy.enums.MarketFieldValueScale;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Code-owned catalog of the six {@link MarketField}s a strategy's rule tree can compare against —
 * the {@link MarketField} counterpart to {@code IndicatorCatalogService}, but with no DB table
 * backing it: these are fixed {@code markets}/{@code sentiment_scores} columns, not seed data that
 * grows over time. Backs the {@code marketFields} half of
 * {@code GET /strategies/condition-fields}; see {@code StrategyConditionFieldsService}.
 */
@Service
public class MarketFieldCatalogService {

    private static final List<MarketFieldCatalogEntryView> CATALOG = List.of(
            MarketFieldCatalogEntryView.builder()
                    .field(MarketField.UP_PRICE)
                    .name("YES probability")
                    .dataType(MarketFieldDataType.NUMBER)
                    .valueScale(MarketFieldValueScale.OSCILLATOR_0_1)
                    .operators(List.of(CompareOperator.LT, CompareOperator.GT, CompareOperator.EQ))
                    .build(),
            MarketFieldCatalogEntryView.builder()
                    .field(MarketField.DOWN_PRICE)
                    .name("NO probability")
                    .dataType(MarketFieldDataType.NUMBER)
                    .valueScale(MarketFieldValueScale.OSCILLATOR_0_1)
                    .operators(List.of(CompareOperator.LT, CompareOperator.GT, CompareOperator.EQ))
                    .build(),
            MarketFieldCatalogEntryView.builder()
                    .field(MarketField.VOLUME_24H)
                    .name("24h volume")
                    .dataType(MarketFieldDataType.NUMBER)
                    .valueScale(MarketFieldValueScale.VOLUME)
                    .operators(List.of(CompareOperator.LT, CompareOperator.GT))
                    .build(),
            MarketFieldCatalogEntryView.builder()
                    .field(MarketField.LIQUIDITY)
                    .name("Liquidity")
                    .dataType(MarketFieldDataType.NUMBER)
                    .valueScale(MarketFieldValueScale.VOLUME)
                    .operators(List.of(CompareOperator.LT, CompareOperator.GT))
                    .build(),
            MarketFieldCatalogEntryView.builder()
                    .field(MarketField.SENTIMENT)
                    .name("AI sentiment is")
                    .dataType(MarketFieldDataType.ENUM)
                    .allowedValues(List.of("BULLISH", "NEUTRAL", "BEARISH"))
                    .operators(List.of(CompareOperator.EQ, CompareOperator.NEQ))
                    .build(),
            MarketFieldCatalogEntryView.builder()
                    .field(MarketField.SENTIMENT_CONFIDENCE)
                    .name("Sentiment confidence")
                    .dataType(MarketFieldDataType.NUMBER)
                    .valueScale(MarketFieldValueScale.OSCILLATOR_0_1)
                    .operators(List.of(CompareOperator.LT, CompareOperator.GT))
                    .build());

    /** All six market fields, in the order shown above. */
    public List<MarketFieldCatalogEntryView> getCatalog() {
        return CATALOG;
    }
}
