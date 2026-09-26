package com.polypilot.ohlc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Natural composite key for {@link OhlcCandle}: {@code (symbol, timeframe, open_time)}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OhlcCandleId implements Serializable {

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "timeframe", nullable = false, length = 8)
    private String timeframe;

    @Column(name = "open_time", nullable = false)
    private OffsetDateTime openTime;
}
