package com.polypilot.strategy;

import com.polypilot.strategy.enums.CompareOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CompareNodeTest {

    private static final IndicatorRequestKey RSI = new IndicatorRequestKey("rsi", Map.of("period", "14"));
    private static final IndicatorRequestKey MACD = new IndicatorRequestKey("macd", Map.of("fast_period", "12"));

    @Test
    void gt_lt_eq_operators_compareAgainstThresholdCorrectly() {
        assertThat(compareToLiteral(CompareOperator.GT, "80", "70")).isTrue();
        assertThat(compareToLiteral(CompareOperator.GT, "70", "70")).isFalse();
        assertThat(compareToLiteral(CompareOperator.GTE, "70", "70")).isTrue();
        assertThat(compareToLiteral(CompareOperator.LT, "60", "70")).isTrue();
        assertThat(compareToLiteral(CompareOperator.LT, "70", "70")).isFalse();
        assertThat(compareToLiteral(CompareOperator.LTE, "70", "70")).isTrue();
        assertThat(compareToLiteral(CompareOperator.EQ, "70", "70")).isTrue();
        assertThat(compareToLiteral(CompareOperator.EQ, "70.0", "70")).isTrue(); // compareTo, not equals — scale-insensitive
        assertThat(compareToLiteral(CompareOperator.NEQ, "70", "70")).isFalse();
        assertThat(compareToLiteral(CompareOperator.NEQ, "71", "70")).isTrue();
    }

    @ParameterizedTest
    @EnumSource(CompareOperator.class)
    void everyOperator_isHandled(CompareOperator operator) {
        // Smoke test: no operator falls through to an unhandled switch branch.
        compareToLiteral(operator, "70", "70");
    }

    @Test
    void collectIndicatorRequests_addsItsOwnIndicator() {
        CompareNode node = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));

        Set<IndicatorRequestKey> requests = new java.util.HashSet<>();
        node.collectIndicatorRequests(requests);

        assertThat(requests).containsExactly(RSI);
    }

    @Test
    void indicatorVsIndicator_resolvesBothOperandsFromTheContext() {
        CompareNode node = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.GT, new IndicatorOperand(MACD, "signal"));
        EvaluationContext ctx = new EvaluationContext(
                Map.of(RSI, Map.of("value", BigDecimal.valueOf(80)), MACD, Map.of("signal", BigDecimal.valueOf(1.2))),
                null);

        assertThat(node.evaluate(ctx)).isTrue();
    }

    @Test
    void indicatorVsIndicator_collectsBothIndicatorRequests() {
        CompareNode node = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.GT, new IndicatorOperand(MACD, "signal"));

        Set<IndicatorRequestKey> requests = new java.util.HashSet<>();
        node.collectIndicatorRequests(requests);

        assertThat(requests).containsExactlyInAnyOrder(RSI, MACD);
    }

    private boolean compareToLiteral(CompareOperator operator, String value, String threshold) {
        CompareNode node = new CompareNode(
                new IndicatorOperand(RSI, "value"), operator, new LiteralOperand(new BigDecimal(threshold)));
        EvaluationContext ctx = new EvaluationContext(Map.of(RSI, Map.of("value", new BigDecimal(value))), null);
        return node.evaluate(ctx);
    }
}
