import { useQuery } from '@tanstack/react-query'
import { fetchMarketPriceHistory } from './marketApi'

/** Look-back windows offered by the chart's segmented control. */
export const PRICE_HISTORY_WINDOWS = ['12h', '24h', '7d'] as const
export type PriceHistoryWindow = (typeof PRICE_HISTORY_WINDOWS)[number]

/** Snapshots move on the same cadence as the series/market sync — refetch to match. */
const REFETCH_INTERVAL_MS = 15_000

/**
 * Price history for one market over `window`. Mirrors `useSeriesDetail`
 * (`features/series/useSeriesQueries.ts`): keyed query, short refetch interval,
 * disabled until the id is known.
 */
export function useMarketPriceHistory(
  marketId: string | undefined,
  window: PriceHistoryWindow,
) {
  return useQuery({
    queryKey: ['market-price-history', marketId, window],
    queryFn: () => fetchMarketPriceHistory(marketId as string, window),
    enabled: Boolean(marketId),
    refetchInterval: REFETCH_INTERVAL_MS,
  })
}
