import { useLatestNews } from '../../features/market/useLatestNews'
import { Card, Empty, ErrorState, SectionLabel } from './primitives'

/** "News summary · news_summaries" (mock `13-market-detail.html`). */
export function NewsSummaryCard({ marketId }: { marketId: string }) {
  const { data, isLoading, isError } = useLatestNews(marketId)

  return (
    <Card>
      <SectionLabel className="mb-[11px]">News summary · news_summaries</SectionLabel>

      {isError ? (
        <ErrorState message="Failed to load news summary." />
      ) : isLoading ? (
        <div className="py-4 text-center text-[11px] text-text-muted">Loading…</div>
      ) : !data ? (
        <Empty message="No news summary yet for this market." />
      ) : (
        <>
          <p className="text-[12px] leading-relaxed text-text-primary">{data.summary}</p>
          <div className="my-2.5 font-mono text-[10px] text-text-muted">
            fetched {formatTime(data.fetchedAt)} · expires {formatTime(data.expiresAt)} · {data.modelUsed}
          </div>

          <SectionLabel className="mt-3">Sources · {data.sources.length}</SectionLabel>
          <div className="mt-1">
            {data.sources.length === 0 ? (
              <div className="py-2.5 text-[11px] text-text-muted">No articles found.</div>
            ) : (
              data.sources.map((source, i) => (
                <a
                  key={`${source.url}-${i}`}
                  href={source.url}
                  target="_blank"
                  rel="noreferrer"
                  className="block border-b border-border py-2.5 last:border-b-0 hover:text-accent"
                >
                  <div className="text-[12px] text-text-primary">{source.title}</div>
                  <div className="mt-1 font-mono text-[10px] text-text-muted">
                    {source.publisher} · {formatTime(source.publishedAt)}
                  </div>
                </a>
              ))
            )}
          </div>
        </>
      )}
    </Card>
  )
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } catch {
    return iso
  }
}
