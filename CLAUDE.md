# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

PolyPilot is an automated trading bot for [Polymarket](https://polymarket.com) prediction markets, built as a
polyglot monorepo of four services. It syncs (read-only) market data from Polymarket and runs LLM-driven
sentiment analysis — with a global `POLYPILOT_LIVE_MODE` / per-strategy `dry_run` kill switch.

**Polymarket API policy — read-only.** Calls that *read* from Polymarket (Gamma market/series data) are the
normal, supported way this project gets market data and remain fully in use. Any integration that *sends* data
to Polymarket — placing/cancelling orders, or deriving L2 trading credentials via the ClobAuth relay — is
**deprecated/frozen**: the code exists (see `auth-service/CLAUDE.md` and the `wallet/` section of
`orchestrator/CLAUDE.md`) but must not be extended, exercised, or relied upon, and no new write-oriented
Polymarket integration may be added anywhere in this repo. Where this file previously described "(eventually)
places orders" as a goal, that goal is on hold under this policy.

The project is mid-build. `/docs` is gitignored, so any local planning notes (roadmap, API guides) live there
and aren't checked into this repo.

## Services

| Dir | Stack | Port | Role |
|---|---|---|---|
| `orchestrator/` | Java 21, Spring Boot 4.1 | 8080 | Owns the DB, auth, scheduling, market sync, indicator calc, strategy evaluation |
| `auth-service/` | Python 3.12, FastAPI, web3.py | 8001 | **Only** service that touches private keys: EIP-712/SIWE signature recovery; L1→L2 credential derivation is **deprecated/frozen** (see below) |
| `ai-agent/` | Python 3.12, FastAPI, LangGraph | 8002 | News fetch → summarize → sentiment score, published to RabbitMQ |
| `dashboard/` | React 18, TS, Vite | 5173 | UI; proxies `/api/*` → `orchestrator:8080` |

Infra: Postgres 16, Redis 7, RabbitMQ 3 — all on the `polypilot-net` bridge network.

Each service that has grown enough to need it has its own `CLAUDE.md` — `orchestrator/CLAUDE.md`,
`auth-service/CLAUDE.md`, `ai-agent/CLAUDE.md`, `dashboard/CLAUDE.md` — with the architecture, conventions, and
gotchas specific to it. This file covers only what's shared across the monorepo.

The dashboard also has a backend-less **demo build** (GitHub Pages, no orchestrator/auth-service/ai-agent
involved) — see `dashboard/CLAUDE.md`'s "Demo mode" section and `dashboard/src/demo/README.md`.

`ai-agent` is no longer a stub: `POST /ai/analyze` runs the full news → summarize/score → publish pipeline
(GDELT fetch, one Claude call, RabbitMQ publish), not just `/health`. `auth-service` is no longer a stub for
read-side auth either — SIWE signature recovery is live. The ClobAuth relay is implemented but
**deprecated/frozen** under the Polymarket read-only policy above (it sends a signed message to Polymarket to
derive trading credentials); `/auth/sign-order` and `/auth/credentials` remain stubbed and, under this policy,
should not be implemented — they exist only to round out the now-frozen order-placement pipeline.

Both Python services follow the same layered layout: a root `main.py` uvicorn shim over an `app/` package
(`main.py` factory, `config.py` via `pydantic-settings`, `logging_config.py`, `exceptions.py`,
`dependencies.py` as the composition root, and `api/` · `schemas/` · `services/` layers). Route
handlers hold no business logic; config is read only in `config.py`; errors are `ServiceError`
subclasses rendered centrally. `logging_config.py` and the `exceptions.py` base are byte-identical
copies in both services (per-service Docker build contexts rule out a shared package) — keep them
in sync. Each service's own `CLAUDE.md` has the details.

`contracts/` documents API contracts the dashboard's mocks/components expect from the orchestrator, including
some the orchestrator doesn't implement yet — check there before assuming a frontend-consumed endpoint exists,
rather than trusting any specific example to stay current.

## Running

Everything runs through Docker Compose from the repo root. There is no Maven wrapper, no `Makefile`,
and `mvn`/`node` are not assumed to be on PATH — the containers provide them.

```bash
cp .env.example .env      # then fill in the blanks; POSTGRES_*/RABBITMQ_* are required
docker compose up --build
docker compose logs -f orchestrator
docker compose down -v    # -v also drops postgres_data, forcing schema re-init
```

`.env.example` uses `POSTGRES_USER`/`POSTGRES_PASSWORD`, but `docker-compose.yaml` reads `DB_USER`/`DB_PASSWORD`.
Set both, or Compose will substitute empty strings and Postgres will refuse to start.

Verify the stack is up with `GET localhost:8080/actuator/health` (the Docker healthcheck, unauthenticated). The
orchestrator's own `GET /health` also fans out to both Python services and reports their status inline, but —
unlike `/actuator/health` — it requires a valid JWT (`SecurityConfig` permits only `/api/auth/login`,
`/api/auth/siwe/nonce`, `/api/auth/siwe/verify`, `/actuator/health`, and `/error` — `GET /health` itself is
deliberately not in that list); log in first and pass the token to check it. RabbitMQ management UI is at
localhost:15672.

Hot reload: the Python services and dashboard are bind-mounted (uvicorn `--reload`, Vite HMR), so edits apply
without a rebuild. The orchestrator is **not** — any Java change needs
`docker compose up -d --build orchestrator` (multi-stage Maven build, ~2 min cold).

### Running the orchestrator outside Docker

`application-local.yml` exists precisely for this: it rewrites the Docker service hostnames (`postgres`, `redis`,
`rabbitmq`, `auth-service`) to `localhost` and sets `spring.sql.init.mode=never`. Start only the infra
containers, then run the app with the `local` profile:

```bash
docker compose up -d postgres redis rabbitmq auth-service ai-agent
export JWT_SECRET=$(openssl rand -base64 32)           # application-local.yml doesn't set one — see JwtService
mvn spring-boot:run -Dspring-boot.run.profiles=local   # or -Dspring.profiles.active=local in IntelliJ VM options
```

### Tests

`orchestrator/` has a real, substantial JUnit 5 test suite now (`orchestrator/src/test/`) — see
`orchestrator/CLAUDE.md` for the testing pattern used there. Neither Python service nor the dashboard has tests
yet.

## Architecture

The orchestrator is by far the largest and most complex service — its schema/auth/market-sync/indicator/strategy
architecture lives entirely in **`orchestrator/CLAUDE.md`**, not here. What follows is the cross-service picture.

**Auth, in one sentence**: SIWE (wallet-signature login) is the primary, self-serve path; email/password login
exists only for a DB-seeded admin account. The JWT is the sole credential everywhere (stateless, no sessions),
and its principal is the user's UUID. Crypto verification (SIWE signature recovery, EIP-712 ClobAuth relay)
happens in `auth-service`; every policy decision (nonce, domain, expiry, chain id, authorization) happens in the
orchestrator. The orchestrator holds encrypted L2 trading credentials and never sees a private key.

### Async boundary

Order-critical calls are synchronous HTTP (`RestClient`) because the caller can't proceed without the result —
this pattern is documented for when the (currently deprecated/frozen, see the Polymarket API policy above)
order-placement path resumes; it is not yet wired up to Polymarket. AI sentiment is fire-and-forget over
RabbitMQ (`ai.signals`) — the orchestrator triggers `POST /ai/analyze` and picks up the result from the queue
later. This split is deliberate (ADR-003); keep new code on the correct side of it.
