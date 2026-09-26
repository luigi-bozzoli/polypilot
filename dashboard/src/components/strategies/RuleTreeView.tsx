import type { RuleNode } from '../../features/strategies/types'

/**
 * Read-only render of a strategy's `ruleTree` (mock `15-strategy-detail.html`'s
 * `.tree` block). Distinct from `ConditionGroup`/`ConditionLeafRow`, which are
 * editable-draft components requiring the condition-fields catalog — this
 * renders the already-fetched `RuleNode` wire shape directly, field keys and
 * all, matching the mock's raw-field-name presentation.
 */

function ConditionBox({ field, operator, value }: { field: string; operator: string; value: string }) {
  return (
    <div className="my-1.5 rounded-md border border-border bg-bg2 px-2.5 py-1.5 font-mono text-[11.5px]">
      <span className="text-text-secondary">{field}</span>
      <span className="mx-1.5 text-text-muted">{operator.toLowerCase()}</span>
      <span className="text-accent">{value}</span>
    </div>
  )
}

function OperatorBadge({ label }: { label: string }) {
  return (
    <span className="inline-block rounded border border-[rgba(77,158,245,0.3)] bg-blue-dim px-1.5 py-0.5 font-mono text-[10px] tracking-wide text-blue">
      {label}
    </span>
  )
}

function formatIndicatorRef(indicatorKey: string, params: Record<string, string>, outputField: string): string {
  const paramSummary = Object.entries(params)
    .map(([k, v]) => `${k}=${v}`)
    .join(', ')
  return `${indicatorKey}(${paramSummary}).${outputField}`
}

function formatIndicatorLabel(node: Extract<RuleNode, { type: 'INDICATOR' }>): string {
  return formatIndicatorRef(node.indicatorKey, node.params, node.outputField)
}

function formatIndicatorValue(value: Extract<RuleNode, { type: 'INDICATOR' }>['value']): string {
  return typeof value === 'number' ? String(value) : formatIndicatorRef(value.indicatorKey, value.params, value.outputField)
}

export function RuleTreeView({ node }: { node: RuleNode }) {
  switch (node.type) {
    case 'BOOLEAN':
      return (
        <div>
          <OperatorBadge label={node.operator} />
          <div className="ml-1 border-l-2 border-border-mid py-1.5 pl-3.5">
            <RuleTreeView node={node.left} />
            <RuleTreeView node={node.right} />
          </div>
        </div>
      )
    case 'UNARY_BOOLEAN':
      return (
        <div>
          <OperatorBadge label={node.operator} />
          <div className="ml-1 border-l-2 border-border-mid py-1.5 pl-3.5">
            <RuleTreeView node={node.child} />
          </div>
        </div>
      )
    case 'MARKET_FIELD':
      return (
        <ConditionBox
          field={node.field}
          operator={node.operator}
          value={typeof node.value === 'string' ? `"${node.value}"` : String(node.value)}
        />
      )
    case 'INDICATOR':
      return <ConditionBox field={formatIndicatorLabel(node)} operator={node.operator} value={formatIndicatorValue(node.value)} />
  }
}
