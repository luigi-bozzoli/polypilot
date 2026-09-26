package com.polypilot.market.dto;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private String id;

    private String label;

    private String parentCategory;

    private String slug;

    private String publishedAt;

    private String createdBy;

    private String updatedBy;

    private Instant createdAt;

    private Instant updatedAt;
}