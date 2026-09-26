package com.polypilot.strategy;

import com.polypilot.strategy.enums.UnaryOperator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Unary boolean operator (NOT) over a single child node. A dedicated type
 * rather than a {@link BooleanNode} with a nullable second child — a
 * malformed NOT (wrong arity) is structurally impossible instead of a
 * runtime check every consumer would otherwise have to repeat.
 */
@Getter
@Setter
@NoArgsConstructor
public class UnaryBooleanNode extends StrategyNode {

    private UnaryOperator operator;
    private StrategyNode child;

    public UnaryBooleanNode(UnaryOperator operator, StrategyNode child) {
        this.operator = operator;
        this.child = child;
    }

    @Override
    public boolean evaluate(EvaluationContext ctx) {
        boolean childResult = child.evaluate(ctx);
        return switch (operator) {
            case NOT -> !childResult;
        };
    }

    @Override
    public void collectIndicatorRequests(Set<IndicatorRequestKey> out) {
        child.collectIndicatorRequests(out);
    }

    @Override
    public boolean usesMarketFields() {
        return child.usesMarketFields();
    }
}
