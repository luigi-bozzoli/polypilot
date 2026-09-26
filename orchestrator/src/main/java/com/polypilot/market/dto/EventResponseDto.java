package com.polypilot.market.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventResponseDto {
    private String id;
    private String slug;
    private List<MarketResponseDto> markets;
}
