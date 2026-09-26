# Demo mode

`VITE_DEMO=true` (Vite `--mode demo`) swaps the dashboard's backend for [MSW](https://mswjs.io/) handlers
answering from typed, static fixtures — no orchestrator, no `auth-service`, no external network calls. See the
root plan for the full design; this file is the living endpoint inventory and a short "how it works".

**Real code imports nothing from `src/demo` except the `main.tsx` bootstrap** (`await import('./demo/start')`,
gated on `import.meta.env.VITE_DEMO === 'true'`). Whenever you add or change an API call in `src/features/*Api.ts`,
update this table, its fixture and its handler in the same change — the Playwright crawl test
(`e2e/`) fails on any unmocked `/api/*` request.

## How it works

1. `main.tsx` dynamically imports `demo/start.ts` only when `VITE_DEMO === 'true'` (a literal check, so Vite
   dead-code-eliminates the whole chain — including MSW itself — out of real builds).
2. `start.ts` starts an MSW browser worker (`msw/browser`) with one handler module per API domain
   (`handlers/*.ts`), each returning fixtures from `fixtures/*.ts` typed against the dashboard's real DTOs
   (imported from `src/features/*/types.ts` — never re-declared).
3. A catch-all handler (`handlers/index.ts`, registered last) answers any unmatched `*/api/*` request with `501`
   and logs `[demo] UNMOCKED`, so a gap is loud, not a silent real network call.
4. Strategies are the only stateful domain — `db.ts` holds an in-memory `Map`, reseeded on every load (Phase 6).
   Everything else is static fixture data.
5. `scripts/check-bundle.mjs` asserts the demo marker string and `mockServiceWorker.js` are present in a demo
   `dist/` and absent from a real one.

## Endpoint inventory

Every fetching function in `src/features/**/*Api.ts` is listed below. Functions the dashboard defines but never
calls from a page are still included and mocked (see the "Anything else" note underneath).

| Method | Browser URL | API function | Request body | Response type | Status codes handled | Handler file |
|---|---|---|---|---|---|---|
| POST | `/api/auth/login` | `authApi.login` | `{ email, password }` | `LoginResponse` | non-OK → error | `handlers/auth.ts` |
| GET | `/api/auth/siwe/nonce` | `authApi.getSiweNonce` | — | `{ nonce: string }` | non-OK → error | `handlers/auth.ts` |
| POST | `/api/auth/siwe/verify` | `authApi.siweLogin` | `{ message, signature }` | `LoginResponse` | non-OK → error | `handlers/auth.ts` |
| GET | `/api/market/series` | `seriesApi.fetchSeriesList` | — | `SeriesSummary[]` | non-OK → error | `handlers/market.ts` |
| GET | `/api/market/series/:id` | `seriesApi.fetchSeriesDetail` | — | `SeriesDetail` | non-OK → error | `handlers/market.ts` |
| GET | `/api/market/:marketId/price-history?window=` | `marketApi.fetchMarketPriceHistory` | — | `MarketPriceHistory` | non-OK → error | `handlers/market.ts` |
| GET | `/api/market/:marketId/ohlc?timeframe=&limit=` | `marketApi.fetchMarketOhlc` | — | `MarketOhlcHistory` | non-OK → error | `handlers/market.ts` |
| GET | `/api/reference/timeframes` | `marketApi.fetchTimeframes` | — | `TimeframeOption[]` | non-OK → error | `handlers/market.ts` |
| GET | `/api/market/:marketId/orders?limit=` | `marketApi.fetchMarketOrders` | — | `MarketOrdersResponse` | non-OK → error | `handlers/market.ts` |
| GET | `/api/market/:marketId/news/latest` | `marketApi.fetchLatestNews` | — | `MarketNewsSummary \| undefined` | **204 → undefined**, non-OK → error | `handlers/market.ts` |
| GET | `/api/market/:marketId/sentiment/latest` | `marketApi.fetchLatestSentiment` | — | `MarketSentiment \| undefined` | **204 → undefined**, non-OK → error | `handlers/market.ts` |
| GET | `/api/market/:marketId/decisions?limit=` | `auditApi.fetchMarketDecisions` | — | `MarketDecisionsResponse` | non-OK → error | `handlers/market.ts` |
| GET | `/api/indicators` | `indicatorApi.fetchIndicatorCatalog` | — | `IndicatorCatalogView` | non-OK → error | `handlers/indicators.ts` |
| GET | `/api/strategies/condition-fields` | `strategiesApi.fetchConditionFields` | — | `ConditionFieldsCatalog` | non-OK → error | `handlers/strategies.ts` |
| GET | `/api/strategies` | `strategiesApi.fetchStrategies` | — | `StrategyView[]` | non-OK → error | `handlers/strategies.ts` |
| GET | `/api/strategies/:id` | `strategiesApi.fetchStrategy` | — | `StrategyView` | **404** unknown id, non-OK → error | `handlers/strategies.ts` |
| POST | `/api/strategies` | `strategiesApi.createStrategy` | `CreateStrategyRequest` | `StrategyView` | non-OK → error | `handlers/strategies.ts` |
| PUT | `/api/strategies/:id` | `strategiesApi.updateStrategy` | `UpdateStrategyRequest` | `StrategyView` | **404** unknown id, non-OK → error | `handlers/strategies.ts` |
| DELETE | `/api/strategies/:id` | `strategiesApi.deleteStrategy` | — | `void` (204) | **404** unknown id, non-OK → error | `handlers/strategies.ts` |
| PUT | `/api/strategies/:id/enabled` | `strategiesApi.setStrategyEnabled` | `{ enabled }` | `StrategyView` | **404** unknown id, non-OK → error | `handlers/strategies.ts` |
| GET | `/api/strategies/:id/orders?limit=` | `strategiesApi.fetchStrategyOrders` | — | `StrategyOrderRow[]` | non-OK → error | `handlers/strategies.ts` |
| GET | `/api/strategies/:id/decisions?limit=` | `auditApi.fetchStrategyDecisions` | — | `StrategyDecision[]` | non-OK → error | `handlers/strategies.ts` |
| GET | `/api/portfolio/summary` | `portfolioApi.fetchPortfolioSummary` | — | `PortfolioSummary` | non-OK → error | `handlers/portfolio.ts` |
| GET | `/api/positions?limit=` | `positionsApi.fetchOpenPositions` | — | `PositionView[]` | non-OK → error | `handlers/positions.ts` |
| GET | `/api/audit-logs?size=` | `auditApi.fetchAuditLogs` | — | `AuditLogPage` | non-OK → error | `handlers/audit.ts` |
| GET | `/api/health` | `healthApi.fetchHealth` | — | `HealthResponse` | non-OK → error | `handlers/health.ts` |
| GET | `/api/health/checks` | `healthApi.fetchHealthChecks` | — | `ChecksResponse` | non-OK → error | `handlers/health.ts` |
| GET | `/api/health/infrastructure` | `healthApi.fetchInfrastructure` | — | `InfrastructureResponse` | non-OK → error | `handlers/health.ts` |

**Anything else.** A full grep of `src` for `fetch(`/`axios`/`XMLHttpRequest`/`EventSource`/`WebSocket` (Phase 0)
turned up only the calls above, all funneled through `src/lib/http.ts` or `authApi.ts`'s local `fetch`. The
plan's snapshot backend surface additionally lists `GET /api/auth/me`, `GET /api/admin/users`,
`POST /api/wallet/connect`, `GET/POST /schedule/config` and a manual `POST /market` sync — **none of these are
called anywhere in the dashboard source** (confirmed by grep), so per the plan's rule 5 ("code is ground truth")
they are not mocked. `ConnectWalletButton.tsx` (wagmi connect/disconnect, no login) is also dead code — not
imported by any page or component — so it needs no demo replacement either.

## Catch-all

`handlers/index.ts` registers every domain handler above, then a final `http.all('*/api/*', …)` that returns
`501 { detail: 'not mocked in demo' }`, logs `console.error('[demo] UNMOCKED', method, url)`, and records the
request on `window.__demoUnhandled` for the e2e test. Non-`/api/` requests bypass MSW (`onUnhandledRequest:
'bypass'`), which is what lets fonts/assets load normally.
