import { Plus } from 'lucide-react'
import { defaultIndicatorLeaf, defaultMarketFieldLeaf, emptyGroup } from '../../features/strategies/conditionTree'
import type { ConditionCatalogs, ConditionDraft, GroupDraft, LeafDraft } from '../../features/strategies/conditionTree'
import type { BooleanOperator } from '../../features/strategies/types'
import { ConditionLeafRow } from './ConditionLeafRow'

const BOOLEAN_OPERATORS: BooleanOperator[] = ['AND', 'OR', 'XOR']

type ConditionGroupProps = {
  group: GroupDraft
  catalogs: ConditionCatalogs
  onChange: (next: GroupDraft) => void
  onDelete?: () => void
  isRoot?: boolean
}

export function ConditionGroup({ group, catalogs, onChange, onDelete, isRoot = false }: ConditionGroupProps) {
  const updateChild = (index: number, next: ConditionDraft) => {
    onChange({ ...group, children: group.children.map((c, i) => (i === index ? next : c)) })
  }

  const deleteChild = (index: number) => {
    const children = group.children.filter((_, i) => i !== index)
    // Drop the connector that pairs with the removed child: the one after it, or (for the
    // first child) the one that followed it — see `conditionTree.ts` header comment on `operators`.
    const removeAt = index > 0 ? index - 1 : 0
    const operators = group.operators.filter((_, i) => i !== removeAt)
    onChange({ ...group, children, operators })
  }

  const setOperator = (connectorIndex: number, op: BooleanOperator) => {
    onChange({ ...group, operators: group.operators.map((o, i) => (i === connectorIndex ? op : o)) })
  }

  const appendChild = (child: ConditionDraft) => {
    const operators = group.children.length > 0 ? [...group.operators, 'AND' as BooleanOperator] : group.operators
    onChange({ ...group, children: [...group.children, child], operators })
  }

  const addLeaf = () => {
    const leaf: LeafDraft | null = catalogs.marketFields.length > 0 ? defaultMarketFieldLeaf(catalogs) : defaultIndicatorLeaf(catalogs.indicators)
    if (leaf) appendChild(leaf)
  }

  const addGroup = () => {
    appendChild(emptyGroup())
  }

  return (
    <div className={isRoot ? '' : 'rounded-lg border border-border-mid bg-bg2 p-3'}>
      {!isRoot && (
        <div className="mb-2.5 flex items-center gap-2.5">
          <button
            type="button"
            role="switch"
            aria-checked={group.negate}
            onClick={() => onChange({ ...group, negate: !group.negate })}
            className={`rounded border px-2 py-1 font-mono text-[10px] font-medium ${
              group.negate ? 'border-red-border bg-red-dim text-red' : 'border-border-mid bg-bg4 text-text-muted'
            }`}
          >
            NOT
          </button>
          {onDelete && (
            <button type="button" onClick={onDelete} className="ml-auto text-[11px] text-text-muted hover:text-red">
              Remove group
            </button>
          )}
        </div>
      )}

      {group.children.map((child, i) => (
        <div key={child.id}>
          {i > 0 && (
            <div className="mb-2 flex overflow-hidden rounded border border-border-mid" style={{ width: 'fit-content' }}>
              {BOOLEAN_OPERATORS.map((op) => (
                <button
                  key={op}
                  type="button"
                  onClick={() => setOperator(i - 1, op)}
                  className={`px-2 py-1 font-mono text-[10px] font-medium ${
                    group.operators[i - 1] === op ? 'bg-accent-dim text-accent' : 'bg-bg4 text-text-muted'
                  }`}
                >
                  {op}
                </button>
              ))}
            </div>
          )}
          {child.kind === 'GROUP' ? (
            <div className="mb-2">
              <ConditionGroup group={child} catalogs={catalogs} onChange={(next) => updateChild(i, next)} onDelete={() => deleteChild(i)} />
            </div>
          ) : (
            <ConditionLeafRow index={i} leaf={child} catalogs={catalogs} onChange={(next) => updateChild(i, next)} onDelete={() => deleteChild(i)} />
          )}
        </div>
      ))}

      <div className="flex gap-2">
        <button
          type="button"
          onClick={addLeaf}
          className="flex flex-1 items-center justify-center gap-1 rounded-lg border border-dashed border-border-mid py-2 text-center text-[12px] text-text-muted hover:border-accent-border hover:bg-accent-dim hover:text-accent"
        >
          <Plus className="h-3 w-3" aria-hidden="true" /> Condition
        </button>
        <button
          type="button"
          onClick={addGroup}
          className="flex flex-1 items-center justify-center gap-1 rounded-lg border border-dashed border-border-mid py-2 text-center text-[12px] text-text-muted hover:border-accent-border hover:bg-accent-dim hover:text-accent"
        >
          <Plus className="h-3 w-3" aria-hidden="true" /> Group
        </button>
      </div>
    </div>
  )
}
