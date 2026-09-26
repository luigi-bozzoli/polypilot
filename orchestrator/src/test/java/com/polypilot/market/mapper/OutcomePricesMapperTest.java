package com.polypilot.market.mapper;

import com.polypilot.market.enums.MarketOutcome;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OutcomePricesMapperTest {

    private final OutcomePricesMapper mapper = new OutcomePricesMapper(JsonMapper.builder().build());

    @Test
    void parsesUpAndDownPrices() {
        assertThat(mapper.getUpPrice("[\"0.505\", \"0.495\"]")).isEqualByComparingTo("0.505");
        assertThat(mapper.getDownPrice("[\"0.505\", \"0.495\"]")).isEqualByComparingTo("0.495");
    }

    @Test
    void missingOrBlankOutcomePrices_yieldNullNotException() {
        assertThat(mapper.getUpPrice(null)).isNull();
        assertThat(mapper.getUpPrice("")).isNull();
        assertThat(mapper.getUpPrice("   ")).isNull();
        assertThat(mapper.getMarketOutcome(null)).isNull();
        assertThat(mapper.getMarketOutcome("")).isNull();
    }

    @Test
    void malformedPayload_yieldsNull() {
        assertThat(mapper.getUpPrice("not-json")).isNull();
        assertThat(mapper.getUpPrice("[\"\", \"\"]")).isNull();
        assertThat(mapper.getDownPrice("[\"0.5\"]")).isNull();     // index 1 absent
        assertThat(mapper.getMarketOutcome("not-json")).isNull();
    }

    @Test
    void resolvedMarket_resolvesOutcomeFromPriceOfExactlyOne() {
        assertThat(mapper.getMarketOutcome("[\"1\", \"0\"]")).isEqualTo(MarketOutcome.UP);
        assertThat(mapper.getMarketOutcome("[\"0\", \"1\"]")).isEqualTo(MarketOutcome.DOWN);
    }

    @Test
    void openMarket_hasNoOutcome() {
        assertThat(mapper.getMarketOutcome("[\"0.505\", \"0.495\"]")).isNull();
    }

    @Test
    void toleratesWhitespaceAroundNumbers() {
        assertThat(mapper.getUpPrice("[\" 0.42 \", \"0.58\"]")).isEqualByComparingTo(new BigDecimal("0.42"));
    }
}
