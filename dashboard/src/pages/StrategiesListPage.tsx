import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { useStrategiesList } from '../features/strategies/useStrategiesQueries'
import { StrategyCard } from '../components/strategies/StrategyCard'
import { Empty, ErrorState, Loading } from '../components/series/ListStates'

export function StrategiesListPage() {
  const { data, isLoading, isError, error, refetch } = useStrategiesList()

  return (
    <div className="min-h-screen bg-bg0">
      <div className="mx-auto max-w-3xl px-6 py-10">
        <div className="mb-1 flex items-end justify-between gap-4">
          <div>
            <h1 className="text-[20px] font-medium text-text-primary">Strategies</h1>
            {data && (
              <p className="mt-1 text-[12px] text-text-muted">
                {data.length} total · {data.filter((s) => s.enabled).length} enabled · all dry-run
              </p>
            )}
          </div>
          <Link
            to="/strategies/new"
            className="flex items-center gap-1.5 rounded-md border border-accent bg-accent px-3.5 py-2 text-[12px] font-medium text-black hover:opacity-90"
          >
            <Plus className="h-3.5 w-3.5" aria-hidden="true" /> New strategy
          </Link>
        </div>

        <div className="mt-6 flex flex-col gap-3">
          {isLoading && <Loading label="Loading strategies…" />}

          {isError && <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />}

          {data && data.length === 0 && <Empty message="No strategies yet. Create one to get started." />}

          {data?.map((strategy) => <StrategyCard key={strategy.id} strategy={strategy} />)}
        </div>
      </div>
    </div>
  )
}
