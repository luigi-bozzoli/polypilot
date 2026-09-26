package com.polypilot.market.dto;


import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDto {

    private String id;

    private String label;

    private String slug;

    private Boolean forceShow;

    private Boolean forceHide;

    private Boolean isCarousel;

    private String publishedAt;

    private Integer createdBy;

    private Integer updatedBy;

    private Instant createdAt;

    private Instant updatedAt;
}