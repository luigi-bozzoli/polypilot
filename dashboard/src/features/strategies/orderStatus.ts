import type { OrderStatus } from './types'

/**
 * Badge tone per order `status` — same visual language as `features/audit/decisionKind.ts`'s
 * decision badges. Only `FILLED` is ever produced today (see `OpenTradeService`); the rest are
 * handled generically so the card doesn't need another pass once cancel/fail paths land.
 */
export type OrderStatusTone = 'green' | 'blue' | 'yellow' | 'red' | 'neutral'

const TONE_BY_STATUS: Record<OrderStatus, OrderStatusTone> = {
  FILLED: 'green',
  PARTIALLY_FILLED: 'blue',
  OPEN: 'blue',
  PENDING: 'yellow',
  CANCELLED: 'neutral',
  FAILED: 'red',
}

const TONE_CLASSNAMES: Record<OrderStatusTone, string> = {
  green: 'border-accent-border bg-accent-dim text-accent',
  blue: 'border-[rgba(77,158,245,0.3)] bg-blue-dim text-blue',
  yellow: 'border-[rgba(240,168,50,0.3)] bg-yellow-dim text-yellow',
  red: 'border-red-border bg-red-dim text-red',
  neutral: 'border-border-mid bg-bg3 text-text-secondary',
}

/** Tailwind classes for a `status` badge — small pill, mono, uppercase. */
export function orderStatusBadgeClassName(status: OrderStatus): string {
  return `rounded border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider ${TONE_CLASSNAMES[TONE_BY_STATUS[status]]}`
}
