import { Link } from 'react-router-dom'
import { Card, SectionLabel } from '../market/primitives'
import { Empty, ErrorState, Loading } from '../series/ListStates'
import { useStrategiesList } from '../../features/strategies/useStrategiesQueries'

const MAX_ROWS = 4

/**
 * "Active strategies" (mock `10-overview.html`) — top strategies from `GET /api/strategies`,
 * condensed to one line each (dot + name + badges + trade count). Same data `StrategyCard`
 * renders full-size on the Strategies list.
 */
export function ActiveStrategiesCard() {
  const { data, isLoading, isError, error, refetch } = useStrategiesList()

  return (
    <Card>
      <SectionLabel className="mb-[11px]">Active strategies</SectionLabel>

      {isLoading && <Loading label="Loading strategies…" />}

      {isError && <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />}

      {data && data.length === 0 && <Empty message="No strategies yet." />}

      {data && data.length > 0 && (
        <div>
          {data.slice(0, MAX_ROWS).map((strategy) => (
            <Link
              key={strategy.id}
              to={`/strategies/${strategy.id}`}
              className="flex items-center gap-2.5 border-b border-border py-2.5 text-[12.5px] last:border-b-0 hover:bg-bg2"
            >
              <span className={`h-2 w-2 shrink-0 rounded-full ${strategy.enabled ? 'bg-accent' : 'bg-text-muted'}`} />
              <span className="flex-1 truncate text-text-primary">{strategy.name}</span>
              {strategy.dryRun && (
                <span className="rounded border border-[rgba(240,168,50,0.3)] bg-yellow-dim px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-yellow">
                  dry
                </span>
              )}
              <span className="rounded border border-border-mid bg-bg3 px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-text-secondary">
                {strategy.orderType}
              </span>
              <span className="font-mono text-[11px] text-text-muted">
                {strategy.tradeCount} trade{strategy.tradeCount === 1 ? '' : 's'}
              </span>
            </Link>
          ))}
        </div>
      )}
    </Card>
  )
}
