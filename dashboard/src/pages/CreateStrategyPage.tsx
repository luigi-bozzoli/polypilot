import { useNavigate } from 'react-router-dom'
import { useConditionFields, useCreateStrategy } from '../features/strategies/useStrategiesQueries'
import { useStrategyForm } from '../features/strategies/useStrategyForm'
import { Loading, ErrorState } from '../components/series/ListStates'
import { StepTabs } from '../components/strategies/StepTabs'
import { StepBasics } from '../components/strategies/StepBasics'
import { StepEntryRules } from '../components/strategies/StepEntryRules'
import { StepRiskSchedule } from '../components/strategies/StepRiskSchedule'

export function CreateStrategyPage() {
  const navigate = useNavigate()

  const {
    data: conditionFieldsCatalog,
    isLoading: conditionFieldsLoading,
    isError: conditionFieldsError,
    error: conditionFieldsErrorObj,
    refetch: refetchConditionFields,
  } = useConditionFields()
  const createStrategy = useCreateStrategy()

  const form = useStrategyForm()
  const effectiveRoot = form.effectiveRoot(!!conditionFieldsCatalog)

  /**
   * Steps 2/3 are only reachable after `validateStep1` requires a non-empty name, so getting
   * here always means there's in-progress state worth confirming before discarding it — no
   * separate dirty-check is needed. Nothing is persisted until `handleSubmit`, so abandoning
   * just means navigating away; there's no draft to clean up.
   */
  const handleCancel = () => {
    if (window.confirm('Discard this strategy and return to the Strategies list? Your progress will be lost.')) {
      navigate('/strategies')
    }
  }

  const handleBack = () => {
    if (form.step === 1) navigate('/strategies')
    else form.handleBack()
  }

  const handleSubmit = async () => {
    if (!conditionFieldsCatalog || !effectiveRoot || !form.validateStep3()) return

    const request = form.buildRequest(conditionFieldsCatalog, effectiveRoot)

    try {
      await createStrategy.mutateAsync(request)
      navigate('/strategies')
    } catch (err) {
      form.setStepErrors({ submit: (err as Error).message })
    }
  }

  return (
    <div className="mx-auto max-w-[1100px] px-6 py-10">
      <div className="flex items-center gap-3 border-b border-border pb-5">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-accent-border bg-accent-dim">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-accent">
            <path d="M2 4h12M4 8h8M6 12h4" />
          </svg>
        </div>
        <div>
          <div className="text-[15px] font-medium text-text-primary">Create new strategy</div>
          <div className="mt-px text-[12px] text-text-muted">Define rules, risk limits, and schedule</div>
        </div>
      </div>

      <StepTabs current={form.step} />

      <div className="py-6">
        {conditionFieldsLoading || !conditionFieldsCatalog || !effectiveRoot ? (
          conditionFieldsError ? (
            <ErrorState message={(conditionFieldsErrorObj as Error).message} onRetry={() => void refetchConditionFields()} />
          ) : (
            <Loading label="Loading condition catalog…" />
          )
        ) : (
          <>
            {form.step === 1 && (
              <StepBasics
                name={form.name}
                description={form.description}
                seriesId={form.seriesId}
                onNameChange={form.setName}
                onDescriptionChange={form.setDescription}
                onSeriesIdChange={form.setSeriesId}
                error={form.stepErrors.name}
                seriesError={form.stepErrors.seriesId}
              />
            )}
            {form.step === 2 && (
              <StepEntryRules
                strategyName={form.name}
                tokenSide={form.tokenSide}
                orderType={form.orderType}
                root={effectiveRoot}
                dryRun={form.dryRun}
                maxBetSizeForPreview={form.maxBetSize}
                catalogs={conditionFieldsCatalog}
                onTokenSideChange={form.setTokenSide}
                onOrderTypeChange={form.setOrderType}
                onRootChange={form.setRoot}
                onDryRunChange={form.setDryRun}
                error={form.stepErrors.conditions}
              />
            )}
            {form.step === 3 && (
              <StepRiskSchedule
                maxBetSize={form.maxBetSize}
                maxDailyExposure={form.maxDailyExposure}
                stopLossThreshold={form.stopLossThreshold}
                cronExpression={form.cronExpression}
                onMaxBetSizeChange={form.setMaxBetSize}
                onMaxDailyExposureChange={form.setMaxDailyExposure}
                onStopLossThresholdChange={form.setStopLossThreshold}
                onCronExpressionChange={form.setCronExpression}
                errors={form.stepErrors}
              />
            )}
          </>
        )}
      </div>

      <div className="flex items-center gap-2.5 border-t border-border pt-4">
        <span className="inline-block rounded px-2 py-0.5 font-mono text-[10px] text-yellow bg-yellow-dim border border-[rgba(240,168,50,0.25)]">
          Step {form.step} of 3
        </span>
        {form.stepErrors.submit && <span className="text-[11px] text-red">{form.stepErrors.submit}</span>}
        <div className="ml-auto flex items-center gap-2.5">
          {form.step > 1 && (
            <button
              type="button"
              onClick={handleCancel}
              className="rounded-md px-3 py-2 text-[12px] font-medium text-text-muted hover:text-red"
            >
              Cancel
            </button>
          )}
          <button
            type="button"
            onClick={handleBack}
            className="rounded-md border border-border-mid bg-bg3 px-4 py-2 text-[12px] font-medium text-text-secondary hover:bg-bg4 hover:text-text-primary"
          >
            {form.step === 1 ? 'Cancel' : '← Back'}
          </button>
        </div>
        {form.step < 3 ? (
          <button
            type="button"
            onClick={() => form.handleNext(effectiveRoot)}
            className="rounded-md border border-accent bg-accent px-4 py-2 text-[12px] font-medium text-black hover:opacity-88"
          >
            Next: {form.step === 1 ? 'Entry rules' : 'Risk & schedule'} →
          </button>
        ) : (
          <button
            type="button"
            onClick={() => void handleSubmit()}
            disabled={createStrategy.isPending}
            className="rounded-md border border-accent bg-accent px-4 py-2 text-[12px] font-medium text-black hover:opacity-88 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {createStrategy.isPending ? 'Creating…' : 'Create strategy'}
          </button>
        )}
      </div>
    </div>
  )
}
