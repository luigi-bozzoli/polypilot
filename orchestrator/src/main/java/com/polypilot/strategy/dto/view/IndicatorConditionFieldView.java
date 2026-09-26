package com.polypilot.strategy.dto.view;

import com.polypilot.indicator.dto.view.IndicatorOutputView;
import com.polypilot.indicator.dto.view.IndicatorParameterView;
import com.polypilot.indicator.enums.IndicatorCategory;
import com.polypilot.strategy.enums.CompareOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One catalog indicator reshaped for the {@code indicators} array of
 * {@code GET /strategies/condition-fields}'s response: {@code IndicatorView} with its {@code key}
 * renamed to {@code indicatorKey} (per the rule-tree contract's {@code INDICATOR} leaf shape) and
 * a flat {@code operators} array added — every {@link CompareOperator}, uniformly, since (unlike
 * market fields) the catalog doesn't restrict which comparison an indicator output accepts. See
 * {@code StrategyConditionFieldsService}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorConditionFieldView {

    private String indicatorKey;
    private String name;
    private String abbreviation;
    private IndicatorCategory category;
    private List<IndicatorParameterView> parameters;
    private List<IndicatorOutputView> outputs;
    private List<CompareOperator> operators;
}
