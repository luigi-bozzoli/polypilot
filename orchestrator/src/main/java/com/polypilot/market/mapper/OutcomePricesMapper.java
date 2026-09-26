package com.polypilot.market.mapper;

import com.polypilot.market.enums.MarketOutcome;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OutcomePricesMapper {

    private final ObjectMapper objectMapper;

    public OutcomePricesMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Named("upPrice")
    public BigDecimal getUpPrice(String outcomePrices) {
        return getPrice(outcomePrices, 0);
    }

    @Named("downPrice")
    public BigDecimal getDownPrice(String outcomePrices) {
        return getPrice(outcomePrices, 1);
    }

    @Named("marketOutcome")
    public MarketOutcome getMarketOutcome(String outcomePrices) {
        BigDecimal upPrice = getUpPrice(outcomePrices);
        BigDecimal downPrice = getDownPrice(outcomePrices);

        // Prices are absent until a market trades, and both sit near 0.5 while it
        // is open — an outcome is only decided once one side settles to exactly 1.
        if (upPrice != null && BigDecimal.ONE.compareTo(upPrice) == 0) {
            return MarketOutcome.UP;
        }

        if (downPrice != null && BigDecimal.ONE.compareTo(downPrice) == 0) {
            return MarketOutcome.DOWN;
        }

        return null;
    }

    private BigDecimal getPrice(String outcomePrices, int index) {
        if (outcomePrices == null || outcomePrices.isBlank()) {
            return null;
        }

        try {
            List<String> prices = objectMapper.readValue(
                    outcomePrices,
                    new TypeReference<List<String>>() {
                    }
            );

            if (prices.size() <= index || prices.get(index) == null) {
                return null;
            }

            return new BigDecimal(prices.get(index).trim());
        } catch (NumberFormatException | JacksonException e) {
            // Malformed payload (not a JSON array, non-numeric entry, ""): treat
            // as "unknown" rather than letting it abort the whole market sync.
            return null;
        }
    }
}
