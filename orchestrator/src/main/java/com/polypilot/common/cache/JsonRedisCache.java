package com.polypilot.common.cache;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Thin best-effort read-through JSON cache over Redis, shared by services that
 * cache DB-backed reference/lookup data ({@code IndicatorCatalogService},
 * {@code ReferenceDataService}). Callers own their own key prefixes and TTLs;
 * this class only does the GET/SET/serialize/deserialize mechanics.
 *
 * <p>Redis is always best-effort: any {@link DataAccessException} is logged at
 * WARN and treated as a miss (for {@link #get}) or swallowed (for
 * {@link #put}), so a Redis outage never breaks the caller — it just serves
 * from the DB every time. A payload that no longer deserializes into the
 * requested type is logged and deleted rather than returned.
 */
@Slf4j
@Component
@AllArgsConstructor
public class JsonRedisCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * Returns {@code null} (⇒ caller loads from source) on a cache miss, on any
     * Redis failure, or on a payload that no longer deserializes into
     * {@code type}; a stale/corrupt payload is also deleted.
     */
    public <T> T get(String key, Class<T> type) {
        String json;
        try {
            json = redis.opsForValue().get(key);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable reading cache entry [{}] — falling back to DB", key, ex);
            return null;
        }
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException ex) {
            log.warn("Discarding unreadable cache entry [{}]", key, ex);
            try {
                redis.delete(key);
            } catch (DataAccessException ignored) {
                // best effort — it will age out via TTL
            }
            return null;
        }
    }

    /** Best-effort write. A Redis failure is logged and swallowed. */
    public void put(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable caching entry [{}] — served from DB only", key, ex);
        }
    }
}
