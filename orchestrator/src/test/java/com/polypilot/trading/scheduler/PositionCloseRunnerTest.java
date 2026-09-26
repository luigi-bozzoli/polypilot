package com.polypilot.trading.scheduler;

import com.polypilot.trading.PositionCloseService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Exercises {@link PositionCloseRunner#run} — the fixed lock-name wiring and delegation to
 * {@link PositionCloseService#closeEligiblePositions()}.
 */
@ExtendWith(MockitoExtension.class)
class PositionCloseRunnerTest {

    @Mock
    private LockingTaskExecutor lockingTaskExecutor;
    @Mock
    private PositionCloseService positionCloseService;

    private PositionCloseRunner runner;

    @BeforeEach
    void setUp() {
        runner = new PositionCloseRunner(lockingTaskExecutor, positionCloseService);
    }

    @Test
    void runUsesAFixedLockNameAndDelegatesToTheLockingExecutor() {
        runner.run();

        ArgumentCaptor<LockConfiguration> captor = ArgumentCaptor.forClass(LockConfiguration.class);
        verify(lockingTaskExecutor).executeWithLock(any(Runnable.class), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("position-close-sweep");
    }
}
