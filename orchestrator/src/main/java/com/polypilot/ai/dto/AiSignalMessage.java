package com.polypilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * One message on the {@code ai.signals} queue — carries both the news summary
 * and the sentiment score from a single ai-agent run, so
 * {@code AiSignalListener} writes both rows from one message. Field names are
 * snake_case to match ai-agent's pydantic {@code AiSignalMessage} (published
 * via {@code model_dump()}, no alias generator).
 */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class AiSignalMessage {
    @JsonProperty("market_id")
    String marketId;

    String summary;

    List<NewsArticle> articles;

    /** BULLISH / BEARISH / NEUTRAL — matches {@code SentimentType}. */
    String sentiment;

    Double confidence;

    String reasoning;

    @JsonProperty("article_count")
    Integer articleCount;

    @JsonProperty("model_used")
    String modelUsed;

    /** Verbatim structured-output response — never discard, see schema comment. */
    @JsonProperty("raw_response")
    Map<String, Object> rawResponse;

    @JsonProperty("generated_at")
    String generatedAt;
}
