package com.polypilot.strategy.service;

import com.polypilot.indicator.dto.view.IndicatorParameterView;
import com.polypilot.indicator.dto.view.IndicatorView;
import com.polypilot.indicator.service.IndicatorCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Validates one {@code INDICATOR} rule-tree leaf against {@link IndicatorCatalogService}'s
 * catalog — the {@code INDICATOR} counterpart to {@link MarketFieldConditionValidator}, backing
 * {@code POST /strategies}'s rule-tree validation (per
 * {@code create-strategy-api-contracts.md}: the indicator must exist, every required parameter
 * must be supplied, and {@code outputField} must be real). Deliberately stays at those three
 * checks — it does not validate supplied parameter values against each parameter's
 * {@code allowedValues}/{@code constraints}, unlike {@code IndicatorCalculationService}'s own
 * {@code resolveParams}, which resolves values for actual computation rather than structural
 * validation at creation time.
 *
 * <p>Searches {@link IndicatorCatalogService#getCatalog()} rather than calling
 * {@code getIndicator(key)} directly: that method 404s on an unknown/disabled key, but every
 * rule-tree validation failure here is a 422 per the contract, so this mirrors
 * {@link MarketFieldConditionValidator}'s approach of checking catalog membership itself.
 */
@Service
@RequiredArgsConstructor
public class IndicatorConditionValidator {

    private final IndicatorCatalogService indicatorCatalogService;

    public void validate(String indicatorKey, Map<String, String> params, String outputField) {
        IndicatorView indicator = indicatorCatalogService.getCatalog().getIndicators().stream()
                .filter(i -> i.getKey().equals(indicatorKey))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_CONTENT, "Unknown indicator: " + indicatorKey));

        for (IndicatorParameterView parameter : indicator.getParameters()) {
            if (parameter.isRequired() && (params == null || !params.containsKey(parameter.getKey()))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "Missing required parameter [%s] for indicator [%s]"
                                .formatted(parameter.getKey(), indicatorKey));
            }
        }

        boolean validOutputField = indicator.getOutputs().stream()
                .anyMatch(output -> output.getKey().equals(outputField));
        if (!validOutputField) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Unknown output field [%s] for indicator [%s]".formatted(outputField, indicatorKey));
        }
    }
}
