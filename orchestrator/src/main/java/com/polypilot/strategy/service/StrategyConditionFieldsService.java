package com.polypilot.strategy.service;

import com.polypilot.indicator.dto.view.IndicatorView;
import com.polypilot.indicator.service.IndicatorCatalogService;
import com.polypilot.strategy.dto.view.ConditionFieldsCatalogView;
import com.polypilot.strategy.dto.view.IndicatorConditionFieldView;
import com.polypilot.strategy.enums.CompareOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Composes the two independent catalogs that back {@code GET /strategies/condition-fields}:
 * {@link MarketFieldCatalogService} (code-owned market fields) and
 * {@link IndicatorCatalogService} (the existing DB-backed indicator catalog, reshaped — its own
 * API, {@code GET /indicators}, is untouched).
 */
@Service
@RequiredArgsConstructor
public class StrategyConditionFieldsService {

    /** Every indicator leaf accepts all six comparison operators — the catalog doesn't restrict this. */
    private static final List<CompareOperator> ALL_OPERATORS = List.of(CompareOperator.values());

    private final MarketFieldCatalogService marketFieldCatalogService;
    private final IndicatorCatalogService indicatorCatalogService;

    public ConditionFieldsCatalogView getConditionFields() {
        List<IndicatorConditionFieldView> indicators = indicatorCatalogService.getCatalog().getIndicators().stream()
                .map(this::toConditionField)
                .toList();

        return ConditionFieldsCatalogView.builder()
                .marketFields(marketFieldCatalogService.getCatalog())
                .indicators(indicators)
                .build();
    }

    private IndicatorConditionFieldView toConditionField(IndicatorView indicator) {
        return IndicatorConditionFieldView.builder()
                .indicatorKey(indicator.getKey())
                .name(indicator.getName())
                .abbreviation(indicator.getAbbreviation())
                .category(indicator.getCategory())
                .parameters(indicator.getParameters())
                .outputs(indicator.getOutputs())
                .operators(ALL_OPERATORS)
                .build();
    }
}
