package com.polypilot.strategy.service;

import com.polypilot.strategy.dto.view.MarketFieldCatalogEntryView;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import com.polypilot.strategy.enums.MarketFieldDataType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates one {@code MARKET_FIELD} rule-tree leaf against the {@link MarketFieldCatalogService}
 * catalog — split out of any particular controller so the eventual {@code POST /strategies}
 * request-validation layer can reuse it as-is (per {@code create-strategy-api-contracts.md}'s
 * rule-tree validation rules), without this task inventing that endpoint.
 *
 * <p>Every failure is a {@code 422 Unprocessable Entity}, per the contract, using the same
 * {@link ResponseStatusException} + {@link HttpStatus#UNPROCESSABLE_CONTENT} convention already
 * used for indicator-calculation failures in {@code IndicatorCalculationService}.
 */
@Service
@RequiredArgsConstructor
public class MarketFieldConditionValidator {

    private final MarketFieldCatalogService marketFieldCatalogService;

    /**
     * @param field    the leaf's {@code field}
     * @param operator the leaf's {@code operator}
     * @param value    the leaf's raw {@code value} — a {@link Number} for numeric fields, a
     *                 {@link String} (one of {@code BULLISH}/{@code NEUTRAL}/{@code BEARISH}) for
     *                 {@code sentiment}
     */
    public void validate(MarketField field, CompareOperator operator, Object value) {
        MarketFieldCatalogEntryView entry = marketFieldCatalogService.getCatalog().stream()
                .filter(e -> e.getField() == field)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_CONTENT, "Unknown market field: " + field));

        if (!entry.getOperators().contains(operator)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Operator [%s] is not valid for market field [%s]".formatted(operator, field));
        }

        if (entry.getDataType() == MarketFieldDataType.ENUM) {
            if (!(value instanceof String stringValue) || !entry.getAllowedValues().contains(stringValue)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "Market field [%s] requires one of %s".formatted(field, entry.getAllowedValues()));
            }
        } else if (!(value instanceof Number)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Market field [%s] requires a numeric value".formatted(field));
        }
    }
}
