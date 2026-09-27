# PolyPilot Dashboard

React 18 + TypeScript + Vite frontend for PolyPilot.

## Running

From the repo root:

```bash
docker compose up --build          # bind-mounted, Vite HMR on http://localhost:5173
```

Standalone: `npm install && npm run dev`.

## Authentication

The dashboard has two ways in. Both end at `POST`-issued JWT → `session.ts`
(localStorage) → `AuthContext`, and `ProtectedRoute` treats the two identically —
there is no separate SIWE session mechanism.

### Real users — Connect Wallet (SIWE, primary)

Sign-In With Ethereum (EIP-4361). On the login screen:

1. **Connect Wallet** — browser-extension wallet (injected) or, if configured, a
   mobile wallet over WalletConnect.
2. **Sign-In With Ethereum** — the app fetches a fresh nonce
   (`GET /api/auth/siwe/nonce`), builds the SIWE message with viem's
   `createSiweMessage()` (its `domain`/`uri` come from `window.location`, so they
   always match whatever the backend is configured for), and asks the wallet for
   a `personal_sign` signature.
3. The `{ message, signature }` pair goes to `POST /api/auth/siwe/verify`; the
   returned JWT is stored exactly like a password login and you land on
   `/overview`. Refreshing keeps you signed in.

If the wallet isn't on an allowed chain the app offers to switch it first.

### Reviewers / demo — email + password (secondary)

Behind the **Admin & demo sign-in** link on the login screen. Still
`POST /api/auth/login`; **no wallet needed**. Like the wallet flow, the returned
JWT is stored and you land on `/overview` — not the homepage (`/`).
`orchestrator/src/main/resources/sql/002_seed_data.sql` seeds these on every boot
(idempotent):

| Account | Email | Password | Role |
|---|---|---|---|
| Admin | `admin@polypilot.local` | `password` | `ADMIN` |
| Demo  | `demo@polypilot.local`  | `password` | `USER`  |

Local development credentials only — the DB stores bcrypt hashes (strength 12) in
`user_identities.secret`; the plaintext above lives only in this README. Change
these before exposing the orchestrator anywhere real.

Self-service signup has been removed — there is no way to create a password
account from the UI.

## Wallet / chain / environment configuration

- **Chains** (`src/features/wallet/wagmiConfig.ts`): Ethereum mainnet (`1`) and
  Polygon (`137`). These mirror the orchestrator's
  `siwe.allowed-chain-ids` (`SIWE_ALLOWED_CHAIN_IDS`, default `1,137`) — a SIWE
  message on any other chain is rejected by `/api/auth/siwe/verify`. Keep the two
  lists in sync if you change either.
- **Connectors**: `injected()` (browser extensions, plus EIP-6963 auto-discovery)
  and `walletConnect()` (mobile). WalletConnect needs a project id from
  <https://cloud.walletconnect.com>:

  ```bash
  # dashboard/.env.local
  VITE_WALLETCONNECT_PROJECT_ID=your_project_id
  ```

  Without it, the injected connector still works for local development.
- **Domain / URI**: not configured in the frontend — taken from
  `window.location` at sign-in time. The backend side is `siwe.domain` /
  `siwe.uri` (`SIWE_DOMAIN`, `SIWE_URI`; defaults `localhost:5173` /
  `http://localhost:5173`). For local dev the defaults already match.

## Third-party licenses

- **Lightweight Charts™** © TradingView, Inc. — Apache License 2.0
  (<https://github.com/tradingview/lightweight-charts>). Used by
  `src/components/market/MarketPriceHistoryChart.tsx`; the license's attribution
  requirement is met by the visible "Charts by TradingView" link rendered beneath
  that chart.
