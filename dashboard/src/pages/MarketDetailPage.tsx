import { Link, useParams } from 'react-router-dom'
import { useMemo } from 'react'
import { useSeriesDetail } from '../features/series/useSeriesQueries'
import { formatDate, formatPriceCents, formatUsdCompact } from '../features/series/format'
import { Empty, ErrorState, Loading } from '../components/series/ListStates'
import { StatusBadge } from '../components/series/StatusBadge'
import { Card } from '../components/market/primitives'
import { MarketPriceHistoryChart } from '../components/market/MarketPriceHistoryChart'
import { MarketOhlcChart } from '../components/market/MarketOhlcChart'
import { RecentOrdersCard } from '../components/market/RecentOrdersCard'
import { EngineDecisionsCard } from '../components/market/EngineDecisionsCard'
import { LatestSentimentCard } from '../components/market/LatestSentimentCard'
import { NewsSummaryCard } from '../components/market/NewsSummaryCard'
import { YourPositionCard } from '../components/market/YourPositionCard'
import { Metric } from '../components/market/Metric'

/**
 * Market Detail page (mock `dashboard/mocks/13-market-detail.html`). Reached by
 * clicking a `MarketRow` on the Series Detail page — route
 * `/series/:seriesId/markets/:marketId`, rendered inside `AppShell`.
 *
 * The header / price / metrics come from the existing series-detail payload
 * (`useSeriesDetail` → find the market by id) — no new endpoint. The probability
 * chart and the OHLC candlestick chart (the underlying asset's price, via the
 * series' linked ticker) are both real. Everything else below (orders, sentiment,
 * news, position, engine decisions) has no backend yet; each is a dedicated
 * skeleton component under `components/market/` documenting its missing contract.
 */

/** UUID → mock-style short id, e.g. "0d3f8c1a…e71b". */
function shortId(id: string): string {
  const compact = id.replace(/-/g, '')
  return compact.length > 12 ? `${compact.slice(0, 8)}…${compact.slice(-4)}` : id
}

/** ISO instant → "14:30:12 UTC", matching the mock's "last synced". */
function formatUtcTime(iso: string | null): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  return `${date.toLocaleTimeString('en-GB', { timeZone: 'UTC', hour12: false })} UTC`
}

function DisabledButton({ label, reason }: { label: string; reason: string }) {
  // Target route not built yet — same treatment as unimplemented nav items.
  return (
    <span
      aria-disabled="true"
      title={reason}
      className="flex cursor-not-allowed items-center rounded-md border border-border-mid bg-bg3 px-3.5 py-2 text-[12px] font-medium text-text-muted"
    >
      {label}
    </span>
  )
}

export function MarketDetailPage() {
  const { seriesId, marketId } = useParams<{ seriesId: string; marketId: string }>()
  const { data, isLoading, isError, error, refetch } = useSeriesDetail(seriesId)

  const market = useMemo(
    () => data?.markets.find((m) => m.id === marketId),
    [data, marketId],
  )

  const backTo = seriesId ? `/series/${seriesId}` : '/series'

  return (
    <div>
      <Link to={backTo} className="text-[12px] text-text-muted hover:text-text-primary">
        ← {data?.title ?? 'Back to series'}
      </Link>

      {isLoading && (
        <div className="mt-6">
          <Loading label="Loading market…" />
        </div>
      )}

      {isError && (
        <div className="mt-6">
          <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />
        </div>
      )}

      {data && !market && (
        <div className="mt-6">
          <Empty message="That market is not part of this series." />
        </div>
      )}

      {data && market && (
        <>
          <div className="mb-5 mt-3.5 flex items-start justify-between gap-5">
            <div className="min-w-0">
              <h1 className="text-[19px] font-medium leading-tight text-text-primary">
                {market.question}
              </h1>
              <div className="mt-1.5 font-mono text-[11px] text-text-muted">
                id {shortId(market.id)} · category {market.category ?? '—'} · last synced{' '}
                {formatUtcTime(market.lastSyncedAt)}
              </div>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <StatusBadge status={market.status} />
              <DisabledButton label="＋ Strategy" reason="Create-strategy route not available yet" />
              <DisabledButton label="Place order" reason="Place-order route not available yet" />
            </div>
          </div>

          <div className="grid gap-4 lg:grid-cols-[1.55fr_1fr]">
            {/* Left column */}
            <div className="space-y-4">
              <Card>
                <div className="mb-3.5 flex gap-2.5">
                  <div className="flex-1 rounded-[9px] border border-border bg-bg2 px-3.5 py-3">
                    <div className="text-[10px] uppercase tracking-wider text-text-muted">
                      UP price
                    </div>
                    <div className="mt-1.5 font-mono text-[20px] text-accent">
                      {formatPriceCents(market.upPrice)}
                    </div>
                  </div>
                  <div className="flex-1 rounded-[9px] border border-border bg-bg2 px-3.5 py-3">
                    <div className="text-[10px] uppercase tracking-wider text-text-muted">
                      DOWN price
                    </div>
                    <div className="mt-1.5 font-mono text-[20px] text-text-primary">
                      {formatPriceCents(market.downPrice)}
                    </div>
                  </div>
                </div>

                <MarketPriceHistoryChart marketId={market.id} />

                <div className="mt-3.5 grid grid-cols-2 gap-2.5 sm:grid-cols-4">
                  <Metric label="Vol 24h" value={formatUsdCompact(market.volume24h)} />
                  <Metric label="Liquidity" value={formatUsdCompact(market.liquidity)} />
                  <Metric label="Resolves" value={formatDate(market.resolutionDate)} />
                  <Metric label="Series" value={data.recurrence} />
                </div>
              </Card>

              <Card>
                <MarketOhlcChart marketId={market.id} />
              </Card>

              <RecentOrdersCard marketId={market.id} />
              <EngineDecisionsCard marketId={market.id} />
            </div>

            {/* Right column */}
            <div className="space-y-4">
              <LatestSentimentCard marketId={market.id} />
              <NewsSummaryCard marketId={market.id} />
              <YourPositionCard marketId={market.id} />
            </div>
          </div>
        </>
      )}
    </div>
  )
}
