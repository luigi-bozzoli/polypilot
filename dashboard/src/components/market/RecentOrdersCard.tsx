import { Link } from 'react-router-dom'
import { Card, SectionLabel } from './primitives'
import { Empty, ErrorState, Loading } from '../series/ListStates'
import { useMarketOrders } from '../../features/market/useMarketOrders'
import { orderStatusBadgeClassName } from '../../features/strategies/orderStatus'
import { formatAuditTimestamp } from '../../features/audit/format'
import { formatPriceCents } from '../../features/series/format'

/**
 * "Recent orders on this market" (mock `13-market-detail.html`). Backed by
 * `GET /api/market/{marketId}/orders` — the market-scoped slice of `orders`, newest
 * first. Mirrors `StrategyRecentOrdersCard` (its "Market" column becomes "Strategy"
 * here, linking to the strategy that placed the order). Contract:
 * `contracts/market-recent-orders.md`.
 */
const COLUMNS = ['Time', 'Strategy', 'Side', 'Req / Filled', 'Price', 'Status'] as const

export function RecentOrdersCard({ marketId }: { marketId: string }) {
  const { data, isLoading, isError, error, refetch } = useMarketOrders(marketId)

  return (
    <Card flush>
      <SectionLabel className="px-4 pt-4">Recent orders on this market</SectionLabel>

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

      {data && data.orders.length === 0 && (
        <div className="px-4 pb-4">
          <Empty message="No orders on this market yet." />
        </div>
      )}

      {data && data.orders.length > 0 && (
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
            {data.orders.map((order) => (
              <tr key={order.id}>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono text-[11px] text-text-muted">
                  {formatAuditTimestamp(order.placedAt)}
                </td>
                <td className="max-w-[220px] truncate border-b border-border px-2.5 py-2.5" title={order.strategyName}>
                  <Link to={`/strategies/${order.strategyId}`} className="text-text-secondary hover:text-text-primary">
                    {order.strategyName}
                  </Link>
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
