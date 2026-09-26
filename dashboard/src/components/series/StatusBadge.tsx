import type { MarketStatus } from '../../features/series/types'

const STYLES: Record<MarketStatus, string> = {
  OPEN: 'bg-accent-dim text-accent border-accent-border',
  RESOLVED: 'bg-bg3 text-text-secondary border-border-mid',
  CLOSED: 'bg-bg3 text-text-muted border-border',
  CANCELLED: 'bg-red-dim text-red border-red-border',
}

export function StatusBadge({ status }: { status: MarketStatus }) {
  return (
    <span
      className={`inline-block rounded border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider ${STYLES[status]}`}
    >
      {status}
    </span>
  )
}
