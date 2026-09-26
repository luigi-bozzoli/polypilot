import { Card, SectionLabel } from '../market/primitives'
import { Empty, ErrorState, Loading } from '../series/ListStates'
import { useOpenPositions } from '../../features/positions/usePositionsQueries'
import { formatPriceCents } from '../../features/series/format'

const COLUMNS = ['Market', 'Side', 'Size', 'Avg', 'Unrl. P&L'] as const

/**
 * "Open positions" (mock `10-overview.html`) — backed by `GET /api/positions`. Every row is
 * currently a simulated dry-run position (no live-trading path exists yet). Table shape mirrors
 * `RecentOrdersCard`.
 */
export function OpenPositionsCard() {
  const { data, isLoading, isError, error, refetch } = useOpenPositions(4)

  return (
    <Card flush>
      <SectionLabel className="px-4 pt-4">Open positions</SectionLabel>

      {isLoading && (
        <div className="px-4 py-4">
          <Loading label="Loading positions…" />
        </div>
      )}

      {isError && (
        <div className="px-4 py-4">
          <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />
        </div>
      )}

      {data && data.length === 0 && (
        <div className="px-4 pb-4">
          <Empty message="No open positions." />
        </div>
      )}

      {data && data.length > 0 && (
        <table className="mt-2 w-full border-collapse text-[12px]">
          <thead>
            <tr>
              {COLUMNS.map((col) => (
                <th
                  key={col}
                  className="border-b border-border px-2.5 py-2.5 text-left text-[10px] font-normal uppercase tracking-wider text-text-muted"
                >
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((position) => (
              <tr key={position.id}>
                <td className="max-w-[180px] truncate border-b border-border px-2.5 py-2.5 text-text-secondary" title={position.marketQuestion}>
                  {position.marketQuestion}
                </td>
                <td className="border-b border-border px-2.5 py-2.5 text-text-primary">{position.tokenSide}</td>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono text-text-primary">
                  {position.size.toFixed(0)}
                </td>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono text-text-primary">
                  {formatPriceCents(position.avgEntryPrice)}
                </td>
                <td
                  className={`whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono ${
                    position.unrealizedPnl >= 0 ? 'text-accent' : 'text-red'
                  }`}
                >
                  {position.unrealizedPnl >= 0 ? '+' : '−'}${Math.abs(position.unrealizedPnl).toFixed(2)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Card>
  )
}
