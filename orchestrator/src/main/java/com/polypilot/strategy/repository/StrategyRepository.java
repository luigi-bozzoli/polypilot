package com.polypilot.strategy.repository;

import com.polypilot.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StrategyRepository extends JpaRepository<Strategy, UUID> {

    List<Strategy> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    List<Strategy> findAllByEnabledTrueAndDeletedAtIsNull();

    Optional<Strategy> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByNameAndUserIdAndDeletedAtIsNull(String name, UUID userId);

    boolean existsByNameAndUserIdAndIdNotAndDeletedAtIsNull(String name, UUID userId, UUID id);
}
