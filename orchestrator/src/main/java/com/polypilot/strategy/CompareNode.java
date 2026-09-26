package com.polypilot.strategy;

import com.polypilot.strategy.enums.CompareOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Leaf node: compares two {@link ComparisonOperand}s — an indicator output field against either a
 * fixed threshold ({@link LiteralOperand}) or another indicator's output field
 * ({@link IndicatorOperand}). A tree that's just a single {@code CompareNode} is valid and
 * evaluates directly — no {@link BooleanNode} wrapper is required.
 *
 * <p>{@code left} is always expected to be an {@link IndicatorOperand} — every construction path
 * ({@link com.polypilot.strategy.json.StrategyNodeDeserializer}, catalog-driven validation in
 * {@link com.polypilot.strategy.service.RuleTreeValidator}) only ever builds it that way — but
 * nothing here enforces that structurally; {@code left}/{@code right} are typed identically so a
 * third operand kind (e.g. a market field) fits either side without changing this class.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompareNode extends StrategyNode {

    private ComparisonOperand left;
    private CompareOperator operator;
    private ComparisonOperand right;

    @Override
    public boolean evaluate(EvaluationContext ctx) {
        int comparison = left.resolve(ctx).compareTo(right.resolve(ctx));
        return switch (operator) {
            case EQ -> comparison == 0;
            case NEQ -> comparison != 0;
            case GT -> comparison > 0;
            case GTE -> comparison >= 0;
            case LT -> comparison < 0;
            case LTE -> comparison <= 0;
        };
    }

    @Override
    public void collectIndicatorRequests(Set<IndicatorRequestKey> out) {
        left.collectIndicatorRequests(out);
        right.collectIndicatorRequests(out);
    }

    @Override
    public boolean usesMarketFields() {
        return false;
    }
}
