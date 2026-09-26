package com.polypilot.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

/** A {@link ComparisonOperand} backed by a fixed value — the "compare against a threshold" case. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiteralOperand implements ComparisonOperand {

    private BigDecimal value;

    @Override
    public BigDecimal resolve(EvaluationContext ctx) {
        return value;
    }

    /** A literal reads no indicator — nothing to collect. */
    @Override
    public void collectIndicatorRequests(Set<IndicatorRequestKey> out) {
        // no-op by design
    }
}
