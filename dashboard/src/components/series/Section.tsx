import { MarketRow } from './MarketRow'
import type { MarketView } from '../../features/series/types'

interface SectionProps {
    title: string
    markets: MarketView[]
    /** Owning series id — forwarded to each `MarketRow` for its detail link. */
    seriesId: string
}

export function Section({ title, markets, seriesId }: SectionProps) {
    if (markets.length === 0) return null

    return (
        <div className="mb-6">
            <div className="mb-2 text-[10px] uppercase tracking-wider text-text-muted">
                {title} · {markets.length}
            </div>
            <div className="flex flex-col gap-3">
                {markets.map((market) => (
                    <MarketRow key={market.id} market={market} seriesId={seriesId} />
                ))}
            </div>
        </div>
    )
}