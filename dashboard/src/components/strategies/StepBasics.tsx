import { FormField } from '../auth/FormField'
import { Loading, ErrorState } from '../series/ListStates'
import { useSeriesList } from '../../features/series/useSeriesQueries'

type StepBasicsProps = {
  name: string
  description: string
  seriesId: string | null
  onNameChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onSeriesIdChange: (value: string) => void
  error?: string
  seriesError?: string
}

export function StepBasics({
  name,
  description,
  seriesId,
  onNameChange,
  onDescriptionChange,
  onSeriesIdChange,
  error,
  seriesError,
}: StepBasicsProps) {
  const { data: series, isLoading, isError, error: seriesQueryError, refetch } = useSeriesList()

  return (
    <div>
      <FormField
        label="Strategy name"
        placeholder="BTC Bullish Momentum"
        value={name}
        onChange={(e) => onNameChange(e.target.value)}
        error={error}
        autoFocus
      />

      <div className="mb-4">
        <label htmlFor="strategy-description" className="mb-1.5 block text-[10px] uppercase tracking-wider text-text-muted">
          Description <span className="normal-case tracking-normal text-text-muted">(optional)</span>
        </label>
        <textarea
          id="strategy-description"
          rows={3}
          className="w-full resize-none rounded-md border border-border bg-bg3 px-3 py-2.5 font-mono text-[12px] text-text-primary outline-none transition-colors placeholder:text-text-muted focus:border-accent-border"
          placeholder="Bets YES when price is cheap and AI sentiment is strongly bullish."
          value={description}
          onChange={(e) => onDescriptionChange(e.target.value)}
        />
        <p className="mt-1 text-[11px] text-text-muted">Shown on the strategies list and detail screens.</p>
      </div>

      <div className="mb-4">
        <label htmlFor="strategy-series" className="mb-1.5 block text-[10px] uppercase tracking-wider text-text-muted">
          Series
        </label>
        {isLoading ? (
          <Loading label="Loading series…" />
        ) : isError ? (
          <ErrorState message={(seriesQueryError as Error).message} onRetry={() => void refetch()} />
        ) : (
          <>
            <select
              id="strategy-series"
              className="w-full rounded-md border border-border bg-bg3 px-3 py-2.5 font-mono text-[12px] text-text-primary outline-none transition-colors focus:border-accent-border"
              value={seriesId ?? ''}
              onChange={(e) => onSeriesIdChange(e.target.value)}
            >
              <option value="" disabled>
                Select a series…
              </option>
              {series?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.title}
                  {s.currentMarket ? '' : ' (no open market yet)'}
                </option>
              ))}
            </select>
            {seriesError && <p className="mt-1 text-[11px] text-red">{seriesError}</p>}
            <p className="mt-1 text-[11px] text-text-muted">
              The strategy evaluates against this series's current open market.
            </p>
          </>
        )}
      </div>
    </div>
  )
}
