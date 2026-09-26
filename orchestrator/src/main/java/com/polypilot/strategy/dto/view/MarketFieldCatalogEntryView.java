package com.polypilot.strategy.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import com.polypilot.strategy.enums.MarketFieldDataType;
import com.polypilot.strategy.enums.MarketFieldValueScale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One entry of the code-owned market-field catalog — the {@link MarketField} counterpart to
 * {@code IndicatorView} in {@code marketFields} of {@code GET /strategies/condition-fields}'s
 * response. See {@code MarketFieldCatalogService}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketFieldCatalogEntryView {

    private MarketField field;
    private String name;
    private MarketFieldDataType dataType;

    /** {@code NUMBER} fields only; omitted for {@code sentiment} (ENUM). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MarketFieldValueScale valueScale;

    /** {@code sentiment} only; omitted for every {@code NUMBER} field. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> allowedValues;

    private List<CompareOperator> operators;
}
