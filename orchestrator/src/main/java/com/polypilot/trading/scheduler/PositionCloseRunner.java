package com.polypilot.trading.scheduler;

import com.polypilot.trading.PositionCloseService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Fires the position-close sweep. Owns none of the scheduling itself (that's
 * {@link PositionCloseScheduler}) — only leases a single, fixed-name ShedLock and delegates to
 * {@link PositionCloseService#closeEligiblePositions()}.
 *
 * <p>Unlike {@code StrategyEvaluationRunner} (one dynamic lock name per strategy ID, hence the
 * programmatic {@code LockingTaskExecutor} API rather than {@code @SchedulerLock}), this job has a
 * single fixed identity — it scans every open position in one run. The programmatic API is kept
 * anyway for consistency with the only other ShedLock usage in this codebase, rather than mixing
 * in {@code @SchedulerLock} as a second locking style for what is, mechanically, the same kind of
 * guard (don't let a slow run overlap the next scheduled tick).
 *
 * <p>{@code lockAtMostFor} is longer than {@code StrategyEvaluationRunner}'s (5 min vs. 2 min):
 * this sweep scans every open position across every user in one run, not a single strategy's
 * evaluation, so it can legitimately take longer. The lease still expires on its own if the
 * instance holding it crashes mid-run — the same self-healing property that keeps this safe if the
 * orchestrator ever runs as more than one instance.
 */
@Component
@RequiredArgsConstructor
public class PositionCloseRunner {

    static final String LOCK_NAME = "position-close-sweep";

    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(10);
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(5);

    private final LockingTaskExecutor lockingTaskExecutor;
    private final PositionCloseService positionCloseService;

    public void run() {
        LockConfiguration lockConfiguration = new LockConfiguration(
                Instant.now(), LOCK_NAME, LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR);

        lockingTaskExecutor.executeWithLock(
                (Runnable) positionCloseService::closeEligiblePositions, lockConfiguration);
    }
}
