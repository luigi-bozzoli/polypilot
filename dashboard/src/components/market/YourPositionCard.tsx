import { Card, NoDataNote, SectionLabel, SkeletonBar } from './primitives'

/**
 * "Your position" (mock `13-market-detail.html`).
 *
 * SKELETON — no backend yet. `positions` is never written (trading not wired)
 * and there is no read endpoint. See `contracts/market-position.md` for the
 * proposed `GET /api/market/{marketId}/position` shape (`MarketPosition` in
 * `features/market/types.ts`).
 *
 * TODO(integration):
 *   - add `features/market/marketApi.ts#fetchMarketPosition` +
 *     `useMarketPosition(marketId)` (react-query, 204 → undefined = flat)
 *   - replace placeholders: side / size / avg_entry_price / unrealized_pnl
 *     (green when >= 0, red when < 0)
 *   - when flat (204), show a "no position" `Empty` and hide "Close position"
 *   - enable "Close position" once the close-position route
 *     (mock `07-close-position.html`) exists; disabled for now
 */

const KV_ROWS = ['side', 'size', 'avg_entry_price', 'unrealized_pnl'] as const

export function YourPositionCard({ marketId }: { marketId: string }) {
  void marketId

  return (
    <Card>
      <SectionLabel className="mb-[11px]">Your position</SectionLabel>

      <div>
        {KV_ROWS.map((k) => (
          <div
            key={k}
            className="flex items-center justify-between border-b border-border py-[5px] text-[11px] last:border-b-0"
          >
            <span className="text-text-muted">{k}</span>
            <SkeletonBar className="w-16" />
          </div>
        ))}
      </div>

      {/* Close-position route does not exist yet — disabled, same treatment as
          unimplemented nav items (`features/shell/navItems.ts`). */}
      <span
        aria-disabled="true"
        title="Close position — route not available yet"
        className="mt-3 flex w-full cursor-not-allowed justify-center rounded-md border border-border-mid bg-bg3 px-3.5 py-2 text-[12px] font-medium text-text-muted"
      >
        Close position
      </span>

      <NoDataNote endpoint="GET /api/market/{id}/position" />
    </Card>
  )
}
