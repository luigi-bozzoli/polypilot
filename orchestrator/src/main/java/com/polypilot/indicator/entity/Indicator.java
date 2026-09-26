package com.polypilot.indicator.entity;

import com.polypilot.indicator.enums.IndicatorCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One technical indicator in the seed-owned catalog. Read-only at runtime — rows
 * are created and edited only via {@code 002_seed_data.sql}. Column names/types
 * must match section 11 of {@code 001_schema.sql} exactly (ddl-auto=validate).
 */
@Entity
@Table(name = "indicators")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Indicator {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 40, unique = true)
    private String key;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String abbreviation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IndicatorCategory category;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    /**
     * Ordered configurable parameters. A {@link Set} (not a {@code List}) so the
     * repository's {@code @EntityGraph} can fetch {@code parameters} and
     * {@code outputs} in one query without a {@code MultipleBagFetchException};
     * {@code @OrderBy} still preserves {@code display_order}.
     */
    @OneToMany(mappedBy = "indicator", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private Set<IndicatorParameter> parameters = new LinkedHashSet<>();

    @OneToMany(mappedBy = "indicator", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private Set<IndicatorOutput> outputs = new LinkedHashSet<>();
}
