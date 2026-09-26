package com.polypilot.indicator.entity;

import com.polypilot.indicator.enums.ParameterDataType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One configurable parameter of an indicator — one row per (indicator, key).
 * When {@link #universal} is set the parameter inherits its allowed-value list
 * and semantics from that {@link UniversalParameter}; {@link #constraints} is
 * then ignored and only {@link #defaultValue} / {@link #required} are local.
 * Column names/types must match section 11 of {@code 001_schema.sql} exactly.
 */
@Entity
@Table(name = "indicator_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorParameter {

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
    @Column(name = "data_type", nullable = false, length = 20)
    private ParameterDataType dataType;

    /** Non-null only for {@code source} / {@code timeframe}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "universal_key")
    private UniversalParameter universal;

    /**
     * Shape depends on {@link #dataType}: {@code {min,max,step}} for INTEGER,
     * {@code {min,max}} for NUMBER, {@code {values:[…]}} for a local ENUM. Empty
     * (and ignored) when {@link #universal} is set.
     */
    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> constraints = new LinkedHashMap<>();

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
