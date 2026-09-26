import type { ConditionCatalogs, GroupDraft } from '../../features/strategies/conditionTree'
import type { OrderType, TokenSide } from '../../features/strategies/types'
import { ConditionGroup } from './ConditionGroup'
import { RulePreview } from './RulePreview'
import { Toggle } from './Toggle'

const ORDER_TYPES: { value: OrderType; label: string }[] = [
  { value: 'GTC', label: 'GTC — rests on order book' },
  { value: 'GTD', label: 'GTD — rests until a deadline' },
  { value: 'FOK', label: 'FOK — all-or-nothing fill' },
  { value: 'FAK', label: 'FAK — partial fill OK' },
]

type StepEntryRulesProps = {
  strategyName: string
  tokenSide: TokenSide
  orderType: OrderType
  root: GroupDraft
  dryRun: boolean
  maxBetSizeForPreview: string
  catalogs: ConditionCatalogs
  onTokenSideChange: (side: TokenSide) => void
  onOrderTypeChange: (type: OrderType) => void
  onRootChange: (root: GroupDraft) => void
  onDryRunChange: (dryRun: boolean) => void
  error?: string
}

export function StepEntryRules({
  strategyName,
  tokenSide,
  orderType,
  root,
  dryRun,
  maxBetSizeForPreview,
  catalogs,
  onTokenSideChange,
  onOrderTypeChange,
  onRootChange,
  onDryRunChange,
  error,
}: StepEntryRulesProps) {
  return (
    <div className="flex flex-col gap-4.5">
      {/* Strategy name */}
      <div className="flex flex-col gap-1.5 border-b border-border pb-6">
        <div className="text-[10px] uppercase tracking-wider text-text-muted">
          Strategy name <span className="text-accent">·</span> from step 1
        </div>

        <div className="rounded-md border border-border bg-bg3 px-3 py-2.5 font-mono text-[12px] text-text-secondary">
          {strategyName || '—'}
        </div>
      </div>

      {/* Trade side / Order type */}
      <div className="grid grid-cols-2 gap-3.5 border-b border-border pb-6">
        <div className="flex flex-col gap-1.5">
          <div className="text-[10px] uppercase tracking-wider text-text-muted">
            Trade side
          </div>

          <div className="grid grid-cols-2 overflow-hidden rounded-md border border-border">
            <button
              type="button"
              onClick={() => onTokenSideChange('YES')}
              className={`px-2 py-2.5 text-center text-[12px] font-medium ${tokenSide === 'YES'
                  ? 'border-r border-accent-border bg-accent-dim text-accent'
                  : 'bg-bg3 text-text-muted'
                }`}
            >
              ▲ BUY YES
            </button>

            <button
              type="button"
              onClick={() => onTokenSideChange('NO')}
              className={`px-2 py-2.5 text-center text-[12px] font-medium ${tokenSide === 'NO'
                  ? 'bg-red-dim text-red'
                  : 'bg-bg3 text-text-muted'
                }`}
            >
              ▼ BUY NO
            </button>
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <div className="text-[10px] uppercase tracking-wider text-text-muted">
            Order type
          </div>

          <select
            className="w-full rounded-md border border-border bg-bg3 px-3 py-2.5 text-[12px] text-text-primary outline-none focus:border-accent-border"
            value={orderType}
            onChange={(e) =>
              onOrderTypeChange(e.target.value as OrderType)
            }
          >
            {ORDER_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Entry conditions */}
      <div className="flex flex-col gap-2.5 pb-2">
        <div className="text-[10px] uppercase tracking-wider text-text-muted">
          Entry conditions{' '}
          <span className="normal-case tracking-normal text-text-muted">
            (build a nested AND / OR / XOR / NOT tree)
          </span>
        </div>

        <ConditionGroup
          group={root}
          catalogs={catalogs}
          onChange={onRootChange}
          isRoot
        />

        {error && (
          <p className="text-[11px] text-red">
            {error}
          </p>
        )}
      </div>

      {/* Preview */}
      <RulePreview
        root={root}
        catalogs={catalogs}
        tokenSide={tokenSide}
        orderType={orderType}
        maxBetSize={maxBetSizeForPreview}
      />

      {/* Dry run */}
      <div className="mt-1 flex items-center justify-between gap-4 border-t border-border py-3">
        <div className="min-w-0">
          <div className="text-[12.5px] text-text-primary">
            Dry run only
          </div>

          <div className="mt-0.5 text-[11px] text-text-muted">
            Strategy will simulate trades — never place real orders
          </div>
        </div>

        <Toggle checked={dryRun} onChange={() => onDryRunChange(!dryRun)} aria-label="Dry run only" />
      </div>
    </div>
  )

}
