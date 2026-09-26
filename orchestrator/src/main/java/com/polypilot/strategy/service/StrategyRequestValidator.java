package com.polypilot.strategy.service;

import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.strategy.StrategyNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
public class StrategyRequestValidator {

    private final RuleTreeValidator ruleTreeValidator;

    public void validate(
            MarketOutcome tokenSide,
            BigDecimal maxBetSize,
            BigDecimal maxDailyExposure,
            String cronExpression,
            StrategyNode ruleTree
    ) {
        if (tokenSide != MarketOutcome.YES && tokenSide != MarketOutcome.NO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "tokenSide must be YES or NO, got: " + tokenSide);
        }

        if (maxDailyExposure.compareTo(maxBetSize) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "maxDailyExposure must be greater than or equal to maxBetSize");
        }

        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Invalid cron expression: " + cronExpression);
        }

        ruleTreeValidator.validate(ruleTree);
    }
}
