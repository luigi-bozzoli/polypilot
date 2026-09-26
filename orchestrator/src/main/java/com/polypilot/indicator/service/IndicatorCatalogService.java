package com.polypilot.indicator.service;

import com.polypilot.common.cache.JsonRedisCache;
import com.polypilot.indicator.dto.view.IndicatorCatalogView;
import com.polypilot.indicator.dto.view.IndicatorView;
import com.polypilot.indicator.entity.Indicator;
import com.polypilot.indicator.mapper.IndicatorViewMapper;
import com.polypilot.indicator.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * Read-through access to the technical-indicator catalog, backed by Redis.
 *
 * <p>The catalog is seed-owned and static for the lifetime of the process, so
 * each view is serialized to JSON and held in Redis (via the shared
 * {@link JsonRedisCache}) under an {@code indicator-catalog:v1:*} key with a
 * fixed TTL ({@code indicator.catalog.cache-ttl}, default 1h). Nothing
 * invalidates the entries at runtime — a reseed takes effect on the next
 * orchestrator boot, once the old entries age out.
 *
 * <p>The {@code v1} key segment is a manual schema version for the view DTOs —
 * bump it when their shape changes so stale JSON from an older deploy cannot be
 * read back into the new shape.
 */
@Service
@RequiredArgsConstructor
public class IndicatorCatalogService {

    static final String CACHE_KEY_ALL = "indicator-catalog:v1:all";
    static final String CACHE_KEY_ONE_PREFIX = "indicator-catalog:v1:key:";

    private final IndicatorRepository indicatorRepository;
    private final IndicatorViewMapper indicatorViewMapper;
    private final JsonRedisCache cache;
    @Value("${indicator.catalog.cache-ttl:1h}")
    private  Duration cacheTtl;

    @Transactional(readOnly = true)
    public IndicatorCatalogView getCatalog() {
        IndicatorCatalogView cached = cache.get(CACHE_KEY_ALL, IndicatorCatalogView.class);
        if (cached != null) {
            return cached;
        }
        IndicatorCatalogView view = IndicatorCatalogView.builder()
                .indicators(indicatorViewMapper.toViews(
                        indicatorRepository.findAllByEnabledTrueOrderByDisplayOrder()))
                .build();
        cache.put(CACHE_KEY_ALL, view, cacheTtl);
        return view;
    }

    @Transactional(readOnly = true)
    public IndicatorView getIndicator(String key) {
        String cacheKey = CACHE_KEY_ONE_PREFIX + key;

        IndicatorView cached = cache.get(cacheKey, IndicatorView.class);
        if (cached != null) {
            return cached;
        }

        Indicator indicator = indicatorRepository.findByKey(key)
                .filter(Indicator::getEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown indicator: " + key));

        IndicatorView view = indicatorViewMapper.toView(indicator);
        cache.put(cacheKey, view, cacheTtl);
        return view;
    }
}
