import { usePortfolioSummary } from '../../features/portfolio/usePortfolioQueries'
import { Loading, ErrorState } from '../series/ListStates'

function formatUsdSigned(amount: number): string {
  const sign = amount >= 0 ? '+' : '−'
  return `${sign}$${Math.abs(amount).toFixed(2)}`
}

/**
 * Stat row (mock `10-overview.html`'s `.stat-row`) — three tiles backed by
 * `GET /api/portfolio/summary`. "Simulated equity" from the mock is deliberately omitted: nothing
 * in the schema stores a starting-capital baseline to compute it from.
 */
export function StatRow() {
  const { data, isLoading, isError, error, refetch } = usePortfolioSummary()

  if (isLoading) return <Loading label="Loading portfolio summary…" />
  if (isError) return <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />
  if (!data) return null

  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
      <Tile
        label="Unrealized P&L"
        value={formatUsdSigned(data.unrealizedPnl)}
        valueClassName={data.unrealizedPnl >= 0 ? 'text-accent' : 'text-red'}
        sub={`across ${data.openPositionCount} open position${data.openPositionCount === 1 ? '' : 's'}`}
      />
      <Tile
        label="Realized P&L · 7d"
        value={formatUsdSigned(data.realizedPnl7d)}
        valueClassName={data.realizedPnl7d >= 0 ? 'text-accent' : 'text-red'}
      />
      <Tile label="Dry orders · today" value={String(data.dryOrdersToday)} />
    </div>
  )
}

function Tile({
  label,
  value,
  valueClassName = 'text-text-primary',
  sub,
}: {
  label: string
  value: string
  valueClassName?: string
  sub?: string
}) {
  return (
    <div className="rounded-[10px] border border-border bg-bg1 p-[14px_16px]">
      <div className="text-[10px] uppercase tracking-[0.06em] text-text-muted">{label}</div>
      <div className={`mt-[7px] font-mono text-[21px] ${valueClassName}`}>{value}</div>
      {sub && <div className="mt-[5px] font-mono text-[11px] text-text-muted">{sub}</div>}
    </div>
  )
}
