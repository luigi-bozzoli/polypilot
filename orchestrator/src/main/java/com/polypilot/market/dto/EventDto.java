package com.polypilot.market.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {

    private String id;

    private String ticker;

    private String slug;

    private String title;

    private String subtitle;

    private String description;

    private String resolutionSource;

    private Instant startDate;

    private Instant endDate;

    private String image;

    private String icon;

    private Boolean active;

    private Boolean closed;

    private Boolean archived;

    private Boolean featured;

    private Boolean restricted;

    private BigDecimal liquidity;

    private BigDecimal volume;

    private BigDecimal openInterest;

    private String category;

    private String subcategory;

    private Boolean isTemplate;

    private String templateVariables;

    private Instant createdAt;

    private Instant updatedAt;

    private Boolean commentsEnabled;


    private ImageOptimizedDto imageOptimized;

    private ImageOptimizedDto iconOptimized;

    private ImageOptimizedDto featuredImageOptimized;


    private List<String> subEvents;

    private List<CategoryDto> categories;

    private List<TagDto> tags;
}