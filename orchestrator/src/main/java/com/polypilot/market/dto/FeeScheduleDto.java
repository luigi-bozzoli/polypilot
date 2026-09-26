package com.polypilot.market.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeScheduleDto {

    private BigDecimal exponent;

    private BigDecimal rate;

    private Boolean takerOnly;

    private BigDecimal rebateRate;
}