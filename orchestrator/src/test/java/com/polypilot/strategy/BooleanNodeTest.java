package com.polypilot.strategy;

import com.polypilot.strategy.enums.BooleanOperator;
import com.polypilot.strategy.enums.CompareOperator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Truth tables for AND/OR/XOR, plus proof that a single {@link CompareNode}
 * evaluates directly with no {@link BooleanNode} wrapper required.
 */
class BooleanNodeTest {

    private static final IndicatorRequestKey RSI = new IndicatorRequestKey("rsi", Map.of("period", "14"));

    @Test
    void and_isTrueOnlyWhenBothChildrenAreTrue() {
        assertThat(booleanNode(BooleanOperator.AND, true, true).evaluate(emptyContext())).isTrue();
        assertThat(booleanNode(BooleanOperator.AND, true, false).evaluate(emptyContext())).isFalse();
        assertThat(booleanNode(BooleanOperator.AND, false, false).evaluate(emptyContext())).isFalse();
    }

    @Test
    void or_isTrueWhenEitherChildIsTrue() {
        assertThat(booleanNode(BooleanOperator.OR, true, false).evaluate(emptyContext())).isTrue();
        assertThat(booleanNode(BooleanOperator.OR, false, true).evaluate(emptyContext())).isTrue();
        assertThat(booleanNode(BooleanOperator.OR, false, false).evaluate(emptyContext())).isFalse();
    }

    @Test
    void xor_isTrueOnlyWhenChildrenDiffer() {
        assertThat(booleanNode(BooleanOperator.XOR, true, false).evaluate(emptyContext())).isTrue();
        assertThat(booleanNode(BooleanOperator.XOR, false, true).evaluate(emptyContext())).isTrue();
        assertThat(booleanNode(BooleanOperator.XOR, true, true).evaluate(emptyContext())).isFalse();
        assertThat(booleanNode(BooleanOperator.XOR, false, false).evaluate(emptyContext())).isFalse();
    }

    @Test
    void singleCompareNode_evaluatesDirectlyWithoutABooleanWrapper() {
        CompareNode node = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));
        EvaluationContext ctx = new EvaluationContext(Map.of(RSI, Map.of("value", BigDecimal.valueOf(80))), null);

        assertThat(node.evaluate(ctx)).isTrue();
    }

    @Test
    void collectIndicatorRequests_delegatesToBothChildrenAndDedupes() {
        CompareNode left = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));
        CompareNode right = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.LT, new LiteralOperand(BigDecimal.valueOf(90)));
        BooleanNode tree = new BooleanNode(BooleanOperator.AND, left, right);

        Set<IndicatorRequestKey> requests = new HashSet<>();
        tree.collectIndicatorRequests(requests);

        assertThat(requests).containsExactly(RSI);
    }

    private static BooleanNode booleanNode(BooleanOperator operator, boolean left, boolean right) {
        return new BooleanNode(operator, constant(left), constant(right));
    }

    private static StrategyNode constant(boolean value) {
        IndicatorRequestKey key = new IndicatorRequestKey("const", Map.of());
        CompareOperator operator = value ? CompareOperator.EQ : CompareOperator.NEQ;
        return new CompareNode(new IndicatorOperand(key, "value"), operator, new LiteralOperand(BigDecimal.ZERO));
    }

    private static EvaluationContext emptyContext() {
        IndicatorRequestKey key = new IndicatorRequestKey("const", Map.of());
        return new EvaluationContext(Map.of(key, Map.of("value", BigDecimal.ZERO)), null);
    }
}
