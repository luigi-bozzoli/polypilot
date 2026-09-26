import { Link } from 'react-router-dom'
import type { MarketView } from '../../features/series/types'
import { formatDate, formatUsdCompact } from '../../features/series/format'
import { PricePair } from './PricePair'
import { StatusBadge } from './StatusBadge'

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-[10px] uppercase tracking-wider text-text-muted">{label}</div>
      <div className="mt-0.5 font-mono text-[12px] text-text-primary">{value}</div>
    </div>
  )
}

/**
 * A market inside a series' Open / History list. The whole row links to the
 * Market Detail page (`/series/:seriesId/markets/:marketId`, mock
 * `13-market-detail.html`) — `seriesId` is threaded down from
 * `SeriesDetailPage` since the detail page reuses the series-detail payload.
 */
export function MarketRow({ market, seriesId }: { market: MarketView; seriesId: string }) {
  return (
    <Link
      to={`/series/${seriesId}/markets/${market.id}`}
      className="block rounded-lg border border-border bg-bg1 p-4 transition-colors hover:border-border-mid hover:bg-bg2"
    >
      <div className="flex items-start justify-between gap-3">
        <span className="text-[13px] text-text-primary">{market.question}</span>
        <StatusBadge status={market.status} />
      </div>

      <div className="mt-3 flex flex-wrap items-end gap-x-8 gap-y-3">
        <div>
          <div className="text-[10px] uppercase tracking-wider text-text-muted">Price</div>
          <div className="mt-1">
            <PricePair upPrice={market.upPrice} downPrice={market.downPrice} outcome={market.outcome} />
          </div>
        </div>
        <Metric label="Vol 24h" value={formatUsdCompact(market.volume24h)} />
        <Metric label="Liquidity" value={formatUsdCompact(market.liquidity)} />
        <Metric label="Resolves" value={formatDate(market.resolutionDate)} />
      </div>
    </Link>
  )
}
