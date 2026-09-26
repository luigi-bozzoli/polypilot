package com.polypilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * One article, as carried inside {@link AiSignalMessage#getArticles()} and
 * stored verbatim (field-renamed) into {@code news_summaries.articles} JSONB.
 * Mirrors ai-agent's pydantic {@code Article}.
 */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class NewsArticle {
    String title;
    String url;
    String source;

    @JsonProperty("published_at")
    String publishedAt;
}
