package com.polypilot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "news_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsSummary {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "articles", nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private List<Map<String, Object>> articles;

    @Column(name = "model_used", nullable = false, length = 100)
    private String modelUsed;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}