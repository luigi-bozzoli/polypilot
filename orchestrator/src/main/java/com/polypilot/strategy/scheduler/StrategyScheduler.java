package com.polypilot.strategy.scheduler;

import com.polypilot.entity.Strategy;
import com.polypilot.strategy.repository.StrategyRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * One reschedulable job per <em>enabled</em> strategy, keyed by strategy ID rather than a fixed
 * job-name enum like {@code MarketScheduler} — the strategy count changes at runtime as users
 * create/enable/disable/delete strategies, so jobs are registered dynamically instead of all at
 * {@code @PostConstruct}. Each strategy runs on its own {@code cron_expression} (schema/roadmap
 * §8.7.2), not a shared sweep interval — see the design doc's rejection of a full-table sweep.
 *
 * <p>{@link #register(Strategy)} / {@link #unregister(UUID)} / {@link #reschedule(Strategy)} are
 * called from {@code StrategyService}'s create/update/delete once their transaction has
 * committed. A fired tick only submits {@code strategy-eval-<id>}'s work to
 * {@link StrategyEvaluationRunner#run(UUID)} — the per-strategy ShedLock lease inside that call is
 * what actually prevents a slow run from overlapping its own next tick; this class only owns
 * *when* a tick fires, not mutual exclusion.
 */
@Slf4j
@Component
public class StrategyScheduler {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final StrategyRepository strategyRepository;
    private final StrategyEvaluationRunner strategyEvaluationRunner;
    private final Environment environment;

    private final Map<UUID, ScheduledFuture<?>> activeJobs = new ConcurrentHashMap<>();

    public StrategyScheduler(
            @Qualifier("strategyTaskScheduler") ThreadPoolTaskScheduler taskScheduler,
            StrategyRepository strategyRepository,
            StrategyEvaluationRunner strategyEvaluationRunner,
            Environment environment) {
        this.taskScheduler = taskScheduler;
        this.strategyRepository = strategyRepository;
        this.strategyEvaluationRunner = strategyEvaluationRunner;
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        // Off only when a test profile explicitly sets polypilot.scheduling.enabled=false
        // (e.g. the Testcontainers IT) — absent/true in every other environment, including prod.
        if (!environment.getProperty("polypilot.scheduling.enabled", Boolean.class, true)) {
            log.info("Scheduling disabled (polypilot.scheduling.enabled=false) — no strategies registered");
            return;
        }
        strategyRepository.findAllByEnabledTrueAndDeletedAtIsNull().forEach(this::register);
    }

    /**
     * Registers (or re-registers, cancelling any existing future for the same strategy first) a
     * cron-triggered evaluation job. Does nothing but log if the strategy's cron is somehow
     * invalid despite {@code StrategyRequestValidator} checking it at write time (e.g. a row
     * edited directly in the DB) — an unschedulable strategy must never crash the scheduler.
     */
    public void register(Strategy strategy) {
        String cronExpression = strategy.getCronExpression();
        if (!CronExpression.isValidExpression(cronExpression)) {
            log.error("Strategy [{}] has invalid cron [{}] — not scheduling", strategy.getId(), cronExpression);
            return;
        }

        UUID strategyId = strategy.getId();
        ScheduledFuture<?> future = taskScheduler.schedule(
                wrapWithLogging(strategyId, strategy.getName()), new CronTrigger(cronExpression));

        ScheduledFuture<?> previous = activeJobs.put(strategyId, future);
        if (previous != null) {
            previous.cancel(false); // let an in-flight run finish, just don't schedule its next tick
        }
        log.info("Strategy [{}] ({}) scheduled with cron [{}]", strategyId, strategy.getName(), cronExpression);
    }

    /** Cancels a strategy's scheduled job, if any. Safe to call for an unscheduled strategy. */
    public void unregister(UUID strategyId) {
        ScheduledFuture<?> future = activeJobs.remove(strategyId);
        if (future != null) {
            future.cancel(false);
            log.info("Strategy [{}] unscheduled", strategyId);
        }
    }

    /**
     * Syncs scheduling state to a strategy's current row: registers it if enabled and not
     * deleted, unregisters it otherwise. The single entry point {@code StrategyService} calls
     * after create/update, so callers never need to diff old vs. new {@code enabled}/cron state
     * themselves.
     */
    public void reschedule(Strategy strategy) {
        if (Boolean.TRUE.equals(strategy.getEnabled()) && strategy.getDeletedAt() == null) {
            register(strategy);
        } else {
            unregister(strategy.getId());
        }
    }

    private Runnable wrapWithLogging(UUID strategyId, String name) {
        return () -> {
            try {
                log.info("Starting scheduled evaluation for strategy [{}] ({})", strategyId, name);
                strategyEvaluationRunner.run(strategyId);
                log.info("Finished scheduled evaluation for strategy [{}] ({})", strategyId, name);
            } catch (Exception ex) {
                // A failed tick must never cancel this strategy's future scheduled runs.
                log.error("Scheduled evaluation failed for strategy [{}] ({})", strategyId, name, ex);
            }
        };
    }
}
