package com.polypilot.config;

import com.polypilot.strategy.config.StrategyExecutorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@RequiredArgsConstructor
public class SchedulingConfig {

    private final StrategyExecutorProperties strategyExecutorProperties;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // series-sync, open-market-sync, ohlc-sync — one thread each so a long
        // run of one job can't delay the others.
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("market-sync-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Separate pool from {@link #taskScheduler()} — strategy count grows with users, unlike the
     * fixed market-sync job count, so it gets its own sizing knob
     * ({@link StrategyExecutorProperties#getSchedulerPoolSize()}) instead of competing with those
     * three jobs for threads. See {@code StrategyScheduler}.
     */
    @Bean
    public ThreadPoolTaskScheduler strategyTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(strategyExecutorProperties.getSchedulerPoolSize());
        scheduler.setThreadNamePrefix("strategy-sched-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Separate pool from both {@link #taskScheduler()} and {@link #strategyTaskScheduler()} — a
     * single fixed job (the position-close sweep), unrelated in cadence and failure profile to
     * either the market-sync jobs or per-strategy evaluation, so it gets its own thread rather
     * than competing with them. See {@code PositionCloseScheduler}.
     */
    @Bean
    public ThreadPoolTaskScheduler positionCloseTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("position-close-");
        scheduler.initialize();
        return scheduler;
    }
}