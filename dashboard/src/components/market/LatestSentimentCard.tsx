import { useLatestSentiment } from '../../features/market/useLatestSentiment'
import { Card, Empty, ErrorState, SectionLabel } from './primitives'
import type { SentimentLabel } from '../../features/market/types'

const LABEL_COLOR: Record<SentimentLabel, string> = {
  BULLISH: 'text-accent',
  BEARISH: 'text-red',
  NEUTRAL: 'text-text-secondary',
}

const KV_ROWS = ['model_used', 'article_count', 'scored_at'] as const

/** "Latest sentiment · sentiment_scores" (mock `13-market-detail.html`). Contract: `contracts/market-sentiment.md`. */
export function LatestSentimentCard({ marketId }: { marketId: string }) {
  const { data, isLoading, isError } = useLatestSentiment(marketId)

  return (
    <Card>
      <SectionLabel className="mb-[11px]">Latest sentiment · sentiment_scores</SectionLabel>

      {isError ? (
        <ErrorState message="Failed to load sentiment." />
      ) : isLoading ? (
        <div className="py-4 text-center text-[11px] text-text-muted">Loading…</div>
      ) : !data ? (
        <Empty message="No sentiment score yet for this market." />
      ) : (
        <>
          <div className="mb-3 flex items-center gap-2.5">
            <span className={`text-[18px] font-semibold ${LABEL_COLOR[data.label]}`}>{data.label}</span>
            <span className="rounded bg-purple-dim px-2 py-0.5 font-mono text-[11px] text-purple">
              conf {data.confidence.toFixed(2)}
            </span>
          </div>

          <div className="my-1.5 h-[7px] overflow-hidden rounded bg-bg4" aria-hidden="true">
            <span
              className="block h-full rounded bg-accent"
              style={{ width: `${Math.round(clamp01(data.confidence) * 100)}%` }}
            />
          </div>
          <div className="font-mono text-[10px] text-text-muted">
            confidence {data.confidence.toFixed(2)} · {data.articleCount} articles
          </div>

          <p className="mt-2.5 text-[12px] leading-relaxed text-text-primary">{data.reasoning}</p>

          <div className="mt-3">
            {KV_ROWS.map((k) => (
              <div
                key={k}
                className="flex items-center justify-between border-b border-border py-[5px] text-[11px] last:border-b-0"
              >
                <span className="text-text-muted">{k}</span>
                <span className="font-mono text-text-secondary">{kvValue(k, data)}</span>
              </div>
            ))}
          </div>
        </>
      )}
    </Card>
  )
}

function clamp01(n: number): number {
  return Math.min(1, Math.max(0, n))
}

function kvValue(
  key: (typeof KV_ROWS)[number],
  data: { modelUsed: string; articleCount: number; scoredAt: string },
): string {
  switch (key) {
    case 'model_used':
      return data.modelUsed
    case 'article_count':
      return String(data.articleCount)
    case 'scored_at':
      return new Date(data.scoredAt).toLocaleString()
  }
}
