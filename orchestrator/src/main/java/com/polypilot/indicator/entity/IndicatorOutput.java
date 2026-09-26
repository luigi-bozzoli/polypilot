package com.polypilot.indicator.entity;

import com.polypilot.indicator.enums.ValueScale;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * One value an indicator produces. Single-output indicators expose {@code value};
 * MACD exposes {@code macd} / {@code signal} / {@code histogram}. Column
 * names/types must match section 11 of {@code 001_schema.sql} exactly.
 */
@Entity
@Table(name = "indicator_outputs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorOutput {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "indicator_id", nullable = false)
    private Indicator indicator;

    @Column(nullable = false, length = 40)
    private String key;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_scale", nullable = false, length = 20)
    private ValueScale valueScale;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
