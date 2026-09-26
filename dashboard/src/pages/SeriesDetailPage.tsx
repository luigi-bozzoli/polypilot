import { Link, useParams } from 'react-router-dom'
import { useMemo } from 'react'
import { useSeriesDetail } from '../features/series/useSeriesQueries'
import { Section } from '../components/series/Section'
import { Empty, ErrorState, Loading } from '../components/series/ListStates'

export function SeriesDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data, isLoading, isError, error, refetch } = useSeriesDetail(id)

  const { open, history } = useMemo(() => {
    const all = data?.markets ?? []
    return {
      open: all.filter((m) => m.status === 'OPEN'),
      history: all.filter((m) => m.status !== 'OPEN'),
    }
  }, [data])

  return (
    <div className="min-h-screen bg-bg0">
      <div className="mx-auto max-w-3xl px-6 py-10">
        <Link to="/series" className="text-[12px] text-text-secondary hover:text-text-primary">
          ← All series
        </Link>

        {isLoading && <div className="mt-6"><Loading label="Loading series…" /></div>}

        {isError && (
          <div className="mt-6">
            <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />
          </div>
        )}

        {data && (
          <>
            <div className="mb-6 mt-3">
              <div className="flex items-center gap-2">
                <h1 className="text-[20px] font-medium text-text-primary">{data.title}</h1>
                <span className="rounded border border-border bg-bg3 px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-text-muted">
                  {data.recurrence}
                </span>
              </div>
              <div className="mt-0.5 font-mono text-[11px] text-text-muted">{data.ticker}</div>
            </div>

            {data.markets.length === 0 ? (
              <Empty message="No markets have been synced for this series yet." />
            ) : (
              <>
                <Section title="Open" markets={open} seriesId={data.id} />
                <Section title="History" markets={history} seriesId={data.id} />
              </>
            )}
          </>
        )}
      </div>
    </div>
  )
}