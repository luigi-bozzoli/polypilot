package com.polypilot.strategy.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full response body of {@code GET /strategies/condition-fields}: both leaf kinds a rule-tree
 * condition can be built from, {@code MARKET_FIELD} and {@code INDICATOR}. See
 * {@code StrategyConditionFieldsService}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionFieldsCatalogView {

    private List<MarketFieldCatalogEntryView> marketFields;
    private List<IndicatorConditionFieldView> indicators;
}
