package com.polypilot.indicator.entity;

import com.polypilot.indicator.enums.ParameterDataType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A shared enum vocabulary factored out of {@code indicator_parameters}. Only two
 * rows ever exist: {@code source} and {@code timeframe} — the only parameters
 * with an identical description <em>and</em> identical valid-value list across
 * every indicator they apply to. An {@link IndicatorParameter} points here via
 * {@code universal_key} and inherits {@link #allowedValues}.
 */
@Entity
@Table(name = "universal_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversalParameter {

    @Id
    @Column(length = 40)
    private String key;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Always {@link ParameterDataType#ENUM} — the CHECK constraint enforces it. */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private ParameterDataType dataType;

    @Column(name = "allowed_values", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<String> allowedValues = new ArrayList<>();

    /** {@code 'close'} for source; {@code null} for timeframe. */
    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
