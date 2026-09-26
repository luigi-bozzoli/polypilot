import { LoginResponse } from './types'

/**
 * localStorage key holding the persisted auth session. The value is a
 * JSON-serialized {@link LoginResponse} ({ token, email, role }).
 */
const STORAGE_KEY = 'polypilot.session'

type JwtPayload = {
  exp?: number
}

/**
 * Decode the payload segment of a JWT without verifying its signature.
 * Signature verification is the orchestrator's job — here we only need the
 * `exp` claim to avoid restoring an obviously-stale session on refresh.
 */
function decodeJwtPayload(token: string): JwtPayload | null {
  const segment = token.split('.')[1]
  if (!segment) return null
  try {
    const normalized = segment.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(normalized)) as JwtPayload
  } catch {
    return null
  }
}

function isExpired(token: string): boolean {
  const payload = decodeJwtPayload(token)
  if (!payload || typeof payload.exp !== 'number') return false // no exp claim → treat as non-expiring
  return payload.exp * 1000 <= Date.now()
}

/**
 * Read the persisted session, or null if there is none, it is malformed, or
 * its token has expired. Safe to call during render / state initialization.
 */
export function loadSession(): LoginResponse | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null

    const parsed = JSON.parse(raw) as Partial<LoginResponse>
    if (!parsed.token || !parsed.email || !parsed.role) return null
    if (isExpired(parsed.token)) {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }

    return { token: parsed.token, email: parsed.email, role: parsed.role }
  } catch {
    return null
  }
}

export function saveSession(session: LoginResponse): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  } catch {
    // Storage unavailable (private mode / quota) — session stays in memory only.
  }
}

export function clearSession(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Nothing we can do; in-memory state is cleared by the caller regardless.
  }
}
