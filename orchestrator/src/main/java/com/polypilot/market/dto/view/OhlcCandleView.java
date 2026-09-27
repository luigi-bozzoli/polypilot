package com.polypilot.market.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * One Binance candle, emitted oldest-first by {@code GET /market/{marketId}/ohlc}.
 *
 * <p>Mirrors {@link com.polypilot.ohlc.entity.OhlcCandle} field-for-field; {@code closed}
 * distinguishes the still-forming rightmost bar (if included) from settled history so the
 * chart can style it differently without inferring "last in the array" itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OhlcCandleView {

    private OffsetDateTime openTime;

    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;

    private boolean closed;
}
