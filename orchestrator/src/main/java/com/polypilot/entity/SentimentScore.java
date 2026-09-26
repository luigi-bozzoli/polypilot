package com.polypilot.entity;

import com.polypilot.enums.SentimentType;
import com.polypilot.market.entity.Market;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sentiment_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentimentScore {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SentimentType sentiment;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "model_used", nullable = false, length = 100)
    private String modelUsed;

    @Column(name = "article_count", nullable = false)
    @Builder.Default
    private Integer articleCount = 0;

    /** Verbatim LLM structured-output response, never discard — invaluable for prompt tuning. */
    @Column(name = "raw_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawResponse;

    @Column(name = "scored_at", nullable = false)
    private OffsetDateTime scoredAt;
}
