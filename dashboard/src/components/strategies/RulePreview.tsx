import { formatConditionTree } from '../../features/strategies/conditionTree'
import type { ConditionCatalogs, GroupDraft } from '../../features/strategies/conditionTree'
import type { OrderType, TokenSide } from '../../features/strategies/types'

type RulePreviewProps = {
  root: GroupDraft
  catalogs: ConditionCatalogs
  tokenSide: TokenSide
  orderType: OrderType
  maxBetSize: string
}

export function RulePreview({ root, catalogs, tokenSide, orderType, maxBetSize }: RulePreviewProps) {
  const lines = formatConditionTree(root, catalogs)

  return (
    <div className="mt-5 rounded-lg border border-border bg-bg3 p-3.5">
      <div className="mb-2 text-[10px] uppercase tracking-wider text-text-muted">Rule preview</div>
      <div className="font-mono text-[11px] leading-[1.8] text-text-secondary">
        {lines.length === 0 ? (
          <span className="text-text-muted">Add at least one condition to see a preview.</span>
        ) : (
          lines.map((line, i) => <div key={i}>{line}</div>)
        )}
        {lines.length > 0 && (
          <div>
            <span className="text-blue">THEN</span> <span className="text-yellow">BUY {tokenSide}</span>{' '}
            <span className="text-text-muted">
              ({orderType}, max ${maxBetSize || '0'})
            </span>
          </div>
        )}
      </div>
    </div>
  )
}
