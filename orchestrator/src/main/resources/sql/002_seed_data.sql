-- =============================================================================
-- PolyPilot — Seed Data
--
-- Inserts:
--   1. Roles: ADMIN, USER
--   2. Default admin user + its PASSWORD identity (change the password on first run)
--   3. Demo user + its PASSWORD identity (login without a wallet)
--   4. Tracked BTC/ETH series + job schedule rows
--   5. Sample strategy (dry_run = TRUE — safe by default)
--
-- Credentials now live in user_identities, not on users. The secret column is a
-- BCrypt hash (strength 12, matching SecurityConfig's BCryptPasswordEncoder(12)).
--
-- Every insert here is idempotent (fixed UUIDs / unique keys + ON CONFLICT
-- DO NOTHING) so spring.sql.init can re-run it on every boot.
-- =============================================================================


-- ---------------------------------------------------------------------------
-- 1. ROLES
-- ---------------------------------------------------------------------------

INSERT INTO roles (role_name)
VALUES ('ADMIN'), ('USER')
ON CONFLICT (role_name) DO NOTHING;


-- ---------------------------------------------------------------------------
-- 2. DEFAULT ADMIN USER  (fixed UUID so the seed is safe to re-run)
--    Password identity: admin@polypilot.local
    -- DEFAULT PASSWORD: password
-- ---------------------------------------------------------------------------

INSERT INTO users (id, role_id, display_name, enabled, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        (SELECT id FROM roles WHERE role_name = 'ADMIN'),
        'PolyPilot Admin',
        TRUE,
        now(),
        now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_identities (user_id, type, identifier, secret, verified_at, created_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'PASSWORD',
        'admin@polypilot.local',
        '$2b$12$.fcZ6DXiLKT5ro6yjrpsBOkEV3MHglfXsyKa.bAW/B7v.tLidDTqm',
        now(),
        now())
ON CONFLICT (type, identifier) DO NOTHING;


-- ---------------------------------------------------------------------------
-- 3. DEMO USER  (fixed UUID, role USER, no wallet required)
--    Password identity: demo@polypilot.local
-- ---------------------------------------------------------------------------

INSERT INTO users (id, role_id, display_name, enabled, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000002',
        (SELECT id FROM roles WHERE role_name = 'USER'),
        'PolyPilot Demo',
        TRUE,
        now(),
        now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_identities (user_id, type, identifier, secret, verified_at, created_at)
VALUES ('00000000-0000-0000-0000-000000000002',
        'PASSWORD',
        'demo@polypilot.local',
        '$2b$12$.fcZ6DXiLKT5ro6yjrpsBOkEV3MHglfXsyKa.bAW/B7v.tLidDTqm',
        now(),
        now())
ON CONFLICT (type, identifier) DO NOTHING;

-- Reference vocabularies for the ohlc-sync job (moved out of application.yml).
-- Idempotent: DO UPDATE refreshes descriptive fields on re-seed. `enabled` is
-- intentionally NOT in the SET clause so a manual disable in the DB survives a
-- reboot.
INSERT INTO ticker (symbol, binance_symbol, display_name, base_asset, quote_asset, sort_order)
VALUES ('BTC', 'BTCUSDT', 'Bitcoin',  'BTC', 'USDT', 1),
       ('ETH', 'ETHUSDT', 'Ethereum', 'ETH', 'USDT', 2)
    ON CONFLICT (symbol) DO UPDATE SET
        binance_symbol = EXCLUDED.binance_symbol,
        display_name   = EXCLUDED.display_name,
        base_asset     = EXCLUDED.base_asset,
        quote_asset    = EXCLUDED.quote_asset,
        sort_order     = EXCLUDED.sort_order,
        updated_at     = now();

INSERT INTO timeframe (code, label, milliseconds, sort_order)
VALUES ('1m',  '1 minute',   60000,      1),
       ('3m',  '3 minutes',  180000,     2),
       ('5m',  '5 minutes',  300000,     3),
       ('15m', '15 minutes', 900000,     4),
       ('30m', '30 minutes', 1800000,    5),
       ('1h',  '1 hour',     3600000,    6),
       ('2h',  '2 hours',    7200000,    7),
       ('4h',  '4 hours',    14400000,   8),
       ('6h',  '6 hours',    21600000,   9),
       ('8h',  '8 hours',    28800000,   10),
       ('12h', '12 hours',   43200000,   11),
       ('1d',  '1 day',      86400000,   12),
       ('3d',  '3 days',     259200000,  13),
       ('1w',  '1 week',     604800000,  14),
       ('1M',  '1 month',    NULL,       15)
    ON CONFLICT (code) DO UPDATE SET
        label        = EXCLUDED.label,
        milliseconds = EXCLUDED.milliseconds,
        sort_order   = EXCLUDED.sort_order,
        updated_at   = now();


-- The five seeded series are the ones the bot tracks out of the box, so
-- tracked = TRUE. DO UPDATE (not DO NOTHING) so databases seeded before the
-- tracked column existed still get switched on. Still idempotent.
INSERT INTO series (polymarket_id, ticker, slug, title, recurrence, tracked, ticker_id)
VALUES
    ('10684', 'btc-up-or-down-5m',       'btc-up-or-down-5m',       'BTC Up or Down 5m',     '5m',     TRUE, (SELECT id FROM ticker WHERE symbol = 'BTC')),
    ('10114', 'btc-up-or-down-hourly',   'btc-up-or-down-hourly',   'BTC Up or Down Hourly', 'hourly', TRUE, (SELECT id FROM ticker WHERE symbol = 'BTC')),
    ('41',    'btc-up-or-down-daily',    'btc-up-or-down-daily',    'BTC Up or Down Daily',  'daily',  TRUE, (SELECT id FROM ticker WHERE symbol = 'BTC')),
    ('10117', 'eth-up-or-down-hourly',   'eth-up-or-down-hourly',   'ETH Up or Down Hourly', 'hourly', TRUE, (SELECT id FROM ticker WHERE symbol = 'ETH')),
    ('40',    'eth-up-or-down-daily',    'eth-up-or-down-daily',    'ETH Up or Down Daily',  'daily',  TRUE, (SELECT id FROM ticker WHERE symbol = 'ETH'))
    ON CONFLICT (ticker) DO UPDATE SET
    tracked = EXCLUDED.tracked;


INSERT INTO job_schedule (job_name, cron_expression, updated_at)
VALUES
    ('series-sync',         '0 */5 * * * *',   now()),
    ('open-market-sync',    '0 */5 * * * *',   now()),
    ('ohlc-sync',           '0 * * * * *',     now()),
    ('position-close-sweep', '0 0 */1 * * *',  now()),
    ('news-sync',           '0 */15 * * * *',  now())
    ON CONFLICT (job_name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. INDICATOR CATALOG  (seed-owned reference data — see section 11 of 001_schema.sql)
--
--    Every statement below is idempotent via ON CONFLICT ... DO UPDATE keyed on
--    the natural/unique key, so 002 stays the single source of truth: edits to
--    descriptive text, defaults or ordering propagate on the next boot. Same
--    technique as the `series` seed above.
--
--    Child rows resolve their indicator_id with a sub-select on indicators.key,
--    so they do not depend on a fixed UUID for the parent.
-- ---------------------------------------------------------------------------

-- 6a. Universal parameter vocabularies (shared enum lists only: source, timeframe)
INSERT INTO universal_parameters (key, name, description, data_type, allowed_values, default_value)
VALUES
    ('source', 'Source',
     'The price series an indicator is calculated on.',
     'ENUM',
     '["open","high","low","close","hl2","hlc3","ohlc4"]'::jsonb,
     'close'),
    ('timeframe', 'Timeframe',
     'The candle interval the indicator is computed on. Has no universal default — it must be set by the strategy configuration.',
     'ENUM',
     '["1m","3m","5m","15m","30m","1h","2h","4h","6h","8h","12h","1d","3d","1w","1M"]'::jsonb,
     NULL)
ON CONFLICT (key) DO UPDATE SET
    name          = EXCLUDED.name,
    description   = EXCLUDED.description,
    data_type     = EXCLUDED.data_type,
    allowed_values = EXCLUDED.allowed_values,
    default_value = EXCLUDED.default_value,
    updated_at    = now();

-- 6b. Indicators (6 rows)
INSERT INTO indicators (key, name, abbreviation, description, category, enabled, display_order)
VALUES
    ('sma', 'Simple Moving Average', 'SMA',
     'An unweighted arithmetic mean of price over a fixed number of preceding candles. Used to identify overall trend direction and as a baseline for crossover-based signals (price vs. SMA, or SMA vs. SMA).',
     'TREND', TRUE, 1),
    ('ema', 'Exponential Moving Average', 'EMA',
     'A moving average that weights recent candles more heavily than older ones, making it more responsive to new price action than SMA. Commonly used for faster trend detection and in crossover pairs (e.g. EMA(12)/EMA(26)).',
     'TREND', TRUE, 2),
    ('rsi', 'Relative Strength Index', 'RSI',
     'A momentum oscillator, scaled 0-100, that measures the speed and magnitude of recent price changes. Used to identify overbought/oversold conditions and momentum shifts.',
     'MOMENTUM', TRUE, 3),
    ('macd', 'Moving Average Convergence Divergence', 'MACD',
     'A trend/momentum indicator derived from the difference between two EMAs (the MACD line) and an EMA of that difference (the signal line). Used to detect momentum shifts and trend changes via line/signal crossovers and zero-line crosses. Filed under MOMENTUM although it is trend + momentum.',
     'MOMENTUM', TRUE, 4),
    ('atr', 'Average True Range', 'ATR',
     'A volatility measure based on the average of the "true range" (which accounts for gaps) over a fixed number of candles. Not directional — used for stop-loss/target sizing, volatility filtering, and as an input to other indicators.',
     'VOLATILITY', TRUE, 5),
    ('volume_ma', 'Volume Moving Average', 'Vol MA',
     'A moving average applied to traded volume rather than price, used as a baseline to detect volume spikes or drop-offs relative to recent activity (e.g. confirming a price move with above-average volume).',
     'VOLUME', TRUE, 6)
ON CONFLICT (key) DO UPDATE SET
    name         = EXCLUDED.name,
    abbreviation = EXCLUDED.abbreviation,
    description  = EXCLUDED.description,
    category     = EXCLUDED.category,
    enabled      = EXCLUDED.enabled,
    display_order = EXCLUDED.display_order,
    updated_at   = now();

-- 6c. Indicator parameters (one row per (indicator, parameter))
INSERT INTO indicator_parameters
    (indicator_id, key, name, description, data_type, universal_key, constraints, default_value, required, display_order)
VALUES
    -- SMA
    ((SELECT id FROM indicators WHERE key = 'sma'), 'period', 'Period',
     'Number of candles averaged. TA-Lib default 30; 20, 50 and 200 are also common depending on strategy horizon.',
     'INTEGER', NULL, '{"min":2,"max":1000,"step":1}'::jsonb, '30', TRUE, 1),
    ((SELECT id FROM indicators WHERE key = 'sma'), 'source', 'Source',
     'The price series the average is calculated on.',
     'ENUM', 'source', '{}'::jsonb, 'close', TRUE, 2),
    ((SELECT id FROM indicators WHERE key = 'sma'), 'timeframe', 'Timeframe',
     'The candle interval the indicator is computed on. Must be set by the strategy configuration.',
     'ENUM', 'timeframe', '{}'::jsonb, NULL, TRUE, 3),

    -- EMA
    ((SELECT id FROM indicators WHERE key = 'ema'), 'period', 'Period',
     'Number of candles in the EMA. TA-Lib default 30; 9, 12, 20, 26 and 50 are common depending on use case.',
     'INTEGER', NULL, '{"min":2,"max":1000,"step":1}'::jsonb, '30', TRUE, 1),
    ((SELECT id FROM indicators WHERE key = 'ema'), 'source', 'Source',
     'The price series the average is calculated on.',
     'ENUM', 'source', '{}'::jsonb, 'close', TRUE, 2),
    ((SELECT id FROM indicators WHERE key = 'ema'), 'timeframe', 'Timeframe',
     'The candle interval the indicator is computed on. Must be set by the strategy configuration.',
     'ENUM', 'timeframe', '{}'::jsonb, NULL, TRUE, 3),

    -- RSI
    ((SELECT id FROM indicators WHERE key = 'rsi'), 'period', 'Period',
     'Look-back for the average gain/loss. Wilder / TA-Lib default 14.',
     'INTEGER', NULL, '{"min":2,"max":1000,"step":1}'::jsonb, '14', TRUE, 1),
    ((SELECT id FROM indicators WHERE key = 'rsi'), 'source', 'Source',
     'The price series the oscillator is calculated on.',
     'ENUM', 'source', '{}'::jsonb, 'close', TRUE, 2),
    ((SELECT id FROM indicators WHERE key = 'rsi'), 'timeframe', 'Timeframe',
     'The candle interval the indicator is computed on. Must be set by the strategy configuration.',
     'ENUM', 'timeframe', '{}'::jsonb, NULL, TRUE, 3),
    ((SELECT id FROM indicators WHERE key = 'rsi'), 'overbought_level', 'Overbought level',
     'Upper threshold marking the overbought zone. Conventionally greater than the oversold level.',
     'NUMBER', NULL, '{"min":0,"max":100}'::jsonb, '70', TRUE, 4),
    ((SELECT id FROM indicators WHERE key = 'rsi'), 'oversold_level', 'Oversold level',
     'Lower threshold marking the oversold zone. Conventionally less than the overbought level.',
     'NUMBER', NULL, '{"min":0,"max":100}'::jsonb, '30', TRUE, 5),

    -- MACD
    ((SELECT id FROM indicators WHERE key = 'macd'), 'fast_period', 'Fast period',
     'Length of the fast EMA. TA-Lib default 12. Conventionally less than the slow period.',
     'INTEGER', NULL, '{"min":2,"max":1000,"step":1}'::jsonb, '12', TRUE, 1),
    ((SELECT id FROM indicators WHERE key = 'macd'), 'slow_period', 'Slow period',
     'Length of the slow EMA. TA-Lib default 26. Conventionally greater than the fast period.',
     'INTEGER', NULL, '{"min":2,"max":1000,"step":1}'::jsonb, '26', TRUE, 2),
    ((SELECT id FROM indicators WHERE key = 'macd'), 'signal_period', 'Signal period',
     'Length of the EMA applied to the MACD line to produce the signal line. TA-Lib default 9.',
     'INTEGER', NULL, '{"min":1,"max":1000,"step":1}'::jsonb, '9', TRUE, 3),
    ((SELECT id FROM indicators WHERE key = 'macd'), 'source', 'Source',
     'The price series the MACD is calculated on.',
     'ENUM', 'source', '{}'::jsonb, 'close', TRUE, 4),
    ((SELECT id FROM indicators WHERE key = 'macd'), 'timeframe', 'Timeframe',
     'The candle interval the indicator is computed on. Must be set by the strategy configuration.',
     'ENUM', 'timeframe', '{}'::jsonb, NULL, TRUE, 5),

    -- ATR  (no `source` — ATR is defined on high/low/previous-close, not a single price series)
    ((SELECT id FROM indicators WHERE key = 'atr'), 'period', 'Period',
     'Look-back for the true-range average. Wilder / TA-Lib default 14.',
     'INTEGER', NULL, '{"min":2,"max":1000,"step":1}'::jsonb, '14', TRUE, 1),
    ((SELECT id FROM indicators WHERE key = 'atr'), 'smoothing_method', 'Smoothing method',
     'Averaging method applied to the true range. "wilder" (a.k.a. RMA) is the TA-Lib default.',
     'ENUM', NULL, '{"values":["wilder","sma","ema"]}'::jsonb, 'wilder', TRUE, 2),
    ((SELECT id FROM indicators WHERE key = 'atr'), 'timeframe', 'Timeframe',
     'The candle interval the indicator is computed on. Must be set by the strategy configuration.',
     'ENUM', 'timeframe', '{}'::jsonb, NULL, TRUE, 3),

    -- Volume MA  (no `source` — operates on the volume field, not a price series)
    ((SELECT id FROM indicators WHERE key = 'volume_ma'), 'period', 'Period',
     'Number of candles of volume averaged. 20 is the common default (no TA-Lib-mandated default for volume).',
     'INTEGER', NULL, '{"min":2,"max":1000,"step":1}'::jsonb, '20', TRUE, 1),
    ((SELECT id FROM indicators WHERE key = 'volume_ma'), 'ma_type', 'MA type',
     'Moving-average type applied to volume.',
     'ENUM', NULL, '{"values":["sma","ema"]}'::jsonb, 'sma', TRUE, 2),
    ((SELECT id FROM indicators WHERE key = 'volume_ma'), 'timeframe', 'Timeframe',
     'The candle interval the indicator is computed on. Must be set by the strategy configuration.',
     'ENUM', 'timeframe', '{}'::jsonb, NULL, TRUE, 3)
ON CONFLICT (indicator_id, key) DO UPDATE SET
    name          = EXCLUDED.name,
    description   = EXCLUDED.description,
    data_type     = EXCLUDED.data_type,
    universal_key = EXCLUDED.universal_key,
    constraints   = EXCLUDED.constraints,
    default_value = EXCLUDED.default_value,
    required      = EXCLUDED.required,
    display_order = EXCLUDED.display_order;

-- 6d. Indicator outputs
INSERT INTO indicator_outputs
    (indicator_id, key, name, description, value_scale, is_default, display_order)
VALUES
    ((SELECT id FROM indicators WHERE key = 'sma'), 'value', 'SMA value',
     'The moving-average value, in price units.', 'PRICE', TRUE, 1),

    ((SELECT id FROM indicators WHERE key = 'ema'), 'value', 'EMA value',
     'The moving-average value, in price units.', 'PRICE', TRUE, 1),

    ((SELECT id FROM indicators WHERE key = 'rsi'), 'value', 'RSI value',
     'The oscillator reading, 0-100.', 'OSCILLATOR_0_100', TRUE, 1),

    ((SELECT id FROM indicators WHERE key = 'macd'), 'macd', 'MACD line',
     'Difference between the fast and slow EMAs.', 'UNBOUNDED', TRUE, 1),
    ((SELECT id FROM indicators WHERE key = 'macd'), 'signal', 'Signal line',
     'EMA of the MACD line.', 'UNBOUNDED', FALSE, 2),
    ((SELECT id FROM indicators WHERE key = 'macd'), 'histogram', 'Histogram',
     'MACD line minus signal line.', 'UNBOUNDED', FALSE, 3),

    ((SELECT id FROM indicators WHERE key = 'atr'), 'value', 'ATR value',
     'The average true range, in price units.', 'PRICE', TRUE, 1),

    ((SELECT id FROM indicators WHERE key = 'volume_ma'), 'value', 'Volume MA value',
     'The average traded volume over the look-back window.', 'VOLUME', TRUE, 1)
ON CONFLICT (indicator_id, key) DO UPDATE SET
    name          = EXCLUDED.name,
    description   = EXCLUDED.description,
    value_scale   = EXCLUDED.value_scale,
    is_default    = EXCLUDED.is_default,
    display_order = EXCLUDED.display_order;