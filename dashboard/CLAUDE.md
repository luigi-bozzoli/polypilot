# CLAUDE.md — dashboard

## What this is

The `dashboard/` service is the React 18 / TS / Vite frontend for PolyPilot. See the repo-root `/CLAUDE.md` for
the overall monorepo architecture (orchestrator, auth-service, ai-agent) — this file only covers what's specific
to the dashboard.

The app is a routed SPA (`react-router-dom`, `BrowserRouter` in `main.tsx`) with a login gate: `/` renders
`AuthPage` (SIWE wallet login via `SiweLoginPanel`, plus password login via `LoginForm` — see
[root `CLAUDE.md`](../CLAUDE.md) for which is the primary path), everything else sits behind `ProtectedRoute` +
`AppShell` (`src/App.tsx` is the route table). Pages under `src/pages/`: `OverviewPage`, `HealthPage`,
`SeriesListPage`/`SeriesDetailPage`, `MarketDetailPage`, `StrategiesListPage`/`StrategyDetailPage`/
`CreateStrategyPage`/`EditStrategyPage`.

Code is feature-sliced, mirroring the orchestrator's own newer style:

- `src/features/<domain>/` — API clients (thin wrappers over `lib/http.ts`), TanStack Query hooks, and
  domain types. Domains: `auth`, `health`, `market`, `series`, `strategies`, `shell`, `wallet`.
- `src/components/<domain>/` — presentational components grouped the same way, plus `shell/` (nav chrome) and
  `auth/` (login form pieces).
- `src/lib/http.ts` — `authGet`/`authPost`/`authPut`/`authDelete`: every authenticated call attaches the JWT
  from `features/auth/session.ts` as a bearer token and normalizes error bodies (`detail` ?? `message`) into a
  single `Error` for React Query to surface. Use these instead of bare `fetch` for anything behind login.
- `features/wallet/wagmiConfig.ts` — wagmi config for the SIWE flow; `ALLOWED_CHAIN_IDS` (mainnet + Polygon)
  **must stay in lockstep with the orchestrator's `siwe.allowed-chain-ids`** (`SIWE_ALLOWED_CHAIN_IDS` env var) —
  a mismatch means valid signatures get rejected by `POST /api/auth/siwe/verify`.

## `mocks/`

`mocks/` holds static, standalone HTML/CSS mockups (dark theme, IBM Plex Mono + DM Sans), numbered `01`–`23`.
Some now describe screens already built for real in `src/pages/` (`09-login`, `10-overview`, `11`/`12-series-*`,
`13-market-detail`, `14`/`15-strategy(-detail)`, `21-health`) — treat those as historical design references, not
current specs; the live component is the source of truth once a screen ships. The rest are still ahead of the
code (market-filter, notifications, audit-log, scheduler, alerts, settings) — for those, the mock file's CSS
custom properties and layout are still the intended visual spec when the real screen gets built.

**place-order, confirm-live-trading, close-position/positions-list, orders-list, order-detail —
DEPRECATED/FROZEN**: these mocks describe UI for placing/closing orders and viewing live positions, which
depends on the order-placement pipeline (`orchestrator`'s strategy→order wiring, `auth-service`'s ClobAuth
relay) that is deprecated/frozen under the repo's Polymarket API policy (root `CLAUDE.md`) because it sends
data to Polymarket. Do not build real screens from these mocks.

## Running

From the repo root: `docker compose up --build` (bind-mounted, Vite HMR — edits apply live, no rebuild needed).
Standalone: `npm install && npm run dev` (serves on 5173).

- `npm run build` — runs `tsc` (noEmit type-check) then `vite build`
- `npm run preview` — preview a production build
- No lint config and no test runner are set up yet.

## Architecture notes

- **API calls go through `/api/*`, never directly to `orchestrator:8080`** — but the proxy rewrite is
  path-dependent, not uniform. `vite.config.ts` forwards `/api/auth`, `/api/admin`, `/api/wallet` to the
  orchestrator **with the `/api` prefix kept** (those controllers bake `/api` into their own
  `@RequestMapping`), while every other `/api/*` call has the prefix **stripped** before forwarding. If you add a
  new orchestrator controller, match its `@RequestMapping` convention to whichever bucket it belongs in, or the
  proxy rule needs a new entry.
- The dev server binds `0.0.0.0:5173` with `hmr.clientPort: 5173` explicitly set — required for HMR to work
  through the Docker port mapping. Don't change the host/port without updating `docker-compose.yaml` too.
- `tsconfig.json` has `strict: true` and no `src` files are excluded — keep new code type-clean rather than
  reaching for `any`.
