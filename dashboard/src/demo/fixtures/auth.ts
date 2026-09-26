import type { LoginResponse } from '../../features/auth/types'

/** Demo principal used for both login paths — only the `role`/`email` differ per path. */
export const DEMO_USER_ID = 'demo-user-0001'

function base64url(input: string): string {
  return btoa(input).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

/**
 * Builds an unsigned (`alg: 'none'`) JWT: valid three-segment structure so
 * `features/auth/session.ts#decodeJwtPayload` reads the `exp` claim exactly like a real
 * token, but with no signature — there's no key to sign with in a backend-less demo, and the
 * dashboard never verifies the signature client-side (that's the orchestrator's job for real).
 */
function buildFakeJwt(payload: Record<string, unknown>): string {
  const header = base64url(JSON.stringify({ alg: 'none', typ: 'JWT' }))
  const body = base64url(JSON.stringify(payload))
  return `${header}.${body}.`
}

function buildSession(email: string, role: string): LoginResponse {
  const exp = Math.floor(Date.now() / 1000) + 24 * 60 * 60
  const token = buildFakeJwt({ sub: DEMO_USER_ID, role, exp })
  return { token, email, role }
}

/** POST /api/auth/login accepts any body in demo mode — this is the response for any input. */
export function demoPasswordLogin(): LoginResponse {
  return buildSession('demo-admin@polypilot.local', 'ADMIN')
}

/** POST /api/auth/siwe/verify's response once the simulated wallet flow "completes". */
export function demoWalletLogin(address: string): LoginResponse {
  return buildSession(address.toLowerCase(), 'USER')
}

/** A valid-looking, deterministic 20-byte hex address for the simulated wallet connection. */
export const DEMO_WALLET_ADDRESS = '0x71C7656EC7ab88b098defB751B7401B5f6d8976'
