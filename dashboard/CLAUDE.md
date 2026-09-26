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
- No lint config and no test runner are set up yet (the demo build has its own Playwright suite —
  see "Demo mode" below).

## Demo mode

A `VITE_DEMO=true` build (`--mode demo`) runs with no backend: every `/api/*` call is answered in-browser by
[MSW](https://mswjs.io/) from `src/demo/`. See `src/demo/README.md` for the full endpoint inventory and design,
and the root README's [Live demo](../README.md#live-demo) section for what's simulated. Summary of the rules:

- Commands: `npm run dev:demo` (5173, demo mode), `npm run build:demo` (→ `dist/`, plus a `404.html` copy for
  GitHub Pages SPA fallback), `npm run preview:demo`, `npm run test:e2e` (builds demo, then runs the Playwright
  crawl in `e2e/`). All accept `BASE_PATH=/x/` for a non-root deploy path.
- **Real code imports nothing from `src/demo` except the `main.tsx` bootstrap** (a dynamic `import('./demo/start')`
  gated on `import.meta.env.VITE_DEMO === 'true'`, so Vite dead-code-eliminates it from real builds). A handful of
  real modules (the wallet login panel, the password form, `wagmiConfig.ts`) are swapped for demo-only
  replacements via `vite.config.ts`'s `resolve.alias`, active only in demo mode — the real files themselves are
  untouched and still ship as-is in `npm run build`.
- `scripts/check-bundle.mjs` enforces the split: it fails if a real `dist/` (`build` / `npm run build`) contains
  the demo marker or MSW's `mockServiceWorker.js`, and fails if a demo `dist/` (`build:demo`) is missing either.
  Wired into `ci.yml` (real build) and `demo.yml` (demo build).
- **When you add or change any API call** (`src/features/*/*.ts`), **update the inventory table in
  `src/demo/README.md`, its fixture (`src/demo/fixtures/`) and its handler (`src/demo/handlers/`) in the same
  change** — the Playwright crawl test fails loudly on any unmocked `/api/*` request (`handlers/index.ts`'s
  catch-all returns 501 and logs `[demo] UNMOCKED`), so a gap doesn't go unnoticed, but it also doesn't fix
  itself.
- Strategies are the only stateful demo domain (`src/demo/db.ts`, an in-memory `Map` that resets on every page
  load); everything else is static fixture data.

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
