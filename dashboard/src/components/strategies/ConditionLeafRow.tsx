import { X } from 'lucide-react'
import {
  defaultIndicatorComparisonTarget,
  defaultIndicatorLeaf,
  defaultMarketFieldLeaf,
  findIndicator,
} from '../../features/strategies/conditionTree'
import type {
  ConditionCatalogs,
  IndicatorComparisonTarget,
  IndicatorLeafDraft,
  LeafDraft,
  MarketFieldLeafDraft,
} from '../../features/strategies/conditionTree'
import type { CompareOperator, MarketField } from '../../features/strategies/types'

const OPERATOR_LABEL: Record<CompareOperator, string> = {
  EQ: '=',
  NEQ: '≠',
  GT: '>',
  GTE: '≥',
  LT: '<',
  LTE: '≤',
}

const ALL_OPERATORS: CompareOperator[] = ['EQ', 'NEQ', 'GT', 'GTE', 'LT', 'LTE']

type ConditionLeafRowProps = {
  index: number
  leaf: LeafDraft
  catalogs: ConditionCatalogs
  onChange: (next: LeafDraft) => void
  onDelete: () => void
}

export function ConditionLeafRow({ index, leaf, catalogs, onChange, onDelete }: ConditionLeafRowProps) {
  return (
    <div className="mb-2 rounded-lg border border-border bg-bg3 px-3.5 py-3">
      <div className="flex items-center gap-2.5">
        <div className="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded border border-border-mid bg-bg4 font-mono text-[9px] text-text-muted">
          {index + 1}
        </div>

        <div className="flex overflow-hidden rounded border border-border-mid">
          <button
            type="button"
            onClick={() => {
              if (leaf.kind !== 'MARKET_FIELD') onChange(defaultMarketFieldLeaf(catalogs))
            }}
            className={`px-2 py-1 text-[10px] font-medium uppercase tracking-wider ${leaf.kind === 'MARKET_FIELD' ? 'bg-accent-dim text-accent' : 'bg-bg4 text-text-muted'
              }`}
          >
            Field
          </button>
          <button
            type="button"
            onClick={() => {
              if (leaf.kind !== 'INDICATOR') {
                const next = defaultIndicatorLeaf(catalogs.indicators)
                if (next) onChange(next)
              }
            }}
            disabled={catalogs.indicators.length === 0}
            className={`px-2 py-1 text-[10px] font-medium uppercase tracking-wider disabled:cursor-not-allowed disabled:opacity-50 ${leaf.kind === 'INDICATOR' ? 'bg-accent-dim text-accent' : 'bg-bg4 text-text-muted'
              }`}
          >
            Indicator
          </button>
        </div>

        <button
          type="button"
          role="switch"
          aria-checked={leaf.negate}
          onClick={() => onChange({ ...leaf, negate: !leaf.negate })}
          className={`flex-shrink-0 rounded border px-2 py-1 font-mono text-[10px] font-medium ${leaf.negate ? 'border-red-border bg-red-dim text-red' : 'border-border-mid bg-bg4 text-text-muted'
            }`}
        >
          NOT
        </button>

        <div className="flex flex-1 flex-wrap items-center gap-2">
          {leaf.kind === 'MARKET_FIELD' ? (
            <MarketFieldFields leaf={leaf} catalogs={catalogs} onChange={onChange} />
          ) : (
            <IndicatorFields leaf={leaf} catalogs={catalogs} onChange={onChange} />
          )}
        </div>

        <button
          type="button"
          onClick={onDelete}
          className="flex h-[22px] w-[22px] flex-shrink-0 items-center justify-center rounded border border-transparent text-text-muted hover:border-red-border hover:bg-red-dim hover:text-red"
          aria-label="Remove condition"
        >
          <X className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      </div>
    </div>
  )
}

function MarketFieldFields({
  leaf,
  catalogs,
  onChange,
}: {
  leaf: MarketFieldLeafDraft
  catalogs: ConditionCatalogs
  onChange: (next: LeafDraft) => void
}) {
  const entry = catalogs.marketFields.find((f) => f.field === leaf.field)

  const handleFieldChange = (field: MarketField) => {
    const nextEntry = catalogs.marketFields.find((f) => f.field === field)
    onChange({
      ...leaf,
      field,
      operator: nextEntry?.operators[0] ?? 'EQ',
      value: nextEntry?.dataType === 'ENUM' ? (nextEntry.allowedValues?.[0] ?? '') : '',
    })
  }

  return (
    <>
      <select
        className="rounded bg-bg4 px-2 py-1 font-sans text-[12px] text-text-secondary outline-none"
        value={leaf.field}
        onChange={(e) => handleFieldChange(e.target.value as MarketField)}
      >
        {catalogs.marketFields.map((f) => (
          <option key={f.field} value={f.field}>
            {f.name}
          </option>
        ))}
      </select>

      <select
        className="rounded border border-border-mid bg-bg4 px-2 py-1 font-mono text-[11px] text-text-secondary outline-none"
        value={leaf.operator}
        onChange={(e) => onChange({ ...leaf, operator: e.target.value as CompareOperator })}
      >
        {(entry?.operators ?? ['EQ']).map((op) => (
          <option key={op} value={op}>
            {OPERATOR_LABEL[op]}
          </option>
        ))}
      </select>

      {entry?.dataType === 'ENUM' ? (
        <select
          className="rounded border border-border-mid bg-bg4 px-2 py-1 font-mono text-[11px] text-accent outline-none"
          value={leaf.value}
          onChange={(e) => onChange({ ...leaf, value: e.target.value })}
        >
          {entry.allowedValues?.map((v) => (
            <option key={v} value={v}>
              {v}
            </option>
          ))}
        </select>
      ) : (
        <input
          type="number"
          className="w-20 rounded border border-border-mid bg-bg4 px-2 py-1 text-center font-mono text-[11px] text-accent outline-none"
          value={leaf.value}
          onChange={(e) => onChange({ ...leaf, value: e.target.value })}
        />
      )}
    </>
  )
}

function IndicatorFields({
  leaf,
  catalogs,
  onChange,
}: {
  leaf: IndicatorLeafDraft
  catalogs: ConditionCatalogs
  onChange: (next: LeafDraft) => void
}) {
  const indicator = findIndicator(catalogs.indicators, leaf.indicatorKey)

  const handleIndicatorChange = (indicatorKey: string) => {
    const next = findIndicator(catalogs.indicators, indicatorKey)
    const params: Record<string, string> = {}
    for (const p of next?.parameters ?? []) {
      params[p.key] = p.default ?? (p.allowedValues?.[0] ?? '')
    }
    const defaultOutput = next?.outputs.find((o) => o.default) ?? next?.outputs[0]
    onChange({ ...leaf, indicatorKey, params, outputField: defaultOutput?.key ?? '' })
  }

  return (
    <>
      <select
        className="rounded bg-bg4 px-2 py-1 font-sans text-[12px] text-text-secondary outline-none"
        value={leaf.indicatorKey}
        onChange={(e) => handleIndicatorChange(e.target.value)}
      >
        {catalogs.indicators.map((i) => (
          <option key={i.indicatorKey} value={i.indicatorKey}>
            {i.name}
          </option>
        ))}
      </select>

      {indicator?.parameters.map((param) => (
        <div key={param.key} className="flex items-center gap-1">
          <span className="text-[9px] uppercase tracking-wider text-text-muted">{param.name}</span>
          {param.dataType === 'ENUM' ? (
            <select
              className="rounded border border-border-mid bg-bg4 px-1.5 py-1 font-mono text-[11px] text-text-secondary outline-none"
              value={leaf.params[param.key] ?? ''}
              onChange={(e) => onChange({ ...leaf, params: { ...leaf.params, [param.key]: e.target.value } })}
            >
              {param.allowedValues?.map((v) => (
                <option key={v} value={v}>
                  {v}
                </option>
              ))}
            </select>
          ) : (
            <input
              type="number"
              min={param.constraints?.min as number | undefined}
              max={param.constraints?.max as number | undefined}
              step={(param.constraints?.step as number | undefined) ?? (param.dataType === 'INTEGER' ? 1 : undefined)}
              className="w-14 rounded border border-border-mid bg-bg4 px-1.5 py-1 text-center font-mono text-[11px] text-text-secondary outline-none"
              value={leaf.params[param.key] ?? ''}
              onChange={(e) => onChange({ ...leaf, params: { ...leaf.params, [param.key]: e.target.value } })}
            />
          )}
        </div>
      ))}

      <span className="text-[10px] text-text-muted">→</span>

      <select
        className="rounded border border-border-mid bg-bg4 px-2 py-1 font-mono text-[11px] text-text-secondary outline-none"
        value={leaf.outputField}
        onChange={(e) => onChange({ ...leaf, outputField: e.target.value })}
      >
        {indicator?.outputs.map((o) => (
          <option key={o.key} value={o.key}>
            {o.name}
          </option>
        ))}
      </select>

      <select
        className="rounded border border-border-mid bg-bg4 px-2 py-1 font-mono text-[11px] text-text-secondary outline-none"
        value={leaf.operator}
        onChange={(e) => onChange({ ...leaf, operator: e.target.value as CompareOperator })}
      >
        {(indicator?.operators ?? ALL_OPERATORS).map((op) => (
          <option key={op} value={op}>
            {OPERATOR_LABEL[op]}
          </option>
        ))}
      </select>

      <div className="flex overflow-hidden rounded border border-border-mid">
        <button
          type="button"
          onClick={() => onChange({ ...leaf, compareTo: 'VALUE' })}
          className={`px-2 py-1 text-[10px] font-medium uppercase tracking-wider ${
            leaf.compareTo === 'VALUE' ? 'bg-accent-dim text-accent' : 'bg-bg4 text-text-muted'
          }`}
        >
          Value
        </button>
        <button
          type="button"
          onClick={() =>
            onChange({
              ...leaf,
              compareTo: 'INDICATOR',
              compareIndicator: leaf.compareIndicator ?? defaultIndicatorComparisonTarget(catalogs.indicators),
            })
          }
          disabled={catalogs.indicators.length === 0}
          className={`px-2 py-1 text-[10px] font-medium uppercase tracking-wider disabled:cursor-not-allowed disabled:opacity-50 ${
            leaf.compareTo === 'INDICATOR' ? 'bg-accent-dim text-accent' : 'bg-bg4 text-text-muted'
          }`}
        >
          Indicator
        </button>
      </div>

      {leaf.compareTo === 'INDICATOR' && leaf.compareIndicator ? (
        <IndicatorComparisonFields
          target={leaf.compareIndicator}
          catalogs={catalogs}
          onChange={(next) => onChange({ ...leaf, compareIndicator: next })}
        />
      ) : (
        <input
          type="number"
          className="w-20 rounded border border-border-mid bg-bg4 px-2 py-1 text-center font-mono text-[11px] text-accent outline-none"
          placeholder="value"
          value={leaf.value}
          onChange={(e) => onChange({ ...leaf, value: e.target.value })}
        />
      )}
    </>
  )
}

/** Picker for the right-hand indicator in an INDICATOR-vs-INDICATOR comparison — mirrors the LHS indicator controls. */
function IndicatorComparisonFields({
  target,
  catalogs,
  onChange,
}: {
  target: IndicatorComparisonTarget
  catalogs: ConditionCatalogs
  onChange: (next: IndicatorComparisonTarget) => void
}) {
  const indicator = findIndicator(catalogs.indicators, target.indicatorKey)

  const handleIndicatorChange = (indicatorKey: string) => {
    const next = findIndicator(catalogs.indicators, indicatorKey)
    const params: Record<string, string> = {}
    for (const p of next?.parameters ?? []) {
      params[p.key] = p.default ?? (p.allowedValues?.[0] ?? '')
    }
    const defaultOutput = next?.outputs.find((o) => o.default) ?? next?.outputs[0]
    onChange({ indicatorKey, params, outputField: defaultOutput?.key ?? '' })
  }

  return (
    <>
      <select
        className="rounded bg-bg4 px-2 py-1 font-sans text-[12px] text-text-secondary outline-none"
        value={target.indicatorKey}
        onChange={(e) => handleIndicatorChange(e.target.value)}
      >
        {catalogs.indicators.map((i) => (
          <option key={i.indicatorKey} value={i.indicatorKey}>
            {i.name}
          </option>
        ))}
      </select>

      {indicator?.parameters.map((param) => (
        <div key={param.key} className="flex items-center gap-1">
          <span className="text-[9px] uppercase tracking-wider text-text-muted">{param.name}</span>
          {param.dataType === 'ENUM' ? (
            <select
              className="rounded border border-border-mid bg-bg4 px-1.5 py-1 font-mono text-[11px] text-text-secondary outline-none"
              value={target.params[param.key] ?? ''}
              onChange={(e) => onChange({ ...target, params: { ...target.params, [param.key]: e.target.value } })}
            >
              {param.allowedValues?.map((v) => (
                <option key={v} value={v}>
                  {v}
                </option>
              ))}
            </select>
          ) : (
            <input
              type="number"
              min={param.constraints?.min as number | undefined}
              max={param.constraints?.max as number | undefined}
              step={(param.constraints?.step as number | undefined) ?? (param.dataType === 'INTEGER' ? 1 : undefined)}
              className="w-14 rounded border border-border-mid bg-bg4 px-1.5 py-1 text-center font-mono text-[11px] text-text-secondary outline-none"
              value={target.params[param.key] ?? ''}
              onChange={(e) => onChange({ ...target, params: { ...target.params, [param.key]: e.target.value } })}
            />
          )}
        </div>
      ))}

      <span className="text-[10px] text-text-muted">→</span>

      <select
        className="rounded border border-border-mid bg-bg4 px-2 py-1 font-mono text-[11px] text-text-secondary outline-none"
        value={target.outputField}
        onChange={(e) => onChange({ ...target, outputField: e.target.value })}
      >
        {indicator?.outputs.map((o) => (
          <option key={o.key} value={o.key}>
            {o.name}
          </option>
        ))}
      </select>
    </>
  )
}
