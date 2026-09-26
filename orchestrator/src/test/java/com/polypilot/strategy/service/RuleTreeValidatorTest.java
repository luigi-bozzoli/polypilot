package com.polypilot.strategy.service;

import com.polypilot.enums.SentimentType;
import com.polypilot.strategy.BooleanNode;
import com.polypilot.strategy.CompareNode;
import com.polypilot.strategy.IndicatorOperand;
import com.polypilot.strategy.IndicatorRequestKey;
import com.polypilot.strategy.LiteralOperand;
import com.polypilot.strategy.MarketFieldCompareNode;
import com.polypilot.strategy.StrategyNode;
import com.polypilot.strategy.UnaryBooleanNode;
import com.polypilot.strategy.enums.BooleanOperator;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import com.polypilot.strategy.enums.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RuleTreeValidatorTest {

    // Real: pure code-owned catalog, nothing worth stubbing (mirrors MarketFieldConditionValidatorTest).
    private final MarketFieldConditionValidator marketFieldConditionValidator =
            new MarketFieldConditionValidator(new MarketFieldCatalogService());
    @Mock IndicatorConditionValidator indicatorConditionValidator;

    RuleTreeValidator validator() {
        return new RuleTreeValidator(marketFieldConditionValidator, indicatorConditionValidator);
    }

    private static MarketFieldCompareNode upPriceLeaf() {
        return new MarketFieldCompareNode(MarketField.UP_PRICE, CompareOperator.LT, BigDecimal.valueOf(0.5), null);
    }

    @Test
    void singleLeafTree_isValid() {
        assertThatCode(() -> validator().validate(upPriceLeaf())).doesNotThrowAnyException();
    }

    @Test
    void sentimentLeaf_passesTheEnumNameThrough() {
        MarketFieldCompareNode sentiment =
                new MarketFieldCompareNode(MarketField.SENTIMENT, CompareOperator.EQ, null, SentimentType.BULLISH);

        assertThatCode(() -> validator().validate(sentiment)).doesNotThrowAnyException();
    }

    @Test
    void invalidMarketFieldLeaf_propagatesAs422() {
        // volume_24h's catalog entry doesn't allow EQ.
        MarketFieldCompareNode invalid =
                new MarketFieldCompareNode(MarketField.VOLUME_24H, CompareOperator.EQ, BigDecimal.TEN, null);

        assertThatThrownBy(() -> validator().validate(invalid))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void indicatorLeaf_delegatesToIndicatorConditionValidator() {
        CompareNode leaf = new CompareNode(
                new IndicatorOperand(new IndicatorRequestKey("rsi", Map.of("period", "14")), "value"),
                CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));

        validator().validate(leaf);

        verify(indicatorConditionValidator).validate("rsi", Map.of("period", "14"), "value");
    }

    @Test
    void indicatorVsIndicatorLeaf_validatesBothIndicators() {
        CompareNode leaf = new CompareNode(
                new IndicatorOperand(new IndicatorRequestKey("rsi", Map.of("period", "14")), "value"),
                CompareOperator.GT,
                new IndicatorOperand(new IndicatorRequestKey("macd", Map.of("fast_period", "12")), "signal"));

        validator().validate(leaf);

        verify(indicatorConditionValidator).validate("rsi", Map.of("period", "14"), "value");
        verify(indicatorConditionValidator).validate("macd", Map.of("fast_period", "12"), "signal");
    }

    @Test
    void nestedBooleanAndUnaryNodes_walkTheWholeTree() {
        StrategyNode tree = new UnaryBooleanNode(UnaryOperator.NOT,
                new BooleanNode(BooleanOperator.AND, upPriceLeaf(), upPriceLeaf()));

        assertThatCode(() -> validator().validate(tree)).doesNotThrowAnyException();
    }

    @Test
    void invalidLeafDeepInATree_stillFails() {
        StrategyNode invalidDeep = new BooleanNode(BooleanOperator.AND,
                upPriceLeaf(),
                new MarketFieldCompareNode(MarketField.VOLUME_24H, CompareOperator.EQ, BigDecimal.TEN, null));

        assertThatThrownBy(() -> validator().validate(invalidDeep))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void treeExceedingTheNodeCap_throws422() {
        // A right-leaning chain of 40 leaves is (40 - 1) BooleanNodes + 40 leaves = 79 nodes, over the 64 cap.
        StrategyNode tree = upPriceLeaf();
        for (int i = 0; i < 39; i++) {
            tree = new BooleanNode(BooleanOperator.AND, upPriceLeaf(), tree);
        }
        StrategyNode oversized = tree;

        assertThatThrownBy(() -> validator().validate(oversized))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
