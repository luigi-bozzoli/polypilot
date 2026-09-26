package com.polypilot.strategy.scheduler;

import com.polypilot.entity.Strategy;
import com.polypilot.market.entity.Series;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.strategy.repository.StrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrategySchedulerTest {

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;
    @Mock
    private StrategyRepository strategyRepository;
    @Mock
    private StrategyEvaluationRunner strategyEvaluationRunner;
    @Mock
    private Environment environment;

    private StrategyScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new StrategyScheduler(taskScheduler, strategyRepository, strategyEvaluationRunner, environment);
    }

    @Test
    void register_schedulesWithTheStrategysOwnCron() {
        Strategy strategy = strategy(UUID.randomUUID(), s -> s.cronExpression("0 */5 * * * *"));
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenAnswer(inv -> future);

        scheduler.register(strategy);

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(taskScheduler).schedule(any(Runnable.class), triggerCaptor.capture());
        assertThat(triggerCaptor.getValue()).isInstanceOf(CronTrigger.class);
    }

    @Test
    void register_withInvalidCron_neverSchedulesAndLogsInstead() {
        Strategy strategy = strategy(UUID.randomUUID(), s -> s.cronExpression("not a cron"));

        scheduler.register(strategy);

        verifyNoInteractions(taskScheduler);
    }

    @Test
    void register_calledTwiceForTheSameStrategy_cancelsThePreviousFuture() {
        Strategy strategy = strategy(UUID.randomUUID(), s -> {});
        ScheduledFuture<?> first = mock(ScheduledFuture.class);
        ScheduledFuture<?> second = mock(ScheduledFuture.class);
        List<ScheduledFuture<?>> futures = List.of(first, second);
        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger();
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class)))
                .thenAnswer(inv -> futures.get(callCount.getAndIncrement()));

        scheduler.register(strategy);
        scheduler.register(strategy);

        verify(first).cancel(false);
        verify(second, never()).cancel(false);
    }

    @Test
    void unregister_cancelsAnExistingFuture() {
        Strategy strategy = strategy(UUID.randomUUID(), s -> {});
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenAnswer(inv -> future);
        scheduler.register(strategy);

        scheduler.unregister(strategy.getId());

        verify(future).cancel(false);
    }

    @Test
    void unregister_isANoOpForAnUnscheduledStrategy() {
        scheduler.unregister(UUID.randomUUID());

        verifyNoInteractions(taskScheduler);
    }

    @Test
    void reschedule_registersWhenEnabledAndNotDeleted() {
        Strategy strategy = strategy(UUID.randomUUID(), s -> s.enabled(true).deletedAt(null));
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class)))
                .thenAnswer(inv -> mock(ScheduledFuture.class));

        scheduler.reschedule(strategy);

        verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void reschedule_unregistersWhenDisabled() {
        Strategy strategy = strategy(UUID.randomUUID(), s -> s.enabled(false));
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenAnswer(inv -> future);
        Strategy enabledStrategy = strategy(strategy.getId(), s -> s.enabled(true));
        scheduler.register(enabledStrategy); // simulate it was previously scheduled

        scheduler.reschedule(strategy);

        verify(future).cancel(false);
    }

    @Test
    void reschedule_unregistersWhenSoftDeletedEvenIfStillFlaggedEnabled() {
        Strategy strategy = strategy(UUID.randomUUID(), s -> s.enabled(true).deletedAt(OffsetDateTime.now()));
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenAnswer(inv -> future);
        scheduler.register(strategy(strategy.getId(), s -> s.enabled(true)));

        scheduler.reschedule(strategy);

        verify(future).cancel(false);
    }

    @Test
    void init_registersEveryEnabledNonDeletedStrategyFromTheRepository() {
        Strategy a = strategy(UUID.randomUUID(), s -> {});
        Strategy b = strategy(UUID.randomUUID(), s -> {});
        when(strategyRepository.findAllByEnabledTrueAndDeletedAtIsNull()).thenReturn(List.of(a, b));
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class)))
                .thenAnswer(inv -> mock(ScheduledFuture.class));
        when(environment.getProperty("polypilot.scheduling.enabled", Boolean.class, true)).thenReturn(true);

        scheduler.init();

        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void scheduledTask_delegatesToTheRunnerAndSwallowsExceptions() {
        Strategy strategy = strategy(UUID.randomUUID(), s -> {});
        doThrow(new RuntimeException("boom")).when(strategyEvaluationRunner).run(strategy.getId());
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(taskScheduler.schedule(taskCaptor.capture(), any(Trigger.class)))
                .thenAnswer(inv -> mock(ScheduledFuture.class));

        scheduler.register(strategy);
        taskCaptor.getValue().run(); // simulate the cron trigger firing

        verify(strategyEvaluationRunner).run(strategy.getId());
    }

    private Strategy strategy(UUID id, java.util.function.Consumer<Strategy.StrategyBuilder> customizer) {
        Strategy.StrategyBuilder builder = Strategy.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .name("btc-dip-buy")
                .dryRun(true)
                .cronExpression("0 */5 * * * *")
                .series(Series.builder().id(UUID.randomUUID()).build())
                .tokenSide(MarketOutcome.YES)
                .maxBetSize(BigDecimal.TEN)
                .maxDailyExposure(BigDecimal.valueOf(100))
                .ruleTree("{\"type\":\"MARKET_FIELD\",\"field\":\"up_price\",\"operator\":\"GT\",\"value\":0}");
        customizer.accept(builder);
        return builder.build();
    }
}
