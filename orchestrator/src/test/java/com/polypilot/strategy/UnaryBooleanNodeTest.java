package com.polypilot.strategy;

import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.UnaryOperator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UnaryBooleanNodeTest {

    private static final IndicatorRequestKey RSI = new IndicatorRequestKey("rsi", Map.of("period", "14"));

    @Test
    void not_negatesTheChildResult() {
        EvaluationContext ctx = new EvaluationContext(Map.of(RSI, Map.of("value", BigDecimal.valueOf(80))), null);
        CompareNode child = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));
        UnaryBooleanNode not = new UnaryBooleanNode(UnaryOperator.NOT, child);

        assertThat(child.evaluate(ctx)).isTrue();
        assertThat(not.evaluate(ctx)).isFalse();
    }

    @Test
    void collectIndicatorRequests_delegatesToChild() {
        CompareNode child = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));
        UnaryBooleanNode not = new UnaryBooleanNode(UnaryOperator.NOT, child);

        Set<IndicatorRequestKey> requests = new HashSet<>();
        not.collectIndicatorRequests(requests);

        assertThat(requests).containsExactly(RSI);
    }
}
