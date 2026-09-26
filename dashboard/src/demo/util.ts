/** Deterministic helpers shared by every demo fixture/handler — same data every load. */

/** mulberry32: tiny, fast, deterministic PRNG. Same seed ⇒ same sequence every run. */
export function mulberry32(seed: number): () => number {
  let a = seed
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

/** The one PRNG instance fixtures should share, so cross-fixture IDs/values stay reproducible. */
export const rng = mulberry32(0x504f4c59) // 'POLY' as hex-ish seed

/** Random float in `[min, max)` using the shared demo `rng`. */
export function randomFloat(min: number, max: number): number {
  return min + rng() * (max - min)
}

/** Random integer in `[min, max]` (inclusive) using the shared demo `rng`. */
export function randomInt(min: number, max: number): number {
  return Math.floor(randomFloat(min, max + 1))
}

/** Pick a deterministic element from `items` using the shared demo `rng`. */
export function pick<T>(items: readonly T[]): T {
  return items[randomInt(0, items.length - 1)]
}

/** ISO-8601 instant `minutes` before now — fixtures use this instead of fixed dates so
 *  "recent" data always looks recent, however long ago the demo was built. */
export function ago(minutes: number): string {
  return new Date(Date.now() - minutes * 60_000).toISOString()
}

/** ISO-8601 instant `minutes` after now. */
export function fromNow(minutes: number): string {
  return new Date(Date.now() + minutes * 60_000).toISOString()
}

/** Deterministic id: a fixed namespace plus a zero-padded counter, e.g. `uuid('market', 3)`. */
export function uuid(namespace: string, n: number): string {
  return `demo-${namespace}-${String(n).padStart(4, '0')}`
}

/** Small artificial delay so loading states are visible. Every demo handler awaits this. */
export function latency(): Promise<void> {
  const ms = randomInt(150, 350)
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Builds an MSW wildcard path for one `/api/...` route. The app is served under
 * `${BASE_URL}` (e.g. `/polypilot/`) but always calls `/api/...` at the origin root, so
 * handlers match on the path suffix (`*` wildcard) rather than a full origin+base URL.
 */
export function api(p: string): string {
  return `*/api${p}`
}
