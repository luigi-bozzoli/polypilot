import { useNavigate, useParams } from 'react-router-dom'
import { raiseRuleTree } from '../features/strategies/conditionTree'
import { useConditionFields, useStrategyDetail, useUpdateStrategy } from '../features/strategies/useStrategiesQueries'
import { useStrategyForm } from '../features/strategies/useStrategyForm'
import type { ConditionFieldsCatalog, StrategyView } from '../features/strategies/types'
import { Loading, ErrorState } from '../components/series/ListStates'
import { StepTabs } from '../components/strategies/StepTabs'
import { StepBasics } from '../components/strategies/StepBasics'
import { StepEntryRules } from '../components/strategies/StepEntryRules'
import { StepRiskSchedule } from '../components/strategies/StepRiskSchedule'

/**
 * Edit Strategy page — route `/strategies/:id/edit`, reached from the disabled-no-more "Edit"
 * button on `StrategyDetailPage`. Reuses the same step components as `CreateStrategyPage` via
 * `useStrategyForm`; the only real difference is where the initial field values come from and
 * which mutation submits the result.
 */
export function EditStrategyPage() {
  const { id } = useParams<{ id: string }>()
  const { data: strategy, isLoading: strategyLoading, isError: strategyError, error: strategyErrorObj, refetch: refetchStrategy } =
    useStrategyDetail(id)
  const {
    data: conditionFieldsCatalog,
    isLoading: conditionFieldsLoading,
    isError: conditionFieldsError,
    error: conditionFieldsErrorObj,
    refetch: refetchConditionFields,
  } = useConditionFields()

  if (strategyLoading || conditionFieldsLoading) {
    return (
      <div className="mx-auto max-w-[1100px] px-6 py-10">
        <Loading label="Loading strategy…" />
      </div>
    )
  }

  if (strategyError) {
    return (
      <div className="mx-auto max-w-[1100px] px-6 py-10">
        <ErrorState message={(strategyErrorObj as Error).message} onRetry={() => void refetchStrategy()} />
      </div>
    )
  }

  if (conditionFieldsError) {
    return (
      <div className="mx-auto max-w-[1100px] px-6 py-10">
        <ErrorState message={(conditionFieldsErrorObj as Error).message} onRetry={() => void refetchConditionFields()} />
      </div>
    )
  }

  if (!strategy || !conditionFieldsCatalog || !id) {
    return null
  }

  // Mounted fresh only once both resources are loaded, so `useStrategyForm`'s initial values —
  // derived from `strategy` — are only ever computed once, on this component's first render.
  return <EditStrategyForm id={id} strategy={strategy} catalogs={conditionFieldsCatalog} />
}

function EditStrategyForm({ id, strategy, catalogs }: { id: string; strategy: StrategyView; catalogs: ConditionFieldsCatalog }) {
  const navigate = useNavigate()
  const updateStrategy = useUpdateStrategy(id)

  const form = useStrategyForm({
    name: strategy.name,
    description: strategy.description ?? '',
    seriesId: strategy.seriesId,
    tokenSide: strategy.tokenSide,
    orderType: strategy.orderType,
    root: raiseRuleTree(strategy.ruleTree),
    dryRun: strategy.dryRun,
    maxBetSize: String(strategy.maxBetSize),
    maxDailyExposure: String(strategy.maxDailyExposure),
    stopLossThreshold: strategy.stopLossThreshold != null ? String(strategy.stopLossThreshold) : '',
    cronExpression: strategy.cronExpression,
  })
  const effectiveRoot = form.effectiveRoot(true)

  const handleBack = () => {
    if (form.step === 1) navigate(`/strategies/${id}`)
    else form.handleBack()
  }

  const handleSubmit = async () => {
    if (!effectiveRoot || !form.validateStep3()) return

    const request = form.buildRequest(catalogs, effectiveRoot)

    try {
      await updateStrategy.mutateAsync(request)
      navigate(`/strategies/${id}`)
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
          <div className="text-[15px] font-medium text-text-primary">Edit strategy</div>
          <div className="mt-px text-[12px] text-text-muted">{strategy.name}</div>
        </div>
      </div>

      <StepTabs current={form.step} />

      <div className="py-6">
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
        {form.step === 2 && effectiveRoot && (
          <StepEntryRules
            strategyName={form.name}
            tokenSide={form.tokenSide}
            orderType={form.orderType}
            root={effectiveRoot}
            dryRun={form.dryRun}
            maxBetSizeForPreview={form.maxBetSize}
            catalogs={catalogs}
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
      </div>

      <div className="flex items-center gap-2.5 border-t border-border pt-4">
        <span className="inline-block rounded px-2 py-0.5 font-mono text-[10px] text-yellow bg-yellow-dim border border-[rgba(240,168,50,0.25)]">
          Step {form.step} of 3
        </span>
        {form.stepErrors.submit && <span className="text-[11px] text-red">{form.stepErrors.submit}</span>}
        <div className="ml-auto flex items-center gap-2.5">
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
            disabled={updateStrategy.isPending}
            className="rounded-md border border-accent bg-accent px-4 py-2 text-[12px] font-medium text-black hover:opacity-88 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {updateStrategy.isPending ? 'Saving…' : 'Save changes'}
          </button>
        )}
      </div>
    </div>
  )
}
