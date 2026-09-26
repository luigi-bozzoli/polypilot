# News Summary + Sentiment — Flow Explanation

This document explains, step by step, how a market's news summary and sentiment score get
produced and surfaced in the dashboard. It covers the full path: scheduler → orchestrator →
ai-agent → RabbitMQ → orchestrator (again) → Postgres → REST API → dashboard.

There are two independent flows that together make up this feature:

1. **The trigger + generate flow** (orchestrator asks ai-agent to analyze a market).
2. **The publish + persist flow** (ai-agent publishes a result, orchestrator consumes and stores it).

They are connected only by RabbitMQ — this is the "fire-and-forget" async boundary documented in
the root `CLAUDE.md` (ADR-003). The orchestrator never blocks waiting for the AI result.

---

## 1. What triggers analysis: the `news-sync` scheduled job

`orchestrator`'s `MarketScheduler` already ran three DB-driven cron jobs (market sync, indicator
calc, strategy evaluation) before this feature. A fourth was added: **`news-sync`**.

- Schedule is stored as a row in the `job_schedule` table, seeded in
  `orchestrator/src/main/resources/sql/002_seed_data.sql`:
  `('news-sync', '0 */15 * * * *', now())` — every 15 minutes.
- This cadence matches GDELT's own update frequency (~15 min), so polling faster would just
  return the same articles.
- `MarketScheduler` reads this row at startup (and on reschedule via
  `ScheduleController` → `PUT /schedule/{job}`) and registers a `ScheduledFuture` via
  `ThreadPoolTaskScheduler`, exactly like the other three jobs.
- Every tick, the scheduler calls `NewsSyncService.syncAll()`.

### `NewsSyncService.syncAll()`

- Queries `marketRepository.findAllByStatus(MarketStatus.OPEN)` — only open (actively tradeable)
  markets get news/sentiment refreshed. Closed/resolved markets are frozen and don't need it.
- Iterates each market and calls `NewsSyncItemService.syncOne(marketId)` **per market**, wrapped
  in try/catch so one market's failure (bad data, ai-agent timeout, etc.) doesn't abort the rest
  of the batch — logged and skipped.

### `NewsSyncItemService.syncOne(String marketId)`

This is a `@Service` bean with `@Transactional(propagation = Propagation.REQUIRES_NEW)` on
`syncOne` — same per-item transactional isolation pattern as `MarketItemSyncService`. Each market
gets its own transaction so one bad row can't roll back the whole batch.

Steps inside:

1. **Re-fetch the market by ID** inside this new transaction (does *not* accept a `Market` entity
   as a parameter). This matters: if a `Market` entity loaded in `syncAll()`'s transaction were
   passed in and its `series.asset` accessed here, it would throw a `LazyInitializationException`
   because the original persistence context is gone. Re-fetching fresh avoids this — mirrors
   `MarketItemSyncService.refreshOpenMarket`'s established pattern.
2. **Freshness check**: calls `newsSummaryRepository.findFirstByMarketIdAndExpiresAtAfterOrderByFetchedAtDesc(...)`.
   If a non-expired summary already exists (TTL = 15 min, matching the job cadence), skip —
   no point re-triggering analysis for data that hasn't gone stale yet.
3. **Asset check**: if `market.series.asset` is null, log a warning and skip. Not every market is
   necessarily linked to a tracked asset with a display name/symbol, and GDELT search needs those.
4. **Trigger the call**: builds an `AnalyzeRequest` (marketId, asset symbol, asset display name,
   market question) and calls `AiAgentClient.analyze(request)`.
5. Wraps the call in try/catch for `RestClientException` — if ai-agent is down or errors, this
   market is just skipped this cycle; the next `news-sync` tick will retry it.

### `AiAgentClient` (orchestrator → ai-agent, synchronous HTTP)

- A `RestClient` pinned to HTTP/1.1 via a raw JDK `HttpClient` — same fix used for
  `AuthServiceClient`: the JDK client's default h2c (HTTP/2 cleartext) upgrade attempt silently
  drops the POST body when talking to uvicorn, so HTTP/1.1 is forced explicitly.
- `POST /ai/analyze` with the `AnalyzeRequest` body, expects an `AnalyzeAccepted` response.
- **This call is synchronous but cheap** — it only waits for ai-agent to accept and schedule the
  work, not for the actual analysis to finish. This is why it's still compatible with the
  "sentiment is fire-and-forget" rule: the *slow* LLM/GDELT work happens after this HTTP call
  already returned.

---

## 2. ai-agent: the actual analysis pipeline

### `POST /ai/analyze` (`app/api/analyze.py`)

- Thin route handler: parses `AnalyzeRequest`, hands off to `AnalyzeService.analyze(request, background_tasks)`,
  returns `AnalyzeAccepted` (HTTP 202-style semantics) immediately.
- `AnalyzeService` schedules `_run_pipeline(...)` as a FastAPI `BackgroundTasks` job — this is the
  actual fire-and-forget mechanism. FastAPI runs it after the HTTP response has already been sent.
  No Celery/task-queue infra exists in this repo; `BackgroundTasks` is the documented, deliberate
  choice for this volume of work.

### `_run_pipeline` — three stages, each fault-tolerant independently

The pipeline is written so that **a failure at any stage still produces a publishable result** —
it degrades gracefully rather than dropping the market entirely.

**Stage 1 — `news_fetcher.fetch_articles(settings, asset_symbol, asset_display_name)`**

- Calls GDELT DOC 2.0 (`https://api.gdeltproject.org/api/v2/doc/doc`), a free, unrestricted
  commercial-use news search API — no API key needed, ~15 min data freshness.
- Async `httpx` client with retry/backoff.
- Searches recent articles (bounded by `news_lookback_hours`, capped at `news_max_articles`)
  matching the asset's symbol/display name.
- **Any error here is swallowed and returns `[]`** — an empty article list, not an exception. A
  market with genuinely no recent news coverage is a normal, valid outcome, not a failure.

**Stage 2 — `analyzer.analyze(settings, market_question, asset_display_name, articles)`**

- Uses `langchain-anthropic`'s `ChatAnthropic(...).with_structured_output(AnalysisResult, include_raw=True)`
  — a single Claude call that returns both the news summary text and the sentiment score/label in
  one structured response, plus the verbatim raw model response (kept for the `raw_response` JSONB
  column, useful for debugging/audit later).
- **Prompt-injection defense**: the system prompt explicitly instructs Claude to treat all fetched
  article text as *untrusted data* to summarize/analyze, never as instructions to follow. Article
  content is sanitized/wrapped before being interpolated into the prompt.
  This matters because GDELT article titles/snippets are attacker-influenceable (anyone can publish
  a web page) — without this, a malicious article title could attempt to hijack the analysis.
- **Low-signal handling**: if very few articles were found, the prompt instructs Claude to weight
  its confidence down accordingly rather than overstating certainty from thin data.
- If `articles` is empty (from Stage 1 swallowing an error, or genuinely no news), the model still
  runs — it can legitimately report "no significant news coverage" with low/neutral confidence.

**Stage 3 — `publisher.publish_signal(settings, message: AiSignalMessage)`**

- Opens a **fresh `pika.BlockingConnection` per call** — no connection pooling. This is a
  deliberate simplicity choice: message volume is low (bounded by number of open markets × once
  per 15 min), so the overhead of a new connection per publish is negligible and avoids managing
  pool lifecycle/reconnection logic.
- Publishes to the durable `ai.signals` queue with `delivery_mode=2` (persistent) so messages
  survive a RabbitMQ restart.
- The message (`AiSignalMessage`) carries: `market_id`, the news summary text, source article
  metadata (title/url/domain/seendate per article), sentiment score, sentiment label, confidence,
  and the raw Claude response.

Each of the three stages is wrapped so an exception in Stage 1 or Stage 2 doesn't prevent Stage 3
from still publishing *something* — a degraded-but-present result (e.g. empty articles + a
low-confidence neutral sentiment) is always better than silence, since the orchestrator's
freshness check (`expiresAt`) means a totally missing result just means the market looks
perpetually stale in the dashboard until the next successful cycle.

---

## 3. Back in orchestrator: consuming the result

### `RabbitConfig` — queue topology

Declares everything needed to safely consume `ai.signals`:

- **`ai.signals`** — the durable primary queue, with `x-dead-letter-exchange` /
  `x-dead-letter-routing-key` arguments pointing at a dead-letter exchange, so a message that
  exhausts its retries doesn't just vanish or block the queue.
- **`ai.signals.dlx`** — a `DirectExchange` dead-letter exchange.
- **`ai.signals.dlq`** — the dead-letter queue itself, bound to the DLX via `BindingBuilder`.
- **`JacksonJsonMessageConverter`** — deserializes the JSON message body into `AiSignalMessage`.
  (Chosen over the older `Jackson2JsonMessageConverter`, which is deprecated in this Spring AMQP
  version — found during implementation via jar inspection, not a functional requirement, just
  avoiding a deprecation warning.)
- **Bounded retry**: a `SimpleRabbitListenerContainerFactory` with an advice chain built from
  `RetryInterceptorBuilder.stateless().maxRetries(3).recoverer(new RejectAndDontRequeueRecoverer())`.
  If the listener throws (e.g. malformed message, DB constraint violation) it retries in-process up
  to 3 times, then rejects without requeueing — RabbitMQ then routes it to `ai.signals.dlq` via the
  primary queue's dead-letter arguments, rather than retrying forever or crashing the listener
  thread.

Note: in Spring AMQP 4.1.0 (Spring Boot 4.1 / Spring 7), `RetryInterceptorBuilder` lives in
`org.springframework.amqp.rabbit.config` (moved from `.retry` in older versions) and uses Spring
7's own `org.springframework.core.retry.RetryPolicy` internally — exposed through the simple
`.maxRetries(int)` builder method shown above. No `spring-retry` dependency is needed for this.

### `AiSignalListener` — `@RabbitListener` on `ai.signals`

For each consumed `AiSignalMessage`:

1. Writes a **`sentiment_scores`** row (score, label, confidence, `raw_response` as JSONB via
   `@JdbcTypeCode(SqlTypes.JSON)` on a `Map<String, Object>` field on `SentimentScore`).
2. Writes a **`news_summaries`** row (summary text, source articles, `fetchedAt = now()`,
   `expiresAt = fetchedAt + 15m` — the same TTL the freshness check in `NewsSyncItemService` reads
   back).
3. **Deliberately does *not* write an `audit_logs` row.** This was considered during
   implementation and rejected: `audit_logs.user_id` is `NOT NULL`, and this event is
   market-scoped, not user-scoped — there's no single correct user to attribute it to. Even a
   placeholder value would make the row invisible anyway, since every existing audit query filters
   by `userId`. Instead, a `log.info(...)` call records the event for operational visibility.

If any of this throws, the retry/DLQ mechanics from `RabbitConfig` take over.

---

## 4. Reading the data: REST API

Two new read-only endpoints on `MarketController`, backed by thin query services:

- **`GET /market/{marketId}/news/latest`** → `NewsQueryService` → returns the most recent
  non-expired `news_summaries` row for the market as a `MarketNewsSummaryView`, or **`204 No
  Content`** if none exists (e.g. market just opened, `news-sync` hasn't reached it yet).
- **`GET /market/{marketId}/sentiment/latest`** → `SentimentQueryService` → same pattern, returns
  `MarketSentimentView` or `204`.

Both view DTOs (`MarketNewsSummaryView`, `MarketSentimentView`, `NewsSourceView`) are shaped to
match `contracts/market-news-summary.md` / `contracts/market-sentiment.md` field-for-field — these
contracts predate the implementation and were treated as the source of truth for response shape.

---

## 5. Dashboard: displaying it

- `marketApi.ts` — `fetchLatestNews` / `fetchLatestSentiment`, using a new `authGetOptional<T>`
  helper in `lib/http.ts` that treats a `204` response as `undefined` (distinct from the existing
  `authGet`/`authPost`/etc., which always expect a body).
- `useLatestNews` / `useLatestSentiment` — TanStack Query hooks with `refetchInterval` set to 15
  minutes, matching the producer's actual cadence (deliberately much slower than, e.g., the 15s
  interval used for live price history — polling faster than the data can change would be wasted
  requests).
- `NewsSummaryCard` / `LatestSentimentCard` — render loading / error / empty (`undefined` from the
  204 case) / success states. `Empty` and `ErrorState` are new shared primitives in
  `components/market/primitives.tsx`.

---

## 6. End-to-end timing example

1. `t=0`: `news-sync` cron tick fires. Market `M` is OPEN, its last summary expired 3 minutes ago.
2. `t=0+ε`: `NewsSyncItemService` re-fetches `M`, confirms staleness, calls ai-agent synchronously
   — this HTTP call returns almost immediately with `AnalyzeAccepted` once ai-agent has scheduled
   the background task.
3. `t=0+ε` to `t=~5s`: ai-agent's background task runs — GDELT fetch, Claude call, publish to
   `ai.signals`. The orchestrator's scheduler thread is already free to move to the next market;
   it does not wait for this.
4. `t=~5s`: `AiSignalListener` consumes the message, writes `sentiment_scores` + `news_summaries`
   rows, `expiresAt` set to `t=~5s + 15m`.
5. Any dashboard client polling `GET /market/M/news/latest` after `t=~5s` sees the fresh data; a
   client polling before that still sees the previous (or no) summary until its own next refetch.
6. At `t=15m` from step 1, the cron fires again, finds the summary from step 4 still fresh
   (`expiresAt` in the future), and skips `M` for that cycle — no wasted ai-agent/Claude call.

---

## 7. Why two separate transports (HTTP then RabbitMQ) instead of one

This is the orchestrator's existing **async boundary** convention (documented in the root
`CLAUDE.md`, ADR-003): synchronous HTTP is reserved for calls where the caller genuinely cannot
proceed without the result (order-critical paths). AI sentiment analysis is not order-critical and
can take several seconds (GDELT + an LLM call) — blocking the scheduler thread for that long, once
per market, would make the `news-sync` job's wall-clock time balloon with market count. Splitting
it into "trigger" (fast, sync) and "result" (slow, async via queue) keeps the scheduler responsive
regardless of how long any individual analysis takes, and means a slow/failed ai-agent run affects
only that one market's data freshness, not the whole sync cycle.
