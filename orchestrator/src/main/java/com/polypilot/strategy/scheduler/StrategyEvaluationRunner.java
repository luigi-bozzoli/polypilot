package com.polypilot.strategy.scheduler;

import com.polypilot.strategy.service.StrategyService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Fires one strategy's evaluation tick. Called by {@code StrategyScheduler}'s per-strategy
 * trigger — this class owns none of the scheduling itself, only leasing the per-strategy
 * ShedLock and delegating the actual evaluation-and-audit work to {@link StrategyService#
 * runEvaluation(UUID)}.
 *
 * <p>{@link #run(UUID)} wraps {@code StrategyService.runEvaluation} in a leased ShedLock
 * ({@code lockAtMostFor} 2 minutes, {@code lockAtLeastFor} 10 seconds) keyed by strategy ID, so a
 * slow evaluation can never overlap the next scheduled tick of the <em>same</em> strategy —
 * different strategies never contend for this lock, each gets its own name. The lease expiring on
 * its own (rather than needing manual cleanup) is what keeps this safe if the instance holding it
 * crashes mid-evaluation; the same mechanism is what keeps it safe if the orchestrator ever runs
 * as more than one instance.
 *
 * <p>{@code StrategyService.runEvaluation} runs in its own {@code REQUIRES_NEW} transaction — a
 * real proxy hop since it's called on the injected {@code StrategyService} bean (not via
 * self-invocation), following the same per-item isolation pattern as
 * {@code MarketItemSyncService}/{@code OhlcStreamSyncService} — so one strategy's failure can
 * never roll back another's audit row. {@code strategyService} is injected {@code @Lazy} to break
 * the {@code StrategyService -> StrategyScheduler -> StrategyEvaluationRunner -> StrategyService}
 * construction cycle without pulling in new circular-reference infrastructure.
 */
@Slf4j
@Component
public class StrategyEvaluationRunner {

    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(10);
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(2);

    private final LockingTaskExecutor lockingTaskExecutor;
    private final StrategyService strategyService;

    public StrategyEvaluationRunner(
            LockingTaskExecutor lockingTaskExecutor,
            @Lazy StrategyService strategyService) {
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.strategyService = strategyService;
    }

    public void run(UUID strategyId) {
        LockConfiguration lockConfiguration = new LockConfiguration(
                Instant.now(), "strategy-eval-" + strategyId, LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR);

        lockingTaskExecutor.executeWithLock(
                (Runnable) () -> strategyService.runEvaluation(strategyId), lockConfiguration);
    }
}
