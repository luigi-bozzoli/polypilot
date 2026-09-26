package com.polypilot.market.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageOptimizedDto {

    private String id;

    private String imageUrlSource;

    private String imageUrlOptimized;

    private Integer imageSizeKbSource;

    private Integer imageSizeKbOptimized;

    private Boolean imageOptimizedComplete;

    private String imageOptimizedLastUpdated;

    private Integer relID;

    private String field;

    private String relname;
}