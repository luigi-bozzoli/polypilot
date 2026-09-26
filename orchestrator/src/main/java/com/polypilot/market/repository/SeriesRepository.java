package com.polypilot.market.repository;

import com.polypilot.market.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeriesRepository extends JpaRepository<Series, UUID> {
    Optional<Series> getByPolymarketId(String polymarketId);

    @Query("SELECT s.polymarketId FROM Series s WHERE s.tracked = true")
    List<String> findTrackedPolymarketIds();

    @Query(value = """
            SELECT s.*
            FROM series s
            JOIN series_users su ON su.series_id = s.id
            WHERE su.user_id = :userId
            """, nativeQuery = true)
    List<Series> findAllByUserId(@Param("userId") UUID userId);

}
