package com.polypilot.indicator.repository;

import com.polypilot.indicator.entity.Indicator;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only access to the seed-owned indicator catalog. Both finders pull
 * {@code parameters} (with their {@code universal} link) and {@code outputs} in a
 * single query via {@code @EntityGraph} to avoid an N+1 over the catalog.
 */
public interface IndicatorRepository extends JpaRepository<Indicator, UUID> {

    @EntityGraph(attributePaths = {"parameters", "parameters.universal", "outputs"})
    List<Indicator> findAllByEnabledTrueOrderByDisplayOrder();

    @EntityGraph(attributePaths = {"parameters", "parameters.universal", "outputs"})
    Optional<Indicator> findByKey(String key);
}
