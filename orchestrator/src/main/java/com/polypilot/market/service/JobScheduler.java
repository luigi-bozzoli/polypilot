package com.polypilot.market.service;
import com.polypilot.ai.service.NewsSyncService;
import com.polypilot.market.entity.JobSchedule;
import com.polypilot.market.repository.JobScheduleRepository;
import com.polypilot.ohlc.service.OhlcSyncService;
import com.polypilot.trading.scheduler.PositionCloseRunner;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Holds one reschedulable job per task name. Replaces static @Scheduled
 * annotations so cron expressions can be changed at runtime without a
 * restart. Cron values default on startup but can be overridden live via
 * ScheduleController.
 */
@Slf4j
@Component
@AllArgsConstructor
public class JobScheduler {

    private static final String SERIES_SYNC_JOB = "series-sync";
    private static final String OPEN_MARKET_JOB = "open-market-sync";
    private static final String OHLC_SYNC_JOB = "ohlc-sync";
    private static final String NEWS_SYNC_JOB = "news-sync";
    private static final String POSITION_CLOSE_SWEEP = "position-close-sweep";

    private final ThreadPoolTaskScheduler taskScheduler;
    private final MarketSyncService marketSyncService;
    private final OhlcSyncService ohlcSyncService;
    private final NewsSyncService newsSyncService;
    private final JobScheduleRepository jobScheduleRepository;
    private final PositionCloseRunner positionCloseRunner;
    private final Environment environment;

    private final Map<String, ScheduledFuture<?>> activeJobs = new ConcurrentHashMap<>();

    private static final String DEFAULT_CRON = "0 */5 * * * *"; // every 5 minutes

    @PostConstruct
    public void init() {
        // Off only when a test profile explicitly sets polypilot.scheduling.enabled=false
        // (e.g. the Testcontainers IT) — absent/true in every other environment, including prod.
        if (!environment.getProperty("polypilot.scheduling.enabled", Boolean.class, true)) {
            log.info("Scheduling disabled (polypilot.scheduling.enabled=false) — no jobs registered");
            return;
        }
        initJob(SERIES_SYNC_JOB, marketSyncService::syncAllSeries);
        initJob(OPEN_MARKET_JOB, marketSyncService::updateOpenMarkets);
        initJob(OHLC_SYNC_JOB, ohlcSyncService::syncAll);
        initJob(NEWS_SYNC_JOB, newsSyncService::syncAll);
        initJob(POSITION_CLOSE_SWEEP, positionCloseRunner::run);
    }

    private void initJob(String jobName, Runnable task) {
        String cron = jobScheduleRepository.findById(jobName)
                .map(JobSchedule::getCronExpression)
                .filter(CronExpression::isValidExpression)
                .orElseGet(() -> {
                    log.warn("No valid cron found in DB for job [{}], falling back to default [{}]",
                            jobName, DEFAULT_CRON);
                    return DEFAULT_CRON;
                });

        schedule(jobName, cron, task);
    }


    /**
     * (Re)schedules a named job with a new cron expression. Cancels any
     * previously running schedule for that job name first.
     */
    public void schedule(String jobName, String cronExpression, Runnable task) {
        ScheduledFuture<?> existing = activeJobs.get(jobName);
        if (existing != null) {
            existing.cancel(false); // let an in-flight run finish, just don't schedule the next one
        }

        ScheduledFuture<?> future = taskScheduler.schedule(
                wrapWithLogging(jobName, task),
                new CronTrigger(cronExpression)
        );

        activeJobs.put(jobName, future);
        log.info("Job [{}] (re)scheduled with cron [{}]", jobName, cronExpression);
    }

    /** True if at least one sync job is currently scheduled (not cancelled). */
    public boolean isRunning() {
        return activeJobs.values().stream().anyMatch(future -> !future.isCancelled());
    }

    public void rescheduleSeriesSync(String cronExpression) {
        schedule(SERIES_SYNC_JOB, cronExpression, marketSyncService::syncAllSeries);
    }

    public void rescheduleOpenMarketSync(String cronExpression) {
        schedule(OPEN_MARKET_JOB, cronExpression, marketSyncService::updateOpenMarkets);
    }

    public void rescheduleOhlcSync(String cronExpression) {
        schedule(OHLC_SYNC_JOB, cronExpression, ohlcSyncService::syncAll);
    }

    public void rescheduleNewsSync(String cronExpression) {
        schedule(NEWS_SYNC_JOB, cronExpression, newsSyncService::syncAll);
    }

    public void reschedulePositionCloseSweep(String cronExpression) {
        schedule(POSITION_CLOSE_SWEEP, cronExpression, positionCloseRunner::run);
    }

    private Runnable wrapWithLogging(String jobName, Runnable task) {
        return () -> {
            try {
                log.info("Starting job [{}]", jobName);
                task.run();
                log.info("Finished job [{}]", jobName);
            } catch (Exception ex) {
                log.error("Job [{}] failed", jobName, ex);
            }
        };
    }
}