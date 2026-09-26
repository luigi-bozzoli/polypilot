package com.polypilot.trading;

import com.polypilot.entity.Order;
import com.polypilot.entity.Position;
import com.polypilot.enums.alert.ActionType;
import lombok.Value;

/**
 * Outcome of {@link OpenTradeService#openTrade}, consumed by the caller to write the
 * corresponding {@code audit_logs} row. {@code order} and {@code position} are {@code null} when
 * {@code auditAction == ActionType.ORDER_SKIPPED} — no rows were written for that attempt.
 */
@Value
public class OpenTradeResult {
    Order order;
    Position position;
    ActionType auditAction;
    String reasoning;

    public static OpenTradeResult skipped(String reasoning) {
        return new OpenTradeResult(null, null, ActionType.ORDER_SKIPPED, reasoning);
    }

    public static OpenTradeResult opened(Order order, Position position, String reasoning) {
        return new OpenTradeResult(order, position, ActionType.DRY_RUN_ORDER, reasoning);
    }
}
