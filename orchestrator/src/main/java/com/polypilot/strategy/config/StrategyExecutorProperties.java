package com.polypilot.strategy.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Config for the bounded pool that runs indicator calculations during
 * strategy evaluation (see {@code StrategyEvaluationService}). Each task
 * blocks on JPA/DB I/O ({@code IndicatorCalculationService.calculate}), so
 * this pool must be sized to stay well under the Hikari connection pool
 * (Spring Boot's default of 10, since no {@code datasource.hikari.*} override
 * exists yet) rather than left to {@code ForkJoinPool.commonPool()}.
 *
 * <p>Picked up automatically by {@code @ConfigurationPropertiesScan} on
 * {@code PolyPilotApplication}.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "polypilot.strategy")
public class StrategyExecutorProperties {

    /** Core and max size of the indicator-calculation executor. */
    @Min(1)
    private int indicatorExecutorPoolSize = 4;

    /**
     * Pool size of the scheduler that fires each enabled strategy's own cron trigger (see
     * {@code StrategyScheduler}) — separate from {@code market-sync-}'s pool since strategy count
     * grows with users while the market-sync job count is fixed. A tick runs
     * {@code StrategyEvaluationRunner.run} inline (ShedLock's {@code executeWithLock} is
     * synchronous), so the calling thread is blocked for the whole evaluation — this pool size is
     * how many strategies can genuinely evaluate at once when their cron ticks coincide; extra
     * concurrent ticks queue rather than being dropped.
     */
    @Min(1)
    private int schedulerPoolSize = 5;
}
