import { Card, SectionLabel } from '../market/primitives'
import { Empty, ErrorState, Loading } from '../series/ListStates'
import { useStrategyOrders } from '../../features/strategies/useStrategiesQueries'
import { orderStatusBadgeClassName } from '../../features/strategies/orderStatus'
import { formatAuditTimestamp } from '../../features/audit/format'
import { formatPriceCents } from '../../features/series/format'

/**
 * "Recent orders from this strategy" (mock `15-strategy-detail.html`). Backed by
 * `GET /api/strategies/{id}/orders` — the strategy-scoped slice of `orders`, newest
 * first. Contract: `contracts/strategy-recent-orders.md`.
 */
const COLUMNS = ['Placed', 'Market', 'Side', 'Req / Filled', 'Price', 'Status'] as const

export function StrategyRecentOrdersCard({ strategyId }: { strategyId: string }) {
  const { data, isLoading, isError, error, refetch } = useStrategyOrders(strategyId)

  return (
    <Card flush>
      <SectionLabel className="px-4 pt-4">Recent orders from this strategy</SectionLabel>

      {isLoading && (
        <div className="px-4 py-4">
          <Loading label="Loading orders…" />
        </div>
      )}

      {isError && (
        <div className="px-4 py-4">
          <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />
        </div>
      )}

      {data && data.length === 0 && (
        <div className="px-4 pb-4">
          <Empty message="No orders for this strategy yet." />
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
            {data.map((order) => (
              <tr key={order.id}>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono text-[11px] text-text-muted">
                  {formatAuditTimestamp(order.placedAt)}
                </td>
                <td className="max-w-[220px] truncate border-b border-border px-2.5 py-2.5 text-text-secondary" title={order.marketQuestion}>
                  {order.marketQuestion}
                </td>
                <td className="border-b border-border px-2.5 py-2.5 text-text-primary">{order.side}</td>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono text-text-primary">
                  {order.sizeRequested.toFixed(4)} / {order.sizeFilled.toFixed(4)}
                </td>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono text-text-primary">
                  {formatPriceCents(order.price)}
                </td>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5">
                  <span className={orderStatusBadgeClassName(order.status)}>
                    {order.isDryRun ? 'dry · ' : ''}
                    {order.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Card>
  )
}
