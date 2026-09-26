# CLAUDE.md — orchestrator

Java 21 / Spring Boot 4.1, port 8080. Owns the DB, auth, scheduling, market sync, technical-indicator
calculation, and strategy rule-tree evaluation. See the repo-root `CLAUDE.md` for the monorepo picture and the
services this one talks to.

## Package layout

Feature-sliced (own `controller/`/`service/`/`dto/`/`entity/`/`repository/` per package) is the current style;
older code is layered globally instead. New work follows the feature-sliced style; shared plumbing goes in
`common/`.

- `auth/` — email/password login (`AuthController`, `AuthService`, `JwtService`, `JwtFilter`), `AdminController`.
  See **Auth** below.
- `auth/siwe/` — SIWE (EIP-4361) wallet login. See **Auth** below.
- `wallet/` — post-login EIP-712 ClobAuth credential derivation. See **Auth** below.
- `market/` — Polymarket series/market discovery and refresh. See **Market sync**.
- `health/` — fans `GET /health` out to `auth-service`/`ai-agent` and reports DB/schema/cron checks.
- `ohlc/` — Binance candle ingestion. See **OHLC, indicators, strategies**.
- `reference/` — the `ticker`/`timeframe` lookup tables that drive what `ohlc/` syncs.
- `indicator/` — ta4j-based technical indicator calculation, config-driven via a DB catalog.
- `strategy/` — JSON rule-tree CRUD and evaluation engine; `strategy/scheduler/` drives per-strategy cron
  evaluation (see **`strategy/scheduler/`** below).
- `common/exception/` — `GlobalExceptionHandler`, `ErrorResponse`. See **Error handling**.
- `common/cache/` — `JsonRedisCache`, a shared best-effort read-through Redis JSON cache used by
  `IndicatorCatalogService` and `ReferenceDataService` for DB-backed lookup data. Any `DataAccessException` is
  logged and treated as a miss/no-op — a Redis outage degrades to DB-only, it never breaks the caller.
- `config/` — cross-cutting `@Configuration`, including `SecurityConfig`.

**Legacy flat packages still in live use, not dead code**: `entity/`, `repository/`, `enums/` hold `Order`,
`Position`, `Alert`, `AuditLog`, `NewsSummary`, `SentimentScore` — and, notably, **`Strategy`**. `controller/` is
currently empty.

**Known layout quirk — don't "fix" this mid-refactor without checking history**: `strategy/` is a hybrid. Its
controller/service/dto/json/mapper live in the new feature-sliced package, but the JPA entity it maps
(`com.polypilot.entity.Strategy`) and its repository are still in the legacy flat packages. This looks like an
intentional, in-progress migration, not an oversight.

## Auth

Two login paths, not equivalent:

- **SIWE (`auth/siwe/`) is the primary, self-serve path.** `GET /api/auth/siwe/nonce` issues a nonce;
  `POST /api/auth/siwe/verify` parses the EIP-4361 message, enforces policy (domain match against `siwe.domain`,
  nonce exists and is consumed atomically — a lost race also counts as replay, `issuedAt`/`notBefore`/
  `expirationTime` window checks, chain id in `siwe.allowed-chain-ids`), then calls `auth-service`'s
  `/auth/siwe/verify` to recover the signing address (crypto verification only happens there — see
  `auth-service/CLAUDE.md`), and on success **auto-provisions** a `User` + `UserIdentity(type=ETH_WALLET)` if the
  address hasn't logged in before. There is no separate registration endpoint — first SIWE login *is* signup.
  Every rejection path converges on one identical `401 "SIWE authentication failed"` (via `SiweRejectionReason`)
  to avoid leaking which check failed; the real reason is logged at `debug` server-side only.
- **`AuthController.login` (email/password) exists only for the DB-seeded admin account** (`002_seed_data.sql`),
  not for general signup — there is no way to create a `USER`-role account through it. `AuthService.login`
  deliberately bypasses the `AuthenticationManager` and calls `passwordEncoder.matches` directly, returning an
  identical 401 for unknown-email and wrong-password to avoid user enumeration. The
  `AuthenticationManager`/`DaoAuthenticationProvider` beans are wired but unused.

Both paths issue the same JWT (`JwtService.generate`). **The JWT principal is the user's `UUID`** (from the
`sub` claim), not an email string — `JwtFilter` extracts it via `jwtService.extractUserId(token)` and populates
`SecurityContext` with it plus `ROLE_<role>` as the authority, which is what `@AuthenticationPrincipal UUID
userId` (`WalletController`, `StrategyController`) and `hasRole("ADMIN")`/`@PreAuthorize` rely on. Sessions are
`STATELESS`, CSRF is off.

`SecurityConfig`'s permit-all list is exactly `/api/auth/**`, `/actuator/health`, `/error` — **the custom
`GET /health` is deliberately *not* in that list**, so it requires a valid JWT like everything else under
`anyRequest().authenticated()`.

**Wallet connection (`wallet/`) — DEPRECATED/FROZEN.** Under the repo's Polymarket API policy (root `CLAUDE.md`),
this pipeline is deprecated/frozen: it exists only to derive Polymarket L2 trading credentials via
`auth-service`'s ClobAuth relay, which sends data to Polymarket rather than reading from it. The code below
remains accurate as a description of what's implemented, but it should not be extended and no new functionality
should be built on top of the credentials it produces. It is a two-step EIP-712 challenge, run only after a user
is already logged in:

1. `GET /api/wallet/challenge?address=0x…` → `WalletService` writes a timestamp to Redis under
   `challenge:<lowercased-address>` with a 5-minute TTL and returns it.
2. `POST /api/wallet/connect` with the signature → validates the timestamp against Redis, forwards to
   `auth-service` via `AuthServiceClient` (blocking on purpose — no credentials, no trading), AES-256-GCM
   encrypts the returned L2 triple via `EncryptionService`, persists it, then deletes the Redis key so the
   challenge can't be replayed.

`EncryptionService` needs `encryption.key` = 32 raw bytes base64-encoded (`openssl rand -base64 32`, set via
`ENCRYPTION_KEY`) and prepends the random 12-byte IV to each ciphertext, so no separate IV store is needed.
Plaintext credentials never live on `WalletCredential`; only the `*_enc` columns exist. The trust boundary is the
point: the orchestrator holds encrypted L2 credentials and never sees a private key — all signing crosses the
HTTP hop into `auth-service`.

## Market sync: DB-driven cron, not `@Scheduled`

This is read-only against Polymarket's Gamma API (discovery + price refresh, never writes) and is the normal,
supported way this service gets market data — unaffected by the repo's Polymarket API policy (root
`CLAUDE.md`), which only deprecates/freezes calls that *send* data to Polymarket.

`MarketScheduler` replaces annotation-based scheduling so cron expressions are changeable at runtime. On
`@PostConstruct` it reads each cron from the `job_schedule` table (falling back to `0 */5 * * * *` if the row is
missing or the expression is invalid) and registers it on a 2-thread `ThreadPoolTaskScheduler`, keeping the
`ScheduledFuture` in a map keyed by job name. `POST /schedule/config {job, cron}` cancels and re-registers a job
live. Two jobs exist: `series-sync` and `open-market-sync`. (`ScheduleController.getCurrentSchedules()` currently
just returns `null` — a known, still-open gap, not a documented behavior to rely on.)

`MarketSyncService` holds the batch loops; each item is delegated to `MarketItemSyncService`, a **separate
bean**, so the `@Transactional(REQUIRES_NEW)` boundary is a real proxy hop (a self-invoked `this.method()` would
silently skip it). One bad series or market — snapshot included — rolls back on its own and the loop moves on.
Keep the per-item work in the second bean; don't inline it back or collapse the snapshot+update into the caller.
`ohlc/`'s sync (below) follows this exact same pattern.

- `syncAllSeries` → **discovery only**. For each series where `series.tracked = true`, hits Gamma `/events`
  filtered to the next open/upcoming event and *inserts* its first market if absent. It never updates an
  existing row.
- `updateOpenMarkets` → **the refresher**. Re-reads every `OPEN` market from `/markets/{id}` each run. If the
  status, prices, `volume24hr` or `liquidityNum` moved it writes a `PriceSnapshot` of the *old* values then an
  in-place update (a transition out of `OPEN` is just another such update); if nothing moved it only bumps
  `last_synced_at` via `MarketRepository.touchLastSyncedAt`. This is where price history accumulates.

The Gamma base URL comes from `polymarket.gamma-url` (`POLYMARKET_GAMMA_URL`, default
`https://gamma-api.polymarket.com`), injected into `MarketItemSyncService`.

### Polymarket DTO → entity mapping

MapStruct (`MarketMapper`, `componentModel = "spring"`) maps `MarketResponseDto` to `Market`, with paired
`toEntity` / `updateEntity(@MappingTarget)` methods that must be kept in sync — every `@Mapping` is duplicated
across both. `updateEntity` additionally carries `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)`:
Gamma drops `volume24hr` / `liquidityNum` / `outcomePrices` from the payload once a market resolves, and
without `IGNORE` a refresh would null out the last good values. `volume24h` maps from `volume24hr` and
`liquidity` from `liquidityNum` (the numeric aggregates — the bare `liquidity` string is AMM-only). Three
helper `@Component`s are pulled in via `uses`:

- `OutcomePricesMapper` — Gamma returns `outcomePrices` as a *JSON-encoded string* (`"[\"0.52\",\"0.48\"]"`),
  so index 0 becomes `upPrice`, index 1 `downPrice`, and a price of exactly `1` resolves the `MarketOutcome`
  (`UP`/`DOWN` for these markets; `markets.outcome` CHECK is `('UP','DOWN', NULL)`). Malformed / blank / absent
  input yields `null`, never an exception.
- `MarketStatusResolver` — collapses Gamma's `umaResolutionStatus` + `closed` flags into `MarketStatus`
  (RESOLVED > CLOSED > OPEN).
- `MarketDateResolver` — fills `resolutionDate` from `closedTime` (non-ISO shape) → `umaEndDate` → `endDate`.

Lombok and MapStruct both run as annotation processors, ordered explicitly in `pom.xml` with
`lombok-mapstruct-binding` between them. Reordering those paths breaks generation. Generated mappers land in
`target/generated-sources/annotations/`.

This is Spring Boot 4 / Jackson 3 — imports are `tools.jackson.databind.ObjectMapper`, not
`com.fasterxml.jackson.databind`. `@JsonProperty` still comes from `com.fasterxml.jackson.annotation`.

## OHLC, indicators, and strategies

These three packages form one pipeline: `ohlc/` stores candles → `indicator/` computes technical indicators on
top of them, on demand → `strategy/` combines indicator and market-field comparisons into a boolean rule tree.
`ohlc/`, `reference/`, and `indicator/`'s catalog all share one convention: **enable/disable behavior with a DB
row, not a redeploy** (`ticker`/`timeframe` rows gate what `ohlc/` syncs; `indicators` rows gate what's
calculable) — the same philosophy `job_schedule` already uses for cron.

### `ohlc/` — Binance candle sync

`BinanceClient` (`ohlc/client/`) calls Binance's public klines REST API — a different upstream than Gamma,
configured via `ohlc/config/BinanceProperties`. `OhlcSyncService.syncAll()` reads the enabled `(symbol,
timeframe)` combinations from `ReferenceDataService` and delegates each to `OhlcStreamSyncService`
(`REQUIRES_NEW`, same isolation pattern as `MarketItemSyncService` above — one bad stream is logged and skipped).
Streams run sequentially, not fanned out, because volume is well under Binance's IP weight budget. Candles land
in `OhlcCandle` (composite-keyed `OhlcCandleId`), the `ohlc_candles` table.

### `reference/` — ticker/timeframe lookup

`Ticker` and `Timeframe` are DB-backed config tables (seeded rows like `('BTC','BTCUSDT',...)`,
`('1m','1 minute',60000,...)`). `ReferenceDataService` exposes `enabledBinanceSymbols()` /
`enabledTimeframeCodes()`, consumed by `OhlcSyncService` to decide what to sync.

### `indicator/` — ta4j-based calculation

Config-driven: one `IndicatorCalculatorRegistry` (in-memory map, built once in its constructor) holds six
registered calculators today — `sma`, `ema`, `rsi`, `macd` (returns `macd`/`signal`/`histogram`), `atr`
(`smoothing_method`: sma/ema/wilder), `volume_ma` (`ma_type`: sma/ema) — keyed by an `indicators.key` DB catalog
value, not a class-per-indicator hierarchy. Adding indicator #7 is one `register(...)` call here plus matching
`002_seed_data.sql` catalog rows.

`IndicatorCalculationService.calculate(binanceSymbol, indicatorKey, rawParams)` is synchronous and computed
on-the-fly — **never persisted**. It merges caller params with the catalog's per-indicator and "universal"
parameter defaults (e.g. `source` defaults to `close`; `timeframe` deliberately has **no universal default** and
must always come from the caller), pulls enough trailing **closed** candles via
`OhlcCandleRepository.findRecentClosedCandles`, builds a ta4j `BarSeries` via `OhlcBarSeriesFactory`, and
delegates to the registered calculator. Lookback size is `max(50, largestIntegerPeriodParam * 3)` — a documented
rule-of-thumb buffer for ta4j's infinite-impulse-response moving averages (EMA/RSI/MACD), not a guarantee. The
method runs `@Transactional(readOnly = true)` because catalog lazy-loading still needs a JPA session even though
nothing is written.

Failure modes are deliberate, not accidental gaps: unknown/disabled key → 404, missing required param → 400, not
enough closed candles → 422, unregistered `smoothing_method`/`ma_type` value → 400, catalog entry with no
calculator wired up yet → 501.

`indicator/calc/source/` resolves price-source params (`open`/`high`/`low`/`close`/…) to a ta4j `Indicator<Num>`,
including derived sources `Hl2Indicator`/`Hlc3Indicator`/`Ohlc4Indicator`. The catalog itself
(`IndicatorCatalogService`, `Indicator`/`IndicatorParameter`/`UniversalParameter`/`IndicatorOutput` entities) is
exposed read-only for the strategy-builder UI's condition-fields picker.

### `strategy/` — rule-tree CRUD and evaluation

The entity is the legacy `entity.Strategy` (see the layout quirk above) — `rule_tree` is `jsonb`, mapped as a
`String` with `@JdbcTypeCode(SqlTypes.JSON)`, alongside `tokenSide`, `orderType`, `maxBetSize`,
`maxDailyExposure`, `stopLossThreshold`, `cronExpression`, `series` (a `@ManyToOne` to `market.entity.Series`,
`series_id NOT NULL`), `enabled`, `dryRun`, and soft-delete via `deletedAt`. A strategy is attached to a
**series**, not a fixed market — `StrategyService.runEvaluation` resolves the evaluation target fresh on every
tick via `MarketRepository.findFirstBySeriesIdAndStatusOrderByCreatedAtDesc(seriesId, OPEN)` (the series's
current `OPEN` market) and `series.getAsset().getBinanceSymbol()` (the series's linked `reference.Ticker`), so
a series's market rollover is picked up automatically without editing the strategy. `StrategyService.create`/
`update` reject an unknown `seriesId` (404) or a series with no linked asset (422) via `resolveSeries`.

**`enabled` defaults to `true`** (`Strategy.enabled`'s field default and the `strategies.enabled` column's DB
default both changed from `FALSE`/off-by-default) — a newly created strategy is scheduled immediately.
`StrategyService.create` sets `enabled(true)` explicitly rather than relying on the entity default alone (Lombok
`@Builder` doesn't apply field initializers to unset builder fields the way plain construction would).
`PUT /strategies/{id}/enabled` (`StrategyController.setEnabled` → `StrategyService.setEnabled`, body
`{"enabled": true|false}`) is the toggle endpoint — it flips the flag and calls
`StrategyScheduler.reschedule` in the same transaction, same as `create`/`update` do.

Strategy evaluation itself only reads indicator/market data and writes an `audit_logs` row (see
`StrategyEvaluationRunner` below) — it never sends anything to Polymarket. Wiring evaluation results to actual
order placement would be a write-oriented Polymarket integration and falls under the repo's Polymarket API
policy (root `CLAUDE.md`): deprecated/frozen, not to be built.

The rule tree is a small JSON DSL (`strategy/json/StrategyNodeDeserializer` + `StrategyNodeJacksonModule`) built
from `StrategyNode` subtypes:

- `BooleanNode` / `UnaryBooleanNode` — boolean composition.
- `CompareNode` — leaf comparing two `ComparisonOperand`s (`IndicatorOperand` — reads a precomputed indicator
  output, or `LiteralOperand` — a fixed threshold) via `CompareOperator` (EQ/NEQ/GT/GTE/LT/LTE on `BigDecimal`).
  Splitting the operand out of `CompareNode` is what lets either side of a comparison be an indicator *or* a
  literal — the mechanism behind "compare one indicator to another."
- `MarketFieldCompareNode` — leaf comparing a Polymarket market field (via `MarketFieldResolver`) instead of an
  indicator.

**`StrategyNode` subtypes are a deliberate, documented exception to the "DTOs are `@Value`" convention below**:
they're mutable domain objects meant to be edited in place while building a strategy, not request/response
bodies, so they use `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` without `@Builder`.

`StrategyEvaluationService` evaluates in two phases: (1) a single-threaded walk collects every distinct indicator
request the tree needs plus whether any market-field comparison exists; (2) one `CompletableFuture` per distinct
indicator (`IndicatorCalculationService.calculate`) plus at most one market-field resolution are dispatched onto
a shared bounded pool, `.join()`ed, then the tree is walked purely in-memory. Any failed indicator calc or
resolution fails the whole evaluation — fail-fast, by design.

The pool (`strategy/config/StrategyExecutorConfig`/`StrategyExecutorProperties`) is sized by
`polypilot.strategy.indicator-executor-pool-size` (default 4, env `STRATEGY_INDICATOR_EXECUTOR_POOL_SIZE`) —
**must stay well under the Hikari pool size** (default 10) because each task blocks on JPA/DB I/O.

Validation is split by concern under `strategy/service/`: `RuleTreeValidator` (structural), `IndicatorConditionValidator`,
`MarketFieldConditionValidator`, `StrategyRequestValidator`. `RuleTreeMapper` handles the JSON tree ↔ entity
column; `StrategyViewMapper` handles entity → `StrategyView`. `StrategyController` (`/strategies`) is full CRUD
(`GET /condition-fields`, `GET`/`GET /{id}`/`POST`/`PUT /{id}`/`PUT /{id}/enabled`/`DELETE /{id}`) scoped by
`@AuthenticationPrincipal UUID userId` — don't trust its class-level Javadoc, which still says "read-only" from
before create/update/delete were added.

### `strategy/scheduler/` — per-strategy cron evaluation

Deliberately **not** a single sweep over all strategies on a shared timer: `strategies.cron_expression` is
per-row (default `0 */5 * * * *`), so each enabled strategy gets its own independent trigger, following
`MarketScheduler`'s DB-driven-cron pattern (see **Market sync**) rather than reinventing one.

- **`StrategyScheduler`** — one `ScheduledFuture` per enabled, non-deleted strategy, keyed by strategy ID in a
  `ConcurrentHashMap` (not a fixed job-name enum like `MarketScheduler`, since the strategy set changes at
  runtime). `@PostConstruct init()` loads every currently-enabled strategy and registers it;
  `register`/`unregister`/`reschedule` are called from `StrategyService.create`/`update`/`delete` after
  `saveAndFlush` (not wrapped in `TransactionSynchronization.afterCommit` — a deliberate, small, accepted risk
  window rather than new transaction-sync infrastructure this codebase doesn't use elsewhere).
  `reschedule(Strategy)` is the one entry point callers use: it cancels any existing future and re-registers iff
  `enabled && deletedAt == null`, so `StrategyService` never has to diff old vs. new state itself. Runs on its
  own `ThreadPoolTaskScheduler` bean (`strategyTaskScheduler`, thread prefix `strategy-sched-`, sized by
  `polypilot.strategy.scheduler-pool-size`, default 5) — separate from `market-sync-`'s pool, since strategy
  count scales with users while the market-sync job count is fixed at three. A fired tick just calls
  `StrategyEvaluationRunner.run`, wrapped in the same try/catch-log-never-throw pattern as
  `MarketScheduler.wrapWithLogging` so one bad tick can't cancel that strategy's future runs.
- **`StrategyEvaluationRunner`** — leases the per-strategy ShedLock (see below) and delegates the actual
  per-tick unit of work to **`StrategyService.runEvaluation`**, a separate bean so its
  `@Transactional(propagation = REQUIRES_NEW)` is a real proxy hop (same isolation pattern as
  `MarketItemSyncService`/`OhlcStreamSyncService`: one strategy's failure never rolls back another's). This split
  is itself a small in-progress migration — `runEvaluation` used to live on `StrategyEvaluationRunner` directly;
  don't be surprised if older docs/comments still point there.
  `StrategyService.runEvaluation` re-reads the strategy inside that transaction and bails if it's no longer
  enabled/deleted, or if its series currently has no resolvable `OPEN` market or linked asset — schedule-time
  state is never trusted at fire-time.
  On a parseable tree, calls `StrategyEvaluationService.evaluate` and writes one `audit_logs` row
  (`action=STRATEGY_EVALUATED`) regardless of outcome — success, a rule-tree parse failure, or an evaluation
  exception all get a row, matching the roadmap's "one entry per run" rule *for evaluation itself*.
  - **Since `docs/open_trades.md` landed, a `true` result writes a second row.** When `evaluate(...)` returns
    `true`, `runEvaluation` additionally calls `OpenTradeService.openTrade(strategy, marketId)` (`trading/`
    package, `marketId` the same series-resolved id used for evaluation) to
    attempt a simulated (dry-run only — no `POLYPILOT_LIVE_MODE` exists) trade, and writes a second `audit_logs`
    row for that outcome (`action=DRY_RUN_ORDER` or `ORDER_SKIPPED`, `order_id` set when an `Order` was created).
    So "one entry per run" now means one row on `false`/parse-failure/exception, two rows on `true`. The second
    row's `is_dry_run` is hardcoded `true` (it only ever records a simulated trade), which can legitimately
    differ from the first row's `is_dry_run = strategy.getDryRun()` for the same tick. A failure while opening
    the trade is deliberately not given its own audit shape — it falls through to the same
    `catch (Exception ex)` that a bad `evaluate()` call hits, so it's recorded as a second
    `STRATEGY_EVALUATED`-shaped failure row instead, which reads a little oddly (the evaluation didn't actually
    fail) but avoids a third row shape for one uncommon failure mode.
  - **Known limitation**: the `signals` JSON on the `STRATEGY_EVALUATED` row only holds
    `marketId`/`binanceSymbol`/`result`/`error`, not the underlying indicator/market-field values —
    `StrategyEvaluationService.evaluate` only returns a `boolean`, it doesn't expose the `EvaluationContext` it
    computed. Capturing the full reasoning trail the `audit_logs` schema comment describes would mean widening
    that already-tested method's return type; flagged as a deliberate follow-up, not done here.
  - Guards against the *same* strategy's evaluation overlapping its own next tick via **ShedLock** (`shedlock`
    table, lease name `strategy-eval-<strategyId>`, `lockAtMostFor` 2 min / `lockAtLeastFor` 10 sec) — via the
    **programmatic `LockingTaskExecutor.executeWithLock(...)` API**, not the `@SchedulerLock` annotation:
    ShedLock only documents a lock name built dynamically per call (one per strategy ID) through that API, not
    through `@SchedulerLock`'s SpEL `name`. `SchedulerLockConfig` wires the `LockProvider`/`LockingTaskExecutor`
    beans against the app's `DataSource`; different strategies never contend for each other's lock, and a lease
    expiring on its own (rather than needing manual cleanup) is what keeps a crash mid-evaluation from wedging
    that strategy's future runs — the same mechanism is what would keep this safe if the orchestrator ever runs
    as more than one instance, though it's single-instance only today (`docker-compose.yaml` has no
    `deploy.replicas`).

## Error handling

`GlobalExceptionHandler` (`common/exception/`, `@RestControllerAdvice`) is the current, evolving convention:
handlers are ordered most-specific first, every response body is one `ErrorResponse{message}` shape, and an
exception already caught and converted locally (e.g. `SiweService.parse` catching `IllegalArgumentException` for
a non-revealing 401) never reaches it. **This does not mean every error needs a custom exception type**:
`ResponseStatusException` with an explicit `HttpStatus` remains the normal way for service code to signal a 4xx
(`IndicatorCalculationService`, `StrategyEvaluationService`, `SiweService`, etc. all still do this) — the handler
just renders it centrally instead of each controller doing so by hand. Reach for a dedicated exception class
(like `EncryptionException`) only for failures that need their own distinct handling/status, not as a blanket
replacement for `ResponseStatusException`.

## Schema is SQL-first, JPA is validate-only

`spring.jpa.hibernate.ddl-auto=validate` — Hibernate never creates or alters tables. The schema lives in
`orchestrator/src/main/resources/sql/001_schema.sql`, run by `spring.sql.init` on every startup, followed by
`002_seed_data.sql` (default admin, tracked BTC/ETH series, job cron rows, ticker/timeframe/indicator catalog
rows).

**Any entity change must be paired with a hand-edit to `001_schema.sql`, or the app fails to boot on validate.**
Migrations are raw numbered SQL; Flyway is planned but not wired up. Seed data is idempotent
(`ON CONFLICT DO NOTHING`) because it re-runs on every boot.

The schema has 23 tables. Live-code tables today: `roles`, `users`, `user_identities`, `wallet_credentials`,
`job_schedule`, `shedlock`, `ticker`, `timeframe`, `series`, `markets`, `price_snapshots`, `strategies`,
`indicators`, `universal_parameters`, `indicator_parameters`, `indicator_outputs`, `ohlc_candles`, `audit_logs`
(written by `StrategyService.runEvaluation`, one or two `audit_logs` rows per scheduled evaluation — see
**`strategy/scheduler/`**), `orders`, `positions` (both written by `OpenTradeService`, `trading/` package —
simulated/dry-run trades only, see **`strategy/scheduler/`** and `docs/open_trades.md`), `sentiment_scores`,
`news_summaries` (written by `ai/listener/AiSignalListener` off the `ai.signals` queue — see **`ai/`** below and
`docs/news_summary.MD`). `shedlock` is ShedLock's standard JDBC schema
(`name`/`lock_until`/`locked_at`/`locked_by`), not a custom table — column names/types must match what
`shedlock-provider-jdbc-template` expects. Still-unwired (entity + repository exist, no service code touches
them yet): `alerts`.

## `ai/` — news summary + sentiment pipeline

Wires `ai-agent`'s news/sentiment pipeline into the orchestrator's DB and dashboard. Full design:
`docs/news_summary.MD`.

- **Trigger**: `MarketScheduler`'s fourth job, `news-sync` (every 15 minutes, matching GDELT's own update
  cadence) — `NewsSyncService.syncAll()` iterates every OPEN market and delegates to
  `NewsSyncItemService.syncOne(marketId)` (`@Transactional(REQUIRES_NEW)`, same per-item isolation as
  `MarketItemSyncService`), which skips if `news_summaries.expires_at` is still in the future for that market
  (the cost-control gate — see the schema comment on that column), else calls `AiAgentClient.analyze(...)`
  (HTTP/1.1-pinned `RestClient`, same reason as `AuthServiceClient`) to fire-and-forget trigger ai-agent. This
  method never itself writes `news_summaries`/`sentiment_scores` — see below.
- **Consumer**: `AiSignalListener` (`@RabbitListener` on `ai.signals`, declared by `RabbitConfig`) writes one
  `sentiment_scores` row and one `news_summaries` row per message — the two tables are populated together from
  a single ai-agent run, matching the schema's own "same LangGraph run" comment. A message that fails
  processing is retried a bounded number of times, then dead-lettered to `ai.signals.dlq`
  (`RabbitConfig`'s retry interceptor) rather than crashing the listener or looping forever. **Not** written:
  an `audit_logs` row — `audit_logs.user_id` is `NOT NULL` and every existing write derives it from a
  strategy/position's owning user, but `sentiment_scores`/`news_summaries` are market-scoped with no single
  owning user (see the comment in `AiSignalListener.onSignal`).
- **Read API**: `GET /market/{id}/news/latest` and `/sentiment/latest` on `MarketController`, backed by
  `NewsQueryService`/`SentimentQueryService` (thin repository fan-in, same shape as `SeriesQueryService`). Both
  `204 No Content` when no row exists yet. `MarketSentimentView.score` (a signed -1..1 scalar) always
  serializes `null` — this pipeline's Claude call produces a label + confidence only, no separate signed score.
- **Strategy engine**: `MarketFieldResolver` already reads `sentiment_scores` (`SENTIMENT`/
  `SENTIMENT_CONFIDENCE` fields) — landing this feature made sentiment-gated strategies functional with zero
  strategy-engine changes.

## Async boundary

Order-critical calls are synchronous HTTP (`RestClient`) because the caller can't proceed without the result.
AI sentiment is fire-and-forget over RabbitMQ (`ai.signals`) — the orchestrator triggers `POST /ai/analyze` and
picks up the result from the queue later (see **`ai/`** above). This split is deliberate (ADR-003 in the
roadmap); keep new code on the correct side of it.

## Conventions

- **No Java `record`s.** Every value/DTO type — request bodies, response bodies, internal tuples — is a
  standalone top-level class in its own file, made immutable with Lombok `@Value` (bean-style `getX()`
  accessors). Never nest it inside a controller/service. `lombok.config` sets
  `lombok.anyConstructor.addConstructorProperties = true` so Jackson binds request bodies by field name.
  `strategy/`'s `StrategyNode` subtypes are the one documented exception (see **`strategy/`** above) — if another
  case genuinely needs a `record` or a different shape, stop and flag it for review rather than adding it.
- Lombok `@AllArgsConstructor` on `@Service`/`@Component` classes for constructor injection; a few classes use
  explicit constructors instead. Either is fine, don't churn existing files.
- `@Slf4j` + parameterized logging with bracketed identifiers: `log.error("Failed to sync series [{}]", id, ex)`.
- Enums are grouped in subpackages by domain (`enums/market/`, `enums/order/`, `enums/alert/`) and persisted as
  `@Enumerated(EnumType.STRING)`.
- Money and prices are `BigDecimal` (`precision=6, scale=4` for prices, `18,4` for volume/liquidity);
  timestamps are `OffsetDateTime` with `hibernate.jdbc.time_zone=UTC`, except the newer wallet/SIWE code which
  uses `Instant`.

## Tests

`orchestrator/src/test/java` exists and is substantial (auth, siwe, wallet, market, indicator, strategy). The
pattern throughout is plain JUnit 5 + AssertJ + Mockito unit tests — no `@SpringBootTest`, no Testcontainers,
nothing boots a Spring context or touches a real database. Where a controller needs `MockMvc`, it's
**`MockMvcBuilders.standaloneSetup(...)`**, not a `@WebMvcTest` slice — match this if you add a new controller
test. `pom.xml` has no custom Surefire config (Spring Boot parent's defaults apply), so
`mvn test -Dtest=ClassName#method` works as-is.
