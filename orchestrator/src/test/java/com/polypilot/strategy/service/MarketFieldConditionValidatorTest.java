package com.polypilot.strategy.service;

import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketFieldConditionValidatorTest {

    private final MarketFieldConditionValidator validator =
            new MarketFieldConditionValidator(new MarketFieldCatalogService());

    @Test
    void sentiment_acceptsEqAndNeq() {
        assertThatCode(() -> validator.validate(MarketField.SENTIMENT, CompareOperator.EQ, "BULLISH"))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(MarketField.SENTIMENT, CompareOperator.NEQ, "BEARISH"))
                .doesNotThrowAnyException();
    }

    @Test
    void sentiment_rejectsOrderingOperators() {
        assertThatThrownBy(() -> validator.validate(MarketField.SENTIMENT, CompareOperator.GT, "BULLISH"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> validator.validate(MarketField.SENTIMENT, CompareOperator.LTE, "BULLISH"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void sentiment_rejectsValuesOutsideTheAllowedSet() {
        assertThatThrownBy(() -> validator.validate(MarketField.SENTIMENT, CompareOperator.EQ, "MAYBE"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void numericField_requiresANumericValue() {
        assertThatCode(() -> validator.validate(MarketField.UP_PRICE, CompareOperator.GT, 0.5))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(MarketField.UP_PRICE, CompareOperator.GT, "not-a-number"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void numericField_rejectsAnOperatorNotInItsCatalogSet() {
        // volume_24h's catalog entry only lists LT/GT, not EQ.
        assertThatThrownBy(() -> validator.validate(MarketField.VOLUME_24H, CompareOperator.EQ, 100))
                .isInstanceOf(ResponseStatusException.class);
    }
}
