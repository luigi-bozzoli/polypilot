import { LoginResponse } from "./types"

async function readErrorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body = await res.json()
    return body.detail ?? body.message ?? fallback
  } catch {
    return fallback
  }
}

/**
 * Admin / demo sign-in. Kept unchanged: email + password → JWT.
 * POST /api/auth/login
 */
export async function login(email: string, password: string): Promise<LoginResponse> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, 'Invalid email or password'))
  }
  return res.json()
}

/**
 * Fresh single-use nonce for building the SIWE (EIP-4361) message.
 * GET /api/auth/siwe/nonce → { nonce }
 *
 * The nonce is handed straight to `createSiweMessage` and never persisted — the
 * orchestrator keeps the only copy (Redis, 5-min TTL, consumed on verify).
 */
export async function getSiweNonce(): Promise<string> {
  const res = await fetch('/api/auth/siwe/nonce')
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, 'Could not start wallet sign-in'))
  }
  const body = (await res.json()) as { nonce: string }
  return body.nonce
}

/**
 * Verify a signed SIWE message and exchange it for a session.
 * POST /api/auth/siwe/verify  { message, signature } → LoginResponse (JWT)
 *
 * Returns the exact same shape as {@link login}, so the caller stores it through
 * the shared session / AuthContext mechanism with no special-casing.
 */
export async function siweLogin(message: string, signature: string): Promise<LoginResponse> {
  const res = await fetch('/api/auth/siwe/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, signature }),
  })
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, 'Wallet sign-in failed'))
  }
  return res.json()
}
