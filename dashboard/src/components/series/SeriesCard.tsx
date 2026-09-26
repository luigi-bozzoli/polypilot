import { Link } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import type { SeriesSummary } from '../../features/series/types'
import { formatUsdCompact } from '../../features/series/format'
import { PricePair } from './PricePair'
import { StatusBadge } from './StatusBadge'

export function SeriesCard({ series }: { series: SeriesSummary }) {
  const { currentMarket } = series

  return (
    <Link
      to={`/series/${series.id}`}
      className="block rounded-lg border border-border bg-bg1 p-4 transition-colors hover:border-border-mid hover:bg-bg2"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="truncate text-[14px] font-medium text-text-primary">{series.title}</span>
            <span className="rounded border border-border bg-bg3 px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-text-muted">
              {series.recurrence}
            </span>
            {series.seriesType === 'USER_ADDITION' && (
              <span className="rounded border border-border-mid bg-bg3 px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-text-secondary">
                Added
              </span>
            )}
          </div>
          <div className="mt-0.5 font-mono text-[11px] text-text-muted">{series.ticker}</div>
        </div>
        <div className="flex flex-shrink-0 items-center gap-2 text-[11px] text-text-muted">
          <span>
            {series.openMarketCount} open · {series.resolvedMarketCount} resolved
          </span>
          <ChevronRight className="h-4 w-4" aria-hidden="true" />
        </div>
      </div>

      <div className="mt-3 border-t border-border pt-3">
        {currentMarket ? (
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="min-w-0 flex-1 truncate text-[12px] text-text-secondary">
              {currentMarket.question}
            </span>
            <div className="flex items-center gap-3">
              <PricePair upPrice={currentMarket.upPrice} downPrice={currentMarket.downPrice} />
              <span className="font-mono text-[11px] text-text-muted">
                {formatUsdCompact(currentMarket.volume24h)} 24h
              </span>
              <StatusBadge status={currentMarket.status} />
            </div>
          </div>
        ) : (
          <span className="text-[12px] text-text-muted">No open market right now</span>
        )}
      </div>
    </Link>
  )
}
