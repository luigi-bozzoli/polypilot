package com.polypilot.strategy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated bounded pool for dispatching {@code IndicatorCalculationService.calculate}
 * calls during strategy evaluation — not {@code ForkJoinPool.commonPool()}, since
 * these tasks block on JPA/DB I/O and must not exhaust the Hikari connection pool.
 * See {@link StrategyExecutorProperties} for sizing.
 */
@Configuration
@RequiredArgsConstructor
public class StrategyExecutorConfig {

    private final StrategyExecutorProperties properties;

    @Bean
    public ThreadPoolTaskExecutor indicatorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = properties.getIndicatorExecutorPoolSize();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("strategy-indicator-");
        executor.initialize();
        return executor;
    }
}
