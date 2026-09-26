import type { ReactNode } from 'react'

/**
 * Shared visual shapes for the Market Detail page (mock
 * `dashboard/mocks/13-market-detail.html`). Small on purpose — just the
 * repeated mock classes (`.card`, `.section-label`) plus the muted
 * "no data source yet" placeholder used by the five skeleton cards, matching
 * `components/health/PlaceholderStatusCard.tsx`.
 */

/** Mock `.card`. */
export function Card({
  children,
  className = '',
  flush = false,
}: {
  children: ReactNode
  className?: string
  /** `flush` drops the padding (used by the orders card whose table bleeds to
   *  the card edge — mock `.card` with `padding:0;overflow:hidden`). */
  flush?: boolean
}) {
  return (
    <div
      className={`rounded-[11px] border border-border bg-bg1 ${flush ? 'overflow-hidden' : 'p-4'} ${className}`}
    >
      {children}
    </div>
  )
}

/** Mock `.section-label`. */
export function SectionLabel({
  children,
  className = '',
}: {
  children: ReactNode
  className?: string
}) {
  return (
    <div
      className={`text-[10px] uppercase tracking-[0.1em] text-text-muted ${className}`}
    >
      {children}
    </div>
  )
}

/** A single muted placeholder bar standing in for a not-yet-fetched value. */
export function SkeletonBar({ className = 'w-24' }: { className?: string }) {
  return (
    <span
      className={`inline-block h-[10px] rounded bg-bg3 align-middle ${className}`}
      aria-hidden="true"
    />
  )
}

/**
 * Footer line naming the missing endpoint. Mirrors the
 * "no data source yet" copy of `PlaceholderStatusCard`.
 */
export function NoDataNote({ endpoint }: { endpoint: string }) {
  return (
    <div className="mt-3 flex items-center gap-2 font-mono text-[10px] text-text-muted">
      <span className="h-[7px] w-[7px] shrink-0 rounded-full bg-text-muted" aria-hidden="true" />
      no data source yet · {endpoint}
    </div>
  )
}

/** Empty state for a real endpoint that returned 204 (no row yet). */
export function Empty({ message }: { message: string }) {
  return (
    <div className="py-4 text-center text-[11px] text-text-muted">{message}</div>
  )
}

/** Error state for a real endpoint whose fetch failed. */
export function ErrorState({ message }: { message: string }) {
  return (
    <div className="py-4 text-center text-[11px] text-red">{message}</div>
  )
}
