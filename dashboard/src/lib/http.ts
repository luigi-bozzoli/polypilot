import { loadSession } from '../features/auth/session'

/**
 * Pull a human-readable message out of an error response body, falling back to
 * a caller-supplied default. Mirrors the private helper in `features/auth/authApi.ts`
 * (kept separate there so the pre-auth flow has no dependency on the session).
 */
export async function readErrorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body = await res.json()
    return body.detail ?? body.message ?? fallback
  } catch {
    return fallback
  }
}

/**
 * GET a JSON resource that requires a valid session. Attaches the persisted JWT
 * as a bearer token; throws `Error(message)` on any non-OK response so callers
 * (and react-query) get a single, displayable failure.
 */
export async function authGet<T>(url: string): Promise<T> {
  const session = loadSession()
  const headers: HeadersInit = session ? { Authorization: `Bearer ${session.token}` } : {}

  const res = await fetch(url, { headers })
  if (!res.ok) {
    const fallback = res.status === 401 || res.status === 403 ? 'Your session has expired' : `Request failed (${res.status})`
    throw new Error(await readErrorMessage(res, fallback))
  }
  return res.json() as Promise<T>
}

/**
 * GET a JSON resource that may not exist yet, requiring a valid session. Same auth/error
 * handling as `authGet`, except a `204 No Content` response returns `undefined` instead of
 * attempting to parse an empty body as JSON — used by endpoints like
 * `GET /api/market/{id}/news/latest` that return 204 when no row exists.
 */
export async function authGetOptional<T>(url: string): Promise<T | undefined> {
  const session = loadSession()
  const headers: HeadersInit = session ? { Authorization: `Bearer ${session.token}` } : {}

  const res = await fetch(url, { headers })
  if (res.status === 204) {
    return undefined
  }
  if (!res.ok) {
    const fallback = res.status === 401 || res.status === 403 ? 'Your session has expired' : `Request failed (${res.status})`
    throw new Error(await readErrorMessage(res, fallback))
  }
  return res.json() as Promise<T>
}

/**
 * POST a JSON body and parse a JSON response, requiring a valid session. Same auth/error
 * handling as `authGet`.
 */
export async function authPost<T>(url: string, body: unknown): Promise<T> {
  const session = loadSession()
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
  }

  const res = await fetch(url, { method: 'POST', headers, body: JSON.stringify(body) })
  if (!res.ok) {
    const fallback = res.status === 401 || res.status === 403 ? 'Your session has expired' : `Request failed (${res.status})`
    throw new Error(await readErrorMessage(res, fallback))
  }
  return res.json() as Promise<T>
}

/**
 * PUT a JSON body and parse a JSON response, requiring a valid session. Same auth/error
 * handling as `authPost`.
 */
export async function authPut<T>(url: string, body: unknown): Promise<T> {
  const session = loadSession()
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
  }

  const res = await fetch(url, { method: 'PUT', headers, body: JSON.stringify(body) })
  if (!res.ok) {
    const fallback = res.status === 401 || res.status === 403 ? 'Your session has expired' : `Request failed (${res.status})`
    throw new Error(await readErrorMessage(res, fallback))
  }
  return res.json() as Promise<T>
}

/**
 * DELETE a resource, requiring a valid session. Same auth/error handling as `authPost`; the
 * response body is discarded (the API returns 204 No Content on success).
 */
export async function authDelete(url: string): Promise<void> {
  const session = loadSession()
  const headers: HeadersInit = session ? { Authorization: `Bearer ${session.token}` } : {}

  const res = await fetch(url, { method: 'DELETE', headers })
  if (!res.ok) {
    const fallback = res.status === 401 || res.status === 403 ? 'Your session has expired' : `Request failed (${res.status})`
    throw new Error(await readErrorMessage(res, fallback))
  }
}
