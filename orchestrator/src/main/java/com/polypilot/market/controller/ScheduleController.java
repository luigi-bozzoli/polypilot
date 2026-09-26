package com.polypilot.market.controller;

import com.polypilot.market.service.JobScheduler;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/schedule")
@AllArgsConstructor
public class ScheduleController {

    private final JobScheduler jobScheduler;

    @PostMapping("/config")
    public ResponseEntity<?> updateSchedule(@RequestBody ScheduleConfigRequest request) {
        if (request.getCron() == null || !CronExpression.isValidExpression(request.getCron())) {
            return ResponseEntity.badRequest().body("Invalid cron expression: " + request.getCron());
        }

        switch (request.getJob()) {
            case "series-sync" -> jobScheduler.rescheduleSeriesSync(request.getCron());
            case "open-market-sync" -> jobScheduler.rescheduleOpenMarketSync(request.getCron());
            case "ohlc-sync" -> jobScheduler.rescheduleOhlcSync(request.getCron());
            case "news-sync" -> jobScheduler.rescheduleNewsSync(request.getCron());
            case "position-close-sweep" -> jobScheduler.reschedulePositionCloseSweep(request.getCron());
            default -> {
                return ResponseEntity.badRequest().body("Unknown job: " + request.getJob());
            }
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/config")
    public Map<String, String> getCurrentSchedules() {
        return null;
    }
}
