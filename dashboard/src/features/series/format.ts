/** Shared display formatting for series/market numbers. All tolerate `null`. */

const EM_DASH = '—'

/** A 0–1 probability as cents, e.g. 0.485 → "48.5¢". */
export function formatPriceCents(price: number | null): string {
  if (price == null) return EM_DASH
  const cents = price * 100
  return `${cents.toFixed(1)}¢`
}

/** Compact USD, e.g. 106421 → "$106.4K", 229.37 → "$229". */
export function formatUsdCompact(amount: number | null): string {
  if (amount == null) return EM_DASH
  const abs = Math.abs(amount)
  if (abs >= 1_000_000) return `$${(amount / 1_000_000).toFixed(1)}M`
  if (abs >= 1_000) return `$${(amount / 1_000).toFixed(1)}K`
  return `$${Math.round(amount)}`
}

/** ISO timestamp → "Sep 3, 2026", or em dash. */
export function formatDate(iso: string | null): string {
  if (!iso) return EM_DASH
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return EM_DASH
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}
