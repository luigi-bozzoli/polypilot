import type {
  BooleanOperator,
  CompareOperator,
  ConditionFieldsCatalog,
  IndicatorCatalogEntry,
  IndicatorNode,
  IndicatorValueRef,
  MarketField,
  MarketFieldCatalogEntry,
  MarketFieldNode,
  RuleNode,
  SentimentLabel,
} from './types'

/**
 * Editable draft mirroring `RuleNode`'s recursive shape (`features/strategies/types.ts`)
 * before it's lowered into the wire format. `negate` lives on every individual condition
 * (leaf or group) rather than on the group as a whole — each condition can be independently
 * wrapped in `NOT`. The boolean operator connecting conditions is chosen per adjacent pair
 * within a group (`GroupDraft.operators`, one entry between each pair of `children`), not
 * shared uniformly by the whole group, so a group can mix e.g. `A AND B OR C`.
 */
export type MarketFieldLeafDraft = {
  kind: 'MARKET_FIELD'
  id: string
  field: MarketField
  operator: CompareOperator
  value: string
  negate: boolean
}

export type IndicatorLeafDraft = {
  kind: 'INDICATOR'
  id: string
  indicatorKey: string
  /** Raw string per parameter key; coerced to number for INTEGER/NUMBER params on submit. */
  params: Record<string, string>
  outputField: string
  operator: CompareOperator
  /** Right-hand side of the comparison: a literal numeric `value`, or another indicator's output (`compareIndicator`). */
  compareTo: 'VALUE' | 'INDICATOR'
  value: string
  compareIndicator: IndicatorComparisonTarget | null
  negate: boolean
}

/** Config for the right-hand indicator in an `INDICATOR`-vs-`INDICATOR` comparison. */
export type IndicatorComparisonTarget = {
  indicatorKey: string
  params: Record<string, string>
  outputField: string
}

export type LeafDraft = MarketFieldLeafDraft | IndicatorLeafDraft

export type GroupDraft = {
  kind: 'GROUP'
  id: string
  /** Operator between `children[i]` and `children[i+1]`; length is always `children.length - 1`. */
  operators: BooleanOperator[]
  negate: boolean
  children: ConditionDraft[]
}

export type ConditionDraft = LeafDraft | GroupDraft

/**
 * The single `GET /strategies/condition-fields` response, threaded through the whole
 * condition-builder UI as-is — market fields and indicators both come from it, never
 * from the standalone `/api/indicators` catalog (see `strategiesApi.ts`).
 */
export type ConditionCatalogs = ConditionFieldsCatalog

const OPERATOR_SYMBOL: Record<CompareOperator, string> = {
  EQ: '==',
  NEQ: '!=',
  GT: '>',
  GTE: '>=',
  LT: '<',
  LTE: '<=',
}

export function findIndicator(indicators: IndicatorCatalogEntry[], key: string): IndicatorCatalogEntry | undefined {
  return indicators.find((i) => i.indicatorKey === key)
}

function coerceMarketFieldValue(field: MarketField, marketFields: MarketFieldCatalogEntry[], raw: string): number | SentimentLabel {
  const entry = marketFields.find((f) => f.field === field)
  if (entry?.dataType === 'ENUM') return raw as SentimentLabel
  const numeric = Number(raw)
  return Number.isFinite(numeric) ? numeric : 0
}

/**
 * The backend's `readParams` (`StrategyNodeDeserializer`) always reads param values as JSON
 * strings (`Map<String, String>`) — a JSON number fails with `IntNode.stringValue()`. So every
 * param goes on the wire as a string regardless of its catalog `dataType`; this just normalizes
 * numeric strings (trimming, etc. is not needed — raw input is already what the user typed).
 */
function coerceIndicatorParams(indicatorKey: string, indicators: IndicatorCatalogEntry[], params: Record<string, string>): Record<string, string> {
  const indicator = findIndicator(indicators, indicatorKey)
  const out: Record<string, string> = {}
  for (const [key, raw] of Object.entries(params)) {
    const paramDef = indicator?.parameters.find((p) => p.key === key)
    if (paramDef?.dataType === 'INTEGER' || paramDef?.dataType === 'NUMBER') {
      const numeric = Number(raw)
      out[key] = String(Number.isFinite(numeric) ? numeric : 0)
    } else {
      out[key] = raw
    }
  }
  return out
}

/** Builds the raw (un-negated) leaf node; `lowerNode` applies `leaf.negate` on top. */
function lowerLeafBase(leaf: LeafDraft, catalogs: ConditionCatalogs): RuleNode {
  if (leaf.kind === 'MARKET_FIELD') {
    return {
      type: 'MARKET_FIELD',
      field: leaf.field,
      operator: leaf.operator,
      value: coerceMarketFieldValue(leaf.field, catalogs.marketFields, leaf.value),
    }
  }
  const value: IndicatorNode['value'] =
    leaf.compareTo === 'INDICATOR' && leaf.compareIndicator
      ? {
          indicatorKey: leaf.compareIndicator.indicatorKey,
          params: coerceIndicatorParams(leaf.compareIndicator.indicatorKey, catalogs.indicators, leaf.compareIndicator.params),
          outputField: leaf.compareIndicator.outputField,
        }
      : coerceNumericValue(leaf.value)
  return {
    type: 'INDICATOR',
    indicatorKey: leaf.indicatorKey,
    params: coerceIndicatorParams(leaf.indicatorKey, catalogs.indicators, leaf.params),
    outputField: leaf.outputField,
    operator: leaf.operator,
    value,
  }
}

function coerceNumericValue(raw: string): number {
  const numeric = Number(raw)
  return Number.isFinite(numeric) ? numeric : 0
}

/**
 * Lowers one condition (leaf or group) into a `RuleNode`, applying its own `negate` last.
 * A group's children are folded left-to-right using the per-pair `operators` — there is no
 * implicit AND/OR precedence; nest a sub-group for explicit grouping/precedence.
 */
function lowerNode(node: ConditionDraft, catalogs: ConditionCatalogs): RuleNode {
  let base: RuleNode
  if (node.kind !== 'GROUP') {
    base = lowerLeafBase(node, catalogs)
  } else {
    if (node.children.length === 0) {
      throw new Error('Every group needs at least one condition')
    }
    base = node.children
      .slice(1)
      .reduce<RuleNode>(
        (acc, child, i) => ({ type: 'BOOLEAN', operator: node.operators[i], left: acc, right: lowerNode(child, catalogs) }),
        lowerNode(node.children[0], catalogs),
      )
  }
  return node.negate ? { type: 'UNARY_BOOLEAN', operator: 'NOT', child: base } : base
}

/** Lowers the root group draft into the `RuleNode` wire format sent as `CreateStrategyRequest.ruleTree`. */
export function buildRuleTree(root: GroupDraft, catalogs: ConditionCatalogs): RuleNode {
  return lowerNode(root, catalogs)
}

function describeLeaf(leaf: LeafDraft, catalogs: ConditionCatalogs): string {
  if (leaf.kind === 'MARKET_FIELD') {
    const entry = catalogs.marketFields.find((f) => f.field === leaf.field)
    const label = entry?.name ?? leaf.field
    const value = entry?.dataType === 'ENUM' ? `"${leaf.value}"` : leaf.value
    return `${label} ${OPERATOR_SYMBOL[leaf.operator]} ${value}`
  }
  const indicator = findIndicator(catalogs.indicators, leaf.indicatorKey)
  const paramSummary = Object.values(leaf.params).join(', ')
  const label = `${indicator?.abbreviation ?? leaf.indicatorKey}(${paramSummary}).${leaf.outputField}`
  const rhs = leaf.compareTo === 'INDICATOR' && leaf.compareIndicator ? describeComparisonTarget(leaf.compareIndicator, catalogs) : leaf.value || '0'
  return `${label} ${OPERATOR_SYMBOL[leaf.operator]} ${rhs}`
}

function describeComparisonTarget(target: IndicatorComparisonTarget, catalogs: ConditionCatalogs): string {
  const indicator = findIndicator(catalogs.indicators, target.indicatorKey)
  const paramSummary = Object.values(target.params).join(', ')
  return `${indicator?.abbreviation ?? target.indicatorKey}(${paramSummary}).${target.outputField}`
}

/**
 * Renders one condition (leaf or group) as one or more indented lines, its own `NOT` prefix
 * applied first. Groups render as a parenthesized block containing their children, each
 * preceded by its connecting operator (`group.operators[i - 1]`) except the first.
 */
function describeCondition(cond: ConditionDraft, catalogs: ConditionCatalogs, depth: number): string[] {
  const indent = '  '.repeat(depth)
  const negatePrefix = cond.negate ? 'NOT ' : ''

  if (cond.kind !== 'GROUP') {
    return [`${indent}${negatePrefix}${describeLeaf(cond, catalogs)}`]
  }

  if (cond.children.length === 0) return [`${indent}${negatePrefix}()`]

  const inner: string[] = []
  cond.children.forEach((child, i) => {
    const [first, ...rest] = describeCondition(child, catalogs, depth + 1)
    if (i === 0) {
      inner.push(first, ...rest)
    } else {
      inner.push(`${'  '.repeat(depth + 1)}${cond.operators[i - 1]} ${first.trimStart()}`, ...rest)
    }
  })

  return [`${indent}${negatePrefix}(`, ...inner, `${indent})`]
}

/**
 * Flattens the root group's children into preview lines: `IF` prefixes the first, each
 * subsequent condition is prefixed by its connecting operator, and every condition carries
 * its own `NOT`/parenthesization via `describeCondition`.
 */
export function formatConditionTree(root: GroupDraft, catalogs: ConditionCatalogs): string[] {
  const lines: string[] = []

  root.children.forEach((child, i) => {
    const [first, ...rest] = describeCondition(child, catalogs, 0)
    const prefix = i === 0 ? 'IF' : root.operators[i - 1]
    lines.push(`${prefix} ${first.trimStart()}`, ...rest)
  })

  return lines
}

/** A fresh root group — the default state for a new strategy's entry-conditions tree. */
export function emptyGroup(): GroupDraft {
  return { kind: 'GROUP', id: crypto.randomUUID(), operators: [], negate: false, children: [] }
}

export function defaultMarketFieldLeaf(catalog: ConditionFieldsCatalog): MarketFieldLeafDraft {
  const first = catalog.marketFields[0]
  return {
    kind: 'MARKET_FIELD',
    id: crypto.randomUUID(),
    field: first.field,
    operator: first.operators[0],
    value: first.dataType === 'ENUM' ? (first.allowedValues?.[0] ?? '') : '',
    negate: false,
  }
}

export function defaultIndicatorLeaf(indicators: IndicatorCatalogEntry[]): IndicatorLeafDraft | null {
  const first = indicators[0]
  if (!first) return null
  const params: Record<string, string> = {}
  for (const p of first.parameters) {
    params[p.key] = p.default ?? (p.allowedValues?.[0] ?? '')
  }
  const defaultOutput = first.outputs.find((o) => o.default) ?? first.outputs[0]
  return {
    kind: 'INDICATOR',
    id: crypto.randomUUID(),
    indicatorKey: first.indicatorKey,
    params,
    outputField: defaultOutput?.key ?? '',
    operator: 'GT',
    compareTo: 'VALUE',
    value: '',
    compareIndicator: null,
    negate: false,
  }
}

/** Default right-hand indicator config for a new INDICATOR-vs-INDICATOR comparison. */
export function defaultIndicatorComparisonTarget(indicators: IndicatorCatalogEntry[]): IndicatorComparisonTarget | null {
  const first = indicators[0]
  if (!first) return null
  const params: Record<string, string> = {}
  for (const p of first.parameters) {
    params[p.key] = p.default ?? (p.allowedValues?.[0] ?? '')
  }
  const defaultOutput = first.outputs.find((o) => o.default) ?? first.outputs[0]
  return { indicatorKey: first.indicatorKey, params, outputField: defaultOutput?.key ?? '' }
}

/* ------------------------------------------------------------------ */
/* raiseRuleTree — the inverse of buildRuleTree, for the edit flow     */
/* ------------------------------------------------------------------ */

function raiseLeaf(node: MarketFieldNode | IndicatorNode): LeafDraft {
  if (node.type === 'MARKET_FIELD') {
    return {
      kind: 'MARKET_FIELD',
      id: crypto.randomUUID(),
      field: node.field,
      operator: node.operator,
      value: String(node.value),
      negate: false,
    }
  }

  const isIndicatorRef = typeof node.value === 'object' && node.value !== null
  const indicatorRef = isIndicatorRef ? (node.value as IndicatorValueRef) : null

  return {
    kind: 'INDICATOR',
    id: crypto.randomUUID(),
    indicatorKey: node.indicatorKey,
    params: { ...node.params },
    outputField: node.outputField,
    operator: node.operator,
    compareTo: indicatorRef ? 'INDICATOR' : 'VALUE',
    value: indicatorRef ? '' : String(node.value),
    compareIndicator: indicatorRef
      ? { indicatorKey: indicatorRef.indicatorKey, params: { ...indicatorRef.params }, outputField: indicatorRef.outputField }
      : null,
    negate: false,
  }
}

/**
 * Decomposes the left-leaning `BOOLEAN` chain `buildRuleTree` folds a group's children into,
 * back into an ordered list of children and connecting operators. Only walks `node.left` — each
 * `node.right` is a single, self-contained lowered child (leaf, negation, or nested subgroup),
 * never itself part of this chain, which is exactly what makes the decomposition unambiguous.
 */
function collectChain(node: RuleNode): { children: RuleNode[]; operators: BooleanOperator[] } {
  if (node.type !== 'BOOLEAN') {
    return { children: [node], operators: [] }
  }
  const left = collectChain(node.left)
  return { children: [...left.children, node.right], operators: [...left.operators, node.operator] }
}

/**
 * Raises one wire `RuleNode` into its editable draft. `UNARY_BOOLEAN(NOT, child)` toggles
 * `negate` on the raised child rather than introducing a wrapper node — this also makes
 * double negation (a negated group containing a single negated child) cancel out for free,
 * matching `buildRuleTree`'s behavior of folding a single-child group down to just that child.
 */
function raiseNode(node: RuleNode): ConditionDraft {
  if (node.type === 'UNARY_BOOLEAN') {
    const inner = raiseNode(node.child)
    return { ...inner, negate: !inner.negate }
  }

  if (node.type === 'BOOLEAN') {
    const chain = collectChain(node)
    return {
      kind: 'GROUP',
      id: crypto.randomUUID(),
      operators: chain.operators,
      negate: false,
      children: chain.children.map(raiseNode),
    }
  }

  return raiseLeaf(node)
}

/**
 * Converts an existing wire `RuleNode` (`StrategyView.ruleTree`) into the editable draft
 * representation `ConditionGroup`/`buildRuleTree` operate on, for the edit-strategy flow.
 * Always returns a root `GroupDraft`: a raised leaf, or a raised negated group, is wrapped as
 * that group's sole child, which `buildRuleTree` folds straight back to the original node.
 */
export function raiseRuleTree(node: RuleNode): GroupDraft {
  const raised = raiseNode(node)
  if (raised.kind === 'GROUP' && !raised.negate) {
    return raised
  }
  return { kind: 'GROUP', id: crypto.randomUUID(), operators: [], negate: false, children: [raised] }
}

