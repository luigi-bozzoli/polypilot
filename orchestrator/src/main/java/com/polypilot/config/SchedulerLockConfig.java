package com.polypilot.config;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Wires the {@code shedlock} table (see {@code 001_schema.sql}) as the lock backend for
 * {@link com.polypilot.strategy.scheduler.StrategyEvaluationRunner}. Deliberately exposes
 * {@link LockingTaskExecutor} (the programmatic API), not the {@code @SchedulerLock} annotation:
 * a lock name has to be built per strategy ID at call time, and ShedLock only documents that as
 * supported through {@code LockingTaskExecutor.executeWithLock(...)} — {@code @SchedulerLock}'s
 * SpEL {@code name} is documented for {@code @Scheduled} methods with a fixed identity, not
 * per-invocation dynamic names.
 */
@Configuration
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(LockProvider lockProvider) {
        return new DefaultLockingTaskExecutor(lockProvider);
    }
}
