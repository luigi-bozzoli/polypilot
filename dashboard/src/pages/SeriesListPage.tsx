import { Link } from 'react-router-dom'
import { useSeriesList } from '../features/series/useSeriesQueries'
import { SeriesCard } from '../components/series/SeriesCard'
import { Empty, ErrorState, Loading } from '../components/series/ListStates'

export function SeriesListPage() {
  const { data, isLoading, isError, error, refetch, isFetching } = useSeriesList()

  return (
    <div className="min-h-screen bg-bg0">
      <div className="mx-auto max-w-3xl px-6 py-10">
        <div className="mb-1 flex items-center justify-between">
          <h1 className="text-[20px] font-medium text-text-primary">Series</h1>
          <Link to="/health" className="text-[12px] text-text-secondary hover:text-text-primary">
            Health →
          </Link>
        </div>
        <p className="mb-6 text-[12px] text-text-muted">
          Tracked event series and their current market{isFetching ? ' · refreshing…' : ''}
        </p>

        {isLoading && <Loading label="Loading series…" />}

        {isError && (
          <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />
        )}

        {data && data.length === 0 && <Empty message="No series are being tracked yet." />}

        {data && data.length > 0 && (
          <div className="flex flex-col gap-3">
            {data.map((series) => (
              <SeriesCard key={series.id} series={series} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
