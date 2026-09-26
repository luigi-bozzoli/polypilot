package com.polypilot.strategy.scheduler;

import com.polypilot.strategy.service.StrategyService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Exercises {@link StrategyEvaluationRunner#run} — the lock-name wiring and delegation to
 * {@link StrategyService#runEvaluation(UUID)}. The evaluation-and-audit logic itself now lives on
 * {@code StrategyService} and is exercised by {@code StrategyServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class StrategyEvaluationRunnerTest {

    @Mock
    private LockingTaskExecutor lockingTaskExecutor;
    @Mock
    private StrategyService strategyService;

    private StrategyEvaluationRunner runner;

    @BeforeEach
    void setUp() {
        runner = new StrategyEvaluationRunner(lockingTaskExecutor, strategyService);
    }

    @Test
    void runBuildsAPerStrategyLockNameAndDelegatesToTheLockingExecutor() {
        UUID id = UUID.randomUUID();

        runner.run(id);

        ArgumentCaptor<LockConfiguration> captor = ArgumentCaptor.forClass(LockConfiguration.class);
        verify(lockingTaskExecutor).executeWithLock(any(Runnable.class), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("strategy-eval-" + id);
    }
}
