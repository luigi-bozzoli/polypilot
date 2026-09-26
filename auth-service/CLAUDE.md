# CLAUDE.md — auth-service

Python 3.12 / FastAPI. The **only** service allowed to touch private keys and raw
wallet signatures. See the repo-root `CLAUDE.md` for the monorepo picture and the
shared Python-service conventions.

## Layout

```
main.py                 # uvicorn entrypoint shim → `from app.main import app`
app/
  main.py               # create_app(): settings → logging → routers → exception handlers
  config.py             # Settings (pydantic-settings) + cached get_settings()
  logging_config.py     # configure_logging() — identical copy in ai-agent
  exceptions.py         # ServiceError hierarchy + install_exception_handlers()
  dependencies.py       # composition root: cached Depends providers
  api/                  # one APIRouter per domain, no business logic in handlers
    health.py  siwe.py  clob.py  stubs.py
  schemas/              # Pydantic request/response models = the public contract
    health.py  siwe.py  clob.py  common.py
  services/             # business logic; pure objects, injected via dependencies.py
    health.py  siwe.py  clob.py
```

Route handlers are two lines: resolve the service via `Depends`, call it, return
its typed result. All configuration comes from `app/config.py` — no `os.environ`
reads anywhere else. Errors are raised as `ServiceError` subclasses and rendered
centrally as `{"detail": ...}`; no ad hoc `try/except` in routes.

## Two verification responsibilities

This service does cryptographic verification only. It never enforces
SIWE/application policy — nonce checks, domain checks, expiry/`issuedAt`
windows, chain id, user lookup, and every authorization decision live in the
**orchestrator**.

### 1. SIWE ownership verification — `POST /auth/siwe/verify`

`app/services/siwe.py`. Recovers the Ethereum address behind a `personal_sign`
signature over an EIP-4361 message, using `eth_account`:

```python
recovered = Account.recover_message(encode_defunct(text=message), signature=signature)
```

Request: `{ "message": "<exact EIP-4361 string>", "signature": "0x…" }`
Response: `{ "address": "0x…" }` — EIP-55 checksummed.

A malformed or unrecoverable signature raises `SignatureRecoveryError` → **HTTP
400**. Nothing else about the message is inspected here.

### 2. ClobAuth relay verification — `POST /auth/verify-and-derive` — **DEPRECATED/FROZEN**

`app/services/clob.py`. Relays an already-signed EIP-712 ClobAuth message to
Polymarket's L1 auth endpoint (`/auth/api-key`) and returns the derived L2 API
credentials (`api_key`, `secret`, `passphrase`). Polymarket verifies the
signature itself; this service holds no key for this flow. Bad address → 400,
transport failure → 502, upstream non-2xx → passed through unchanged.

**Under the repo's Polymarket API policy (see root `CLAUDE.md`), this endpoint is deprecated/frozen**: it sends
data to Polymarket (the signed ClobAuth message) rather than merely reading from it, and exists only to derive
credentials for the now-frozen order-placement pipeline. The code stays as-is and its behavior above remains
accurate, but do not extend it, add new callers of it, or build new functionality on top of the credentials it
derives.

## Dependencies

- HTTP client is `httpx` (sync `httpx.Client`), matching ai-agent. The outbound
  CLOB call is intentionally blocking — no credentials, no trading until it
  returns.
- `eth-account` is pulled in transitively by `web3==7.6.0` but is listed
  explicitly in `requirements.txt` because `app/services/siwe.py` imports it
  directly. Do not upgrade `web3` to satisfy it.
- `pydantic-settings` backs `app/config.py`.

## Phase 2 stubs — **DEPRECATED/FROZEN, do not implement**

`app/api/stubs.py`: `POST /auth/sign-order` and `POST /auth/credentials` return
**200** `{"detail": "not implemented yet"}`. `Settings.polymarket_private_key`
(`POLYMARKET_PRIVATE_KEY`) is already wired into `config.py` and injected by
docker-compose ahead of this work, but nothing reads it yet.

Both stubs exist only to round out the order-placement pipeline (signing and submitting orders to Polymarket —
sending, not reading, data), which is deprecated/frozen under the repo's Polymarket API policy (root
`CLAUDE.md`). Leave them as stubs; do not build out their implementations.
