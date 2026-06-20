-- =============================================================================
-- PolyPilot — Seed Data
-- Flyway migration V2
--
-- Inserts:
--   1. Default admin user  (password must be changed on first run)
--   2. Sample strategy     (dry_run = TRUE — safe by default)
--
-- The password hash below is BCrypt for 'changeme' with 12 rounds.
-- Change this before running in any real environment.
-- =============================================================================


-- ---------------------------------------------------------------------------
-- 1. DEFAULT ADMIN USER
-- ---------------------------------------------------------------------------

INSERT INTO users (id, email, password_hash, role)
VALUES (
           gen_random_uuid(),
           'admin@polypilot.local',
           '$2a$12$GlCBCzuOpDKRO4N3MTt8oeGwuLYBNI4/2JxIjgB/C7mPc/pZ4ySAK',  -- BCrypt 'changeme'
           'ADMIN'
       )
    ON CONFLICT (email) DO NOTHING;


-- ---------------------------------------------------------------------------
-- 2. SAMPLE STRATEGY (dry_run = TRUE, disabled)
--
--    Rule tree reads as:
--      BET YES if:
--        yes_price < 0.30
--        AND (sentiment.confidence > 0.80 OR volume_24h > 50000)
--
--    This strategy is DISABLED and in DRY_RUN mode. Flip enabled = TRUE
--    via the dashboard when you're ready to test it in simulation.
-- ---------------------------------------------------------------------------

INSERT INTO strategies (
    id,
    name,
    description,
    enabled,
    dry_run,
    cron_expression,
    token_side,
    order_type,
    max_bet_size,
    max_daily_exposure,
    stop_loss_threshold,
    rule_tree
)
VALUES (
           gen_random_uuid(),
           'Low-Probability YES with Sentiment Gate',
           'Bets YES on markets priced below 30¢ when AI sentiment confidence is high or volume is strong. Dry-run only until manually enabled.',
           FALSE,    -- disabled: must be explicitly turned on
           TRUE,     -- dry_run: will never place real orders until flipped to FALSE
           '0 */5 * * * *',   -- every 5 minutes
           'YES',
           'GTC',
           10.0,     -- max $10 USDC per bet
           100.0,    -- max $100 USDC per day
           0.10,     -- stop-loss at 10% drawdown
           '{
               "operator": "AND",
               "conditions": [
                   {
                       "field": "yes_price",
                       "op": "lt",
                       "value": 0.30
                   },
                   {
                       "operator": "OR",
                       "conditions": [
                           {
                               "field": "sentiment.confidence",
                               "op": "gt",
                               "value": 0.80
                           },
                           {
                               "field": "volume_24h",
                               "op": "gt",
                               "value": 50000
                           }
                       ]
                   }
               ]
           }'::jsonb
       )
    ON CONFLICT (name) DO NOTHING;