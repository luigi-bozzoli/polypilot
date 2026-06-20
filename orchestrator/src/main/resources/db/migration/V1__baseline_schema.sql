-- =============================================================================
-- PolyPilot — Baseline Schema
-- Flyway migration V1
--
-- Conventions:
--   • All PKs are UUID (gen_random_uuid()) — portable, no sequence contention
--   • ENUM-like columns use VARCHAR + CHECK constraints (easy to extend via ALTER)
--   • JSONB for structured-but-flexible payloads (rule trees, signals, articles)
--   • All timestamps are TIMESTAMPTZ — always store UTC, display in local tz
--   • created_at / updated_at on every mutable entity
-- =============================================================================


-- ---------------------------------------------------------------------------
-- EXTENSIONS
-- ---------------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "btree_gin";  -- GIN indexes on JSONB + scalars


-- =============================================================================
-- 1. USERS
--    Single row for now; schema supports multi-user from day one.
--    role: 'ADMIN' only initially — 'READ_ONLY', 'ANALYST' can be added later
--    via a simple ALTER TABLE ... ADD VALUE or new CHECK value.
-- =============================================================================

CREATE TABLE users (
                       id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   VARCHAR(255) NOT NULL,
                       role            VARCHAR(50)  NOT NULL DEFAULT 'ADMIN'
                           CHECK (role IN ('ADMIN', 'READ_ONLY', 'ANALYST')),
                       enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  users              IS 'Application users. Single-user MVP; schema is multi-user ready.';
COMMENT ON COLUMN users.role         IS 'ADMIN | READ_ONLY | ANALYST';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash; never store plaintext.';


-- =============================================================================
-- 2. MARKETS
--    Local cache of Polymarket markets, synced via the Gamma API on a schedule.
--    Not a live view — prices here are stale by design; live prices come from
--    the WebSocket feed at trade time. The dashboard reads from this table.
--
--    tracked: only tracked markets are evaluated by the strategy engine.
--    This is the gate that prevents the bot from iterating over all ~10k markets.
-- =============================================================================

CREATE TABLE markets (
                         id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                         polymarket_condition_id VARCHAR(255) NOT NULL UNIQUE,  -- Polymarket hex ID, used by CLOB
                         question                TEXT         NOT NULL,
                         category                VARCHAR(100),
                         status                  VARCHAR(50)  NOT NULL DEFAULT 'OPEN'
                             CHECK (status IN ('OPEN', 'CLOSED', 'RESOLVED', 'CANCELLED')),
                         yes_price               NUMERIC(6,4) CHECK (yes_price BETWEEN 0 AND 1),
                         no_price                NUMERIC(6,4) CHECK (no_price  BETWEEN 0 AND 1),
                         volume_24h              NUMERIC(18,4),
                         liquidity               NUMERIC(18,4),
                         resolution_date         TIMESTAMPTZ,                   -- when the market settles
                         outcome                 VARCHAR(10)  CHECK (outcome IN ('YES', 'NO', NULL)),
                         tracked                 BOOLEAN      NOT NULL DEFAULT FALSE,
                         last_synced_at          TIMESTAMPTZ,
                         created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                         updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  markets                          IS 'Cached Polymarket markets. Synced via Gamma API.';
COMMENT ON COLUMN markets.polymarket_condition_id  IS 'Polymarket canonical hex ID used in CLOB order placement.';
COMMENT ON COLUMN markets.tracked                  IS 'Only tracked markets are evaluated by the strategy engine.';
COMMENT ON COLUMN markets.yes_price                IS 'Cached price — stale by design. Live price comes from WebSocket.';

CREATE INDEX idx_markets_status   ON markets (status);
CREATE INDEX idx_markets_tracked  ON markets (tracked) WHERE tracked = TRUE;


-- =============================================================================
-- 3. STRATEGIES
--    A strategy is a named, configurable unit of trading logic.
--    Each strategy has its own schedule, risk limits, and rule tree.
--
--    rule_tree (JSONB): AND/OR nested condition tree evaluated by
--    StrategyEvaluator.java. Example:
--      {
--        "operator": "AND",
--        "conditions": [
--          { "field": "yes_price", "op": "lt", "value": 0.30 },
--          { "operator": "OR",
--            "conditions": [
--              { "field": "sentiment.confidence", "op": "gt", "value": 0.8 },
--              { "field": "volume_24h",            "op": "gt", "value": 50000 }
--            ]
--          }
--        ]
--      }
--
--    dry_run: per-strategy flag. A strategy can be in dry-run while another
--    trades live — allows safe parallel testing of new strategies.
-- =============================================================================

CREATE TABLE strategies (
                            id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                            name                 VARCHAR(255) NOT NULL UNIQUE,
                            description          TEXT,
                            enabled              BOOLEAN      NOT NULL DEFAULT FALSE,
                            dry_run              BOOLEAN      NOT NULL DEFAULT TRUE,   -- safe default
                            cron_expression      VARCHAR(100) NOT NULL DEFAULT '0 */5 * * * *',  -- every 5 min
                            token_side           VARCHAR(3)   NOT NULL DEFAULT 'YES'
                                CHECK (token_side IN ('YES', 'NO')),
                            order_type           VARCHAR(10)  NOT NULL DEFAULT 'GTC'
                                CHECK (order_type IN ('GTC', 'GTD', 'FOK', 'FAK')),
                            max_bet_size         NUMERIC(18,4) NOT NULL DEFAULT 10.0,   -- USDC
                            max_daily_exposure   NUMERIC(18,4) NOT NULL DEFAULT 100.0,  -- USDC
                            stop_loss_threshold  NUMERIC(6,4)  CHECK (stop_loss_threshold BETWEEN 0 AND 1),
                            rule_tree            JSONB         NOT NULL,
                            created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                            updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  strategies                  IS 'Named trading strategy configurations.';
COMMENT ON COLUMN strategies.dry_run          IS 'TRUE = simulate only, never place real orders. Default TRUE for safety.';
COMMENT ON COLUMN strategies.rule_tree        IS 'AND/OR nested condition tree. Evaluated by StrategyEvaluator.java.';
COMMENT ON COLUMN strategies.cron_expression  IS 'Spring cron expression. Stored in DB so cadence changes need no redeploy.';
COMMENT ON COLUMN strategies.max_bet_size     IS 'Max USDC size per single order.';
COMMENT ON COLUMN strategies.max_daily_exposure IS 'RiskGuard blocks new orders once SUM(filled today) >= this value.';

CREATE INDEX idx_strategies_enabled ON strategies (enabled) WHERE enabled = TRUE;
CREATE INDEX idx_strategies_rule_tree ON strategies USING GIN (rule_tree);


-- =============================================================================
-- 4. ORDERS
--    Every order the engine considers placing — real or dry-run — gets a row.
--    is_dry_run is denormalised from strategies deliberately: a strategy can
--    toggle between modes over time, and you need to query "all real orders
--    in March" without joining back to the strategy's current state.
--
--    external_order_id: null for dry-run or pre-submission failures.
--    Non-null means the order reached Polymarket's CLOB infrastructure.
--
--    size_requested vs size_filled: GTC/FAK orders can partially fill.
--    Tracking both lets you measure slippage and fill rate over time.
-- =============================================================================

CREATE TABLE orders (
                        id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                        strategy_id       UUID         NOT NULL REFERENCES strategies (id),
                        market_id         UUID         NOT NULL REFERENCES markets (id),
                        user_id           UUID         NOT NULL REFERENCES users (id),
                        external_order_id VARCHAR(255) UNIQUE,        -- CLOB order ID; NULL if dry-run
                        token_side        VARCHAR(3)   NOT NULL CHECK (token_side IN ('YES', 'NO')),
                        order_type        VARCHAR(10)  NOT NULL CHECK (order_type IN ('GTC', 'GTD', 'FOK', 'FAK')),
                        size_requested    NUMERIC(18,4) NOT NULL,
                        size_filled       NUMERIC(18,4) NOT NULL DEFAULT 0,
                        price             NUMERIC(6,4)  NOT NULL,      -- limit price at submission
                        status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN (
                                              'PENDING', 'OPEN', 'FILLED',
                                              'PARTIALLY_FILLED', 'CANCELLED', 'FAILED'
                                )),
                        is_dry_run        BOOLEAN       NOT NULL DEFAULT TRUE,
                        failure_reason    TEXT,                         -- populated when status = 'FAILED'
                        placed_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                        filled_at         TIMESTAMPTZ,
                        created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                        updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  orders                    IS 'All orders considered by the engine — real and simulated.';
COMMENT ON COLUMN orders.external_order_id  IS 'CLOB order ID. NULL means dry-run or pre-submission failure.';
COMMENT ON COLUMN orders.is_dry_run         IS 'Denormalised from strategy.dry_run at the time of order creation.';
COMMENT ON COLUMN orders.size_filled        IS 'Cumulative fill. May be < size_requested for GTC/FAK partial fills.';

-- RiskGuard hot path: daily exposure check
CREATE INDEX idx_orders_strategy_date ON orders (strategy_id, placed_at DESC)
    WHERE is_dry_run = FALSE;

-- Dashboard: recent orders per market
CREATE INDEX idx_orders_market ON orders (market_id, placed_at DESC);

-- Polling: find all non-terminal orders to sync status from CLOB
CREATE INDEX idx_orders_open ON orders (status)
    WHERE status IN ('PENDING', 'OPEN', 'PARTIALLY_FILLED');


-- =============================================================================
-- 5. POSITIONS
--    Net holding after one or more order fills.
--    One position row per (user, market, token_side, is_dry_run) group — a
--    position is OPEN until fully sold or market resolves.
--
--    avg_entry_price: recalculated on every fill using weighted average.
--      new_avg = (old_size * old_avg + fill_size * fill_price)
--                / (old_size + fill_size)
--
--    unrealized_pnl: (current_price - avg_entry_price) * size
--      Written by the price-sync scheduler; not a computed column because
--      current_price comes from the WebSocket cache, not a join.
-- =============================================================================

CREATE TABLE positions (
                           id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                           market_id         UUID        NOT NULL REFERENCES markets (id),
                           user_id           UUID        NOT NULL REFERENCES users (id),
                           token_side        VARCHAR(3)  NOT NULL CHECK (token_side IN ('YES', 'NO')),
                           size              NUMERIC(18,4) NOT NULL DEFAULT 0,
                           avg_entry_price   NUMERIC(6,4)  NOT NULL,
                           current_price     NUMERIC(6,4),
                           unrealized_pnl    NUMERIC(18,4),
                           realized_pnl      NUMERIC(18,4) NOT NULL DEFAULT 0,
                           status            VARCHAR(20)   NOT NULL DEFAULT 'OPEN'
                               CHECK (status IN ('OPEN', 'CLOSED', 'RESOLVED')),
                           is_dry_run        BOOLEAN       NOT NULL DEFAULT TRUE,
                           opened_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                           closed_at         TIMESTAMPTZ,
                           created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                           updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    -- one live position per (user, market, side, mode)
                           CONSTRAINT uq_position_key UNIQUE (user_id, market_id, token_side, is_dry_run, status)
);

COMMENT ON TABLE  positions                IS 'Net holdings — aggregate of fills, not individual orders.';
COMMENT ON COLUMN positions.avg_entry_price IS 'Weighted average of all fills into this position.';
COMMENT ON COLUMN positions.unrealized_pnl  IS 'Refreshed by scheduler; not a DB computed column.';

CREATE INDEX idx_positions_user_open ON positions (user_id, status)
    WHERE status = 'OPEN';
CREATE INDEX idx_positions_market ON positions (market_id);


-- =============================================================================
-- ORDER → POSITION link
-- Each fill links to the position it contributed to.
-- Added as an ALTER so the column references positions.id cleanly
-- after both tables exist.
-- =============================================================================

ALTER TABLE orders ADD COLUMN position_id UUID REFERENCES positions (id);
COMMENT ON COLUMN orders.position_id IS 'Set when the order fills and contributes to a position.';


-- =============================================================================
-- 6. SENTIMENT_SCORES
--    Full time-series: one row per LangGraph analysis run per market.
--    The dashboard reads the latest row; the history enables drift analysis
--    and backtesting signal quality.
--
--    article_count: a confidence of 0.9 from 1 article vs. 20 articles is
--    very different. Stored explicitly for signal quality auditing.
--
--    raw_response: full LLM output preserved verbatim. Essential for prompt
--    iteration and debugging unexpected scores.
-- =============================================================================

CREATE TABLE sentiment_scores (
                                  id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                  market_id     UUID        NOT NULL REFERENCES markets (id),
                                  sentiment     VARCHAR(10) NOT NULL CHECK (sentiment IN ('BULLISH', 'BEARISH', 'NEUTRAL')),
                                  confidence    NUMERIC(4,3) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
                                  reasoning     TEXT,                      -- LLM's explanation of the score
                                  model_used    VARCHAR(100) NOT NULL,     -- e.g. 'claude-3-5-sonnet-20241022'
                                  article_count INTEGER      NOT NULL DEFAULT 0,
                                  raw_response  JSONB,                     -- verbatim LLM output for debugging
                                  scored_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  sentiment_scores               IS 'Time-series sentiment scores from the LangGraph AI agent.';
COMMENT ON COLUMN sentiment_scores.confidence    IS '0.0–1.0. Strategy rules gate on this value directly.';
COMMENT ON COLUMN sentiment_scores.article_count IS 'Articles analysed. Low count = low signal reliability.';
COMMENT ON COLUMN sentiment_scores.raw_response  IS 'Full LLM output. Never discard — invaluable for prompt tuning.';

-- Primary query pattern: latest score per market
CREATE INDEX idx_sentiment_market_time ON sentiment_scores (market_id, scored_at DESC);


-- =============================================================================
-- 7. NEWS_SUMMARIES
--    Separate from sentiment_scores: they're produced in the same LangGraph
--    run but serve different consumers (dashboard reader vs. trading engine)
--    and have different freshness requirements.
--
--    expires_at: the scheduler checks this before triggering a new LLM call.
--    If NOW() < expires_at, skip — primary cost-control for LLM API usage.
--
--    articles (JSONB): array of { url, title, published_at, source } objects.
--    Stored so the dashboard can render source links, and so you can audit
--    exactly what the LLM read when producing the summary.
-- =============================================================================

CREATE TABLE news_summaries (
                                id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                                market_id   UUID    NOT NULL REFERENCES markets (id),
                                summary     TEXT    NOT NULL,
                                articles    JSONB   NOT NULL DEFAULT '[]',   -- [{url, title, published_at, source}]
                                model_used  VARCHAR(100) NOT NULL,
                                fetched_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                expires_at  TIMESTAMPTZ  NOT NULL            -- scheduler skips re-fetch before this
);

COMMENT ON TABLE  news_summaries            IS 'LLM-generated news summaries per market. Separate lifetime from sentiment.';
COMMENT ON COLUMN news_summaries.expires_at IS 'Scheduler skips LLM call if NOW() < expires_at. Primary cost control.';
COMMENT ON COLUMN news_summaries.articles   IS 'Source articles as [{url,title,published_at,source}]. Rendered in dashboard.';

-- Latest valid summary per market
CREATE INDEX idx_news_market_expiry ON news_summaries (market_id, expires_at DESC);


-- =============================================================================
-- 8. PRICE_SNAPSHOTS
--    Time-series of market prices written by the scheduler every N minutes.
--    Powers two things:
--      1. Probability history chart on the dashboard
--      2. Backtesting: "what was the price when a strategy would have fired?"
--
--    Note: (market_id, snapshot_at) is a natural composite PK but UUID is
--    used for consistency. Add PARTITION BY RANGE (snapshot_at) monthly
--    if this table grows large.
-- =============================================================================

CREATE TABLE price_snapshots (
                                 id           UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
                                 market_id    UUID     NOT NULL REFERENCES markets (id),
                                 yes_price    NUMERIC(6,4) CHECK (yes_price BETWEEN 0 AND 1),
                                 no_price     NUMERIC(6,4) CHECK (no_price  BETWEEN 0 AND 1),
                                 volume_24h   NUMERIC(18,4),
                                 liquidity    NUMERIC(18,4),
                                 snapshot_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE price_snapshots IS 'Periodic price snapshots. Powers history charts and backtesting.';

-- Range queries: prices for market X between time A and B
CREATE INDEX idx_price_snapshots_market_time ON price_snapshots (market_id, snapshot_at DESC);


-- =============================================================================
-- 9. AUDIT_LOGS
--    The most important table for a trading bot. Every engine decision writes
--    a row here — order placed, order skipped, risk blocked, dry-run trade.
--
--    action: machine-readable event type for filtering/grouping in the dashboard.
--
--    signals (JSONB): snapshot of every input value at decision time:
--      {
--        "yes_price": 0.27,
--        "sentiment": "BULLISH",
--        "confidence": 0.91,
--        "daily_exposure_used": 45.00,
--        "rule_evaluation": [
--          { "field": "yes_price", "op": "lt", "value": 0.30, "result": true },
--          { "field": "sentiment.confidence", "op": "gt", "value": 0.8, "result": true }
--        ]
--      }
--    Even if the market moves later, you can reconstruct exactly what the
--    engine saw and why it acted as it did. This is the reasoning trail.
-- =============================================================================

CREATE TABLE audit_logs (
                            id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                            strategy_id  UUID    REFERENCES strategies (id),
                            market_id    UUID    REFERENCES markets (id),
                            order_id     UUID    REFERENCES orders (id),   -- NULL when action = ORDER_SKIPPED
                            user_id      UUID    NOT NULL REFERENCES users (id),
                            action       VARCHAR(50) NOT NULL
                                CHECK (action IN (
                                'STRATEGY_EVALUATED',
                                'ORDER_PLACED',
                                'ORDER_SKIPPED',
                                'RISK_BLOCKED',
                                'DRY_RUN_ORDER',
                                'SIGNAL_RECEIVED',
                                'MARKET_RESOLVED',
                                'POSITION_OPENED',
                                'POSITION_CLOSED',
                                'ALERT_SENT'
                                )),
    is_dry_run   BOOLEAN     NOT NULL DEFAULT TRUE,
    signals      JSONB,                 -- full snapshot of engine inputs at decision time
    reasoning    TEXT,                  -- human-readable explanation of the decision
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  audit_logs          IS 'Immutable decision trail. Every engine action logged with full context.';
COMMENT ON COLUMN audit_logs.signals  IS 'Snapshot of all signal values at decision time. Never mutated after insert.';
COMMENT ON COLUMN audit_logs.order_id IS 'NULL for ORDER_SKIPPED and RISK_BLOCKED. Non-null when an order was created.';

-- Dashboard: recent decisions for a strategy
CREATE INDEX idx_audit_strategy_time ON audit_logs (strategy_id, created_at DESC);

-- Debugging: all decisions for a specific market
CREATE INDEX idx_audit_market_time ON audit_logs (market_id, created_at DESC);

-- Filter by action type across all strategies
CREATE INDEX idx_audit_action ON audit_logs (action, created_at DESC);

-- GIN index for querying inside the signals JSONB
CREATE INDEX idx_audit_signals ON audit_logs USING GIN (signals);


-- =============================================================================
-- 10. ALERTS
--     Records every outbound notification with its delivery status.
--     Storing alerts in the DB enables:
--       - Replay of failed deliveries
--       - Audit of what was sent and when
--       - Deduplication (check for recent SENT alert before sending again)
--
--     channel: 'EMAIL' | 'WEBHOOK' — add 'SLACK', 'TELEGRAM' later freely.
--     payload: channel-specific body (email HTML, webhook JSON, etc.)
-- =============================================================================

CREATE TABLE alerts (
                        id         UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                        order_id   UUID    REFERENCES orders (id),
                        user_id    UUID    NOT NULL REFERENCES users (id),
                        type       VARCHAR(50)  NOT NULL
                            CHECK (type IN (
                                            'ORDER_PLACED', 'ORDER_FILLED', 'ORDER_FAILED',
                                            'RISK_LIMIT_HIT', 'POSITION_CLOSED', 'MARKET_RESOLVED'
                                )),
                        channel    VARCHAR(20)  NOT NULL CHECK (channel IN ('EMAIL', 'WEBHOOK')),
                        payload    JSONB        NOT NULL DEFAULT '{}',
                        status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
                        error      TEXT,                   -- populated on delivery failure
                        sent_at    TIMESTAMPTZ,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  alerts         IS 'Outbound notification log. Enables replay, audit, and deduplication.';
COMMENT ON COLUMN alerts.payload IS 'Channel-specific body: email HTML, webhook JSON, etc.';
COMMENT ON COLUMN alerts.error   IS 'Delivery error message when status = FAILED.';

CREATE INDEX idx_alerts_status ON alerts (status) WHERE status = 'PENDING';
CREATE INDEX idx_alerts_user   ON alerts (user_id, created_at DESC);


-- =============================================================================
-- UPDATED_AT TRIGGER
-- Automatically update updated_at on every mutable table.
-- =============================================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_markets_updated_at
    BEFORE UPDATE ON markets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_strategies_updated_at
    BEFORE UPDATE ON strategies
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_positions_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();