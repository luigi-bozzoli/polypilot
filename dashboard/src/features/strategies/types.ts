/**
 * Frontend contracts for the Create Strategy flow (`pages/StrategiesListPage.tsx`,
 * `components/strategies/*`, mock `dashboard/mocks/01-create-strategy.html`).
 *
 * Mirrors `scratchpad_shared/create-strategy-api-contracts.md` field-for-field so
 * mock data here is drop-in replaceable with real responses once
 * `GET /strategies/condition-fields` and `POST /strategies` exist. Neither
 * endpoint is called yet — see `strategiesApi.ts`.
 */

export type TokenSide = 'YES' | 'NO'
export type OrderType = 'GTC' | 'GTD' | 'FOK' | 'FAK'
export type SentimentLabel = 'BULLISH' | 'NEUTRAL' | 'BEARISH'

export type CompareOperator = 'EQ' | 'NEQ' | 'GT' | 'GTE' | 'LT' | 'LTE'
export type BooleanOperator = 'AND' | 'OR' | 'XOR'
export type UnaryOperator = 'NOT'

/* ------------------------------------------------------------------ */
/* Rule tree — wire format for `strategies.rule_tree`                 */
/* ------------------------------------------------------------------ */

export type MarketField = 'up_price' | 'down_price' | 'volume_24h' | 'liquidity' | 'sentiment' | 'sentiment.confidence'

export type MarketFieldNode = {
  type: 'MARKET_FIELD'
  field: MarketField
  operator: CompareOperator
  value: number | SentimentLabel
}

/** Right-hand side of an `INDICATOR`-vs-`INDICATOR` comparison — another indicator's output. */
export type IndicatorValueRef = {
  indicatorKey: string
  params: Record<string, string>
  outputField: string
}

export type IndicatorNode = {
  type: 'INDICATOR'
  indicatorKey: string
  params: Record<string, string>
  outputField: string
  operator: CompareOperator
  /** A bare number for indicator-vs-value, or a nested indicator ref for indicator-vs-indicator. */
  value: number | IndicatorValueRef
}

export type BooleanNode = {
  type: 'BOOLEAN'
  operator: BooleanOperator
  left: RuleNode
  right: RuleNode
}

export type UnaryBooleanNode = {
  type: 'UNARY_BOOLEAN'
  operator: UnaryOperator
  child: RuleNode
}

/** A leaf (`MARKET_FIELD` / `INDICATOR`) or internal (`BOOLEAN` / `UNARY_BOOLEAN`) node. */
export type RuleNode = MarketFieldNode | IndicatorNode | BooleanNode | UnaryBooleanNode

/* ------------------------------------------------------------------ */
/* GET /strategies/condition-fields                                   */
/* ------------------------------------------------------------------ */

/** Value range a market-field leaf's threshold lives in — display/UI hint only. */
export type MarketFieldValueScale = 'OSCILLATOR_0_1' | 'VOLUME'

export type MarketFieldCatalogEntry = {
  field: MarketField
  name: string
  dataType: 'NUMBER' | 'ENUM'
  valueScale?: MarketFieldValueScale
  allowedValues?: SentimentLabel[]
  operators: CompareOperator[]
}

export type IndicatorParameterDataType = 'INTEGER' | 'NUMBER' | 'ENUM'

/** Mirrors `IndicatorParameterView` (orchestrator). */
export type IndicatorParameterView = {
  key: string
  name: string
  description?: string
  dataType: IndicatorParameterDataType
  required: boolean
  default: string | null
  allowedValues?: string[]
  constraints?: Record<string, number | string>
  universal: boolean
}

export type IndicatorValueScale = 'PRICE' | 'OSCILLATOR_0_100' | 'UNBOUNDED' | 'VOLUME'

/** Mirrors `IndicatorOutputView` (orchestrator). */
export type IndicatorOutputView = {
  key: string
  name: string
  valueScale: IndicatorValueScale
  default: boolean
}

export type IndicatorCategory = 'TREND' | 'MOMENTUM' | 'VOLATILITY' | 'VOLUME'

export type IndicatorCatalogEntry = {
  indicatorKey: string
  name: string
  abbreviation: string
  category: IndicatorCategory
  parameters: IndicatorParameterView[]
  outputs: IndicatorOutputView[]
  operators: CompareOperator[]
}

export type ConditionFieldsCatalog = {
  marketFields: MarketFieldCatalogEntry[]
  indicators: IndicatorCatalogEntry[]
}

/* ------------------------------------------------------------------ */
/* GET /api/indicators — real orchestrator endpoint                   */
/* ------------------------------------------------------------------ */

/**
 * Mirrors `IndicatorView` (orchestrator) exactly. Distinct from
 * `IndicatorCatalogEntry` above: the real endpoint has no per-indicator
 * `operators` list (the contract mock invented one) and uses `key` instead
 * of `indicatorKey`. Step 2's indicator leaves fall back to the full
 * `CompareOperator` set since the API doesn't scope allowed operators.
 */
export type IndicatorApiView = {
  key: string
  name: string
  abbreviation: string
  category: IndicatorCategory
  description?: string
  parameters: IndicatorParameterView[]
  outputs: IndicatorOutputView[]
}

/** Body of `GET /api/indicators`. */
export type IndicatorCatalogView = {
  indicators: IndicatorApiView[]
}

/* ------------------------------------------------------------------ */
/* POST /strategies                                                   */
/* ------------------------------------------------------------------ */

export type CreateStrategyRequest = {
  name: string
  description: string | null
  tokenSide: TokenSide
  orderType: OrderType
  ruleTree: RuleNode
  maxBetSize: number
  maxDailyExposure: number
  stopLossThreshold: number | null
  cronExpression: string
  /** The series this strategy evaluates against — see `features/series/types.ts`. */
  seriesId: string
  dryRun: boolean
}

/** `enabled` is server-assigned (defaults to `true` on create); change it via `setStrategyEnabled`. */
export type StrategyView = CreateStrategyRequest & {
  id: string
  enabled: boolean
  /** Denormalized alongside `seriesId` so the list/detail screens don't need a second fetch. */
  seriesTitle: string
  createdAt: string
  updatedAt: string
  /** Total orders placed by this strategy, batch-computed server-side. */
  tradeCount: number
}

/** Body of `PUT /strategies/{id}` — same editable fields as create; a full replacement. */
export type UpdateStrategyRequest = CreateStrategyRequest

/* ------------------------------------------------------------------ */
/* GET /strategies/{id}/orders                                        */
/* ------------------------------------------------------------------ */

/** Mirrors the backend's real `OrderStatus` enum — see `contracts/strategy-recent-orders.md`. */
export type OrderStatus = 'PENDING' | 'OPEN' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'FAILED'

/** One row of the strategy-scoped recent-orders feed. Mirrors `StrategyOrderView` (orchestrator). */
export type StrategyOrderRow = {
  id: string
  /** Order creation instant, ISO-8601 UTC. */
  placedAt: string
  marketId: string
  marketQuestion: string
  side: TokenSide
  sizeRequested: number
  sizeFilled: number
  price: number
  status: OrderStatus
  /** true = simulated. Every row is `true` today — no live order-placement path exists yet. */
  isDryRun: boolean
}
