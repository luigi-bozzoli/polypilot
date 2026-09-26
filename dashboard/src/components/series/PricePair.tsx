import type { MarketOutcome } from '../../features/series/types'
import { formatPriceCents } from '../../features/series/format'

type Props = {
  upPrice: number | null
  downPrice: number | null
  /** When the market has resolved, the winning side is highlighted. */
  outcome?: MarketOutcome | null
}

function Pill({ label, price, won }: { label: string; price: number | null; won: boolean | undefined }) {
  const tone =
    won === undefined
      ? 'border-border bg-bg3'
      : won
        ? 'border-accent-border bg-accent-dim text-accent'
        : 'border-border bg-bg3 opacity-50'
  return (
    <span className={`rounded border px-1.5 py-0.5 ${tone}`}>
      <span className="text-text-muted">{label}</span>{' '}
      <span className={won ? '' : 'text-text-primary'}>{formatPriceCents(price)}</span>
    </span>
  )
}

/** Up/Down price pills for an up-or-down market. */
export function PricePair({ upPrice, downPrice, outcome }: Props) {
  const resolved = outcome === 'UP' || outcome === 'DOWN' || outcome === 'YES' || outcome === 'NO'
  const upWon = resolved ? outcome === 'UP' || outcome === 'YES' : undefined
  const downWon = resolved ? outcome === 'DOWN' || outcome === 'NO' : undefined

  return (
    <div className="flex gap-1.5 font-mono text-[11px]">
      <Pill label="Up" price={upPrice} won={upWon} />
      <Pill label="Down" price={downPrice} won={downWon} />
    </div>
  )
}
