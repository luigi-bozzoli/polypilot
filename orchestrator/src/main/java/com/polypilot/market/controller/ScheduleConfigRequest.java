package com.polypilot.market.controller;

import lombok.Value;

/**
 * Request body for {@code POST /schedule/config}: the job to reschedule and the
 * new cron expression. Deserialized by Jackson via the all-args constructor.
 */
@Value
public class ScheduleConfigRequest {
    String job;
    String cron;
}
