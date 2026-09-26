package com.polypilot.strategy;

import com.polypilot.strategy.enums.BooleanOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Binary boolean combinator (AND / OR / XOR) over two child nodes.
 *
 * <p>Both children are always evaluated — no short-circuiting — since both
 * sides' indicators were already computed in the parallel pre-pass, so
 * skipping the second {@code evaluate()} call saves nothing and short-circuit
 * semantics would only add a case to reason about. See the design doc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BooleanNode extends StrategyNode {

    private BooleanOperator operator;
    private StrategyNode left;
    private StrategyNode right;


    @Override
    public boolean evaluate(EvaluationContext ctx) {
        boolean leftResult = left.evaluate(ctx);
        boolean rightResult = right.evaluate(ctx);
        return switch (operator) {
            case AND -> leftResult && rightResult;
            case OR -> leftResult || rightResult;
            case XOR -> leftResult ^ rightResult;
        };
    }

    @Override
    public void collectIndicatorRequests(Set<IndicatorRequestKey> out) {
        left.collectIndicatorRequests(out);
        right.collectIndicatorRequests(out);
    }

    @Override
    public boolean usesMarketFields() {
        return left.usesMarketFields() || right.usesMarketFields();
    }
}
