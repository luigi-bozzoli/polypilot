import { useState } from 'react'
import { buildRuleTree, emptyGroup } from './conditionTree'
import type { GroupDraft } from './conditionTree'
import type { ConditionFieldsCatalog, CreateStrategyRequest, OrderType, TokenSide } from './types'

export type Step = 1 | 2 | 3

export type StrategyFormInitialValues = {
  name: string
  description: string
  seriesId: string | null
  tokenSide: TokenSide
  orderType: OrderType
  root: GroupDraft
  dryRun: boolean
  maxBetSize: string
  maxDailyExposure: string
  stopLossThreshold: string
  cronExpression: string
}

/**
 * Field state, step navigation, and validation shared by the create- and edit-strategy
 * multi-step forms (`CreateStrategyPage`, `EditStrategyPage`). Stays agnostic of which mutation
 * eventually submits the built request and of navigation — callers own both, since create and
 * update hit different endpoints with different success routes.
 */
export function useStrategyForm(initial?: Partial<StrategyFormInitialValues>) {
  const [step, setStep] = useState<Step>(1)
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [seriesId, setSeriesId] = useState<string | null>(initial?.seriesId ?? null)
  const [tokenSide, setTokenSide] = useState<TokenSide>(initial?.tokenSide ?? 'YES')
  const [orderType, setOrderType] = useState<OrderType>(initial?.orderType ?? 'GTC')
  const [root, setRoot] = useState<GroupDraft | null>(initial?.root ?? null)
  const [dryRun, setDryRun] = useState(initial?.dryRun ?? true)
  const [maxBetSize, setMaxBetSize] = useState(initial?.maxBetSize ?? '100')
  const [maxDailyExposure, setMaxDailyExposure] = useState(initial?.maxDailyExposure ?? '1000')
  const [stopLossThreshold, setStopLossThreshold] = useState(initial?.stopLossThreshold ?? '0.15')
  const [cronExpression, setCronExpression] = useState(initial?.cronExpression ?? '0 */30 * * * *')

  const [stepErrors, setStepErrors] = useState<Record<string, string>>({})

  /** Lazily seeds an empty root once a condition catalog exists to build leaves against. */
  const effectiveRoot = (catalogLoaded: boolean): GroupDraft | null => root ?? (catalogLoaded ? emptyGroup() : null)

  const validateStep1 = () => {
    const errors: Record<string, string> = {}
    if (!name.trim()) errors.name = 'Strategy name is required'
    if (!seriesId) errors.seriesId = 'A series is required'
    setStepErrors(errors)
    return Object.keys(errors).length === 0
  }

  const validateStep2 = (currentRoot: GroupDraft | null) => {
    if (!currentRoot || currentRoot.children.length === 0) {
      setStepErrors({ conditions: 'At least one entry condition is required' })
      return false
    }
    setStepErrors({})
    return true
  }

  const validateStep3 = () => {
    const errors: Record<string, string> = {}
    const bet = Number(maxBetSize)
    const daily = Number(maxDailyExposure)
    if (!(bet > 0)) errors.maxBetSize = 'Must be greater than 0'
    if (!(daily > 0)) errors.maxDailyExposure = 'Must be greater than 0'
    if (bet > 0 && daily > 0 && daily < bet) errors.maxDailyExposure = 'Must be ≥ max bet size'
    if (stopLossThreshold) {
      const sl = Number(stopLossThreshold)
      if (!(sl >= 0 && sl <= 1)) errors.stopLossThreshold = 'Must be between 0 and 1'
    }
    if (!cronExpression) errors.cronExpression = 'A run schedule is required'
    setStepErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleNext = (currentRoot: GroupDraft | null) => {
    if (step === 1 && validateStep1()) setStep(2)
    else if (step === 2 && validateStep2(currentRoot)) setStep(3)
  }

  const handleBack = () => {
    setStepErrors({})
    setStep((s) => (s === 3 ? 2 : 1) as Step)
  }

  const buildRequest = (catalogs: ConditionFieldsCatalog, currentRoot: GroupDraft): CreateStrategyRequest => {
    if (!seriesId) throw new Error('buildRequest called before a series was selected')
    return {
      name: name.trim(),
      description: description.trim() || null,
      seriesId,
      tokenSide,
      orderType,
      ruleTree: buildRuleTree(currentRoot, catalogs),
      maxBetSize: Number(maxBetSize),
      maxDailyExposure: Number(maxDailyExposure),
      stopLossThreshold: stopLossThreshold ? Number(stopLossThreshold) : null,
      cronExpression,
      dryRun,
    }
  }

  return {
    step,
    setStep,
    name,
    setName,
    description,
    setDescription,
    seriesId,
    setSeriesId,
    tokenSide,
    setTokenSide,
    orderType,
    setOrderType,
    root,
    setRoot,
    effectiveRoot,
    dryRun,
    setDryRun,
    maxBetSize,
    setMaxBetSize,
    maxDailyExposure,
    setMaxDailyExposure,
    stopLossThreshold,
    setStopLossThreshold,
    cronExpression,
    setCronExpression,
    stepErrors,
    setStepErrors,
    validateStep3,
    handleNext,
    handleBack,
    buildRequest,
  }
}
