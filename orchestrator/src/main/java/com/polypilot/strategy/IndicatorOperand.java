package com.polypilot.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

/**
 * A {@link ComparisonOperand} backed by one indicator output field — the same
 * {@code (indicator, outputField)} pair {@link CompareNode} used to hold directly before indicator
 * comparisons existed, factored out so either side of a {@link CompareNode} can be one.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorOperand implements ComparisonOperand {

    private IndicatorRequestKey indicator;
    private String outputField;

    @Override
    public BigDecimal resolve(EvaluationContext ctx) {
        return ctx.valueFor(indicator, outputField);
    }

    @Override
    public void collectIndicatorRequests(Set<IndicatorRequestKey> out) {
        out.add(indicator);
    }
}
