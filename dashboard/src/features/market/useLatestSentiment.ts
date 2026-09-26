import { useQuery } from '@tanstack/react-query'
import { fetchLatestSentiment } from './marketApi'

/** Matches the `news-sync` scheduler cadence (15 min) — see `useLatestNews`. */
const REFETCH_INTERVAL_MS = 15 * 60_000

/** Latest sentiment score for one market. Mirrors `useMarketPriceHistory`. */
export function useLatestSentiment(marketId: string | undefined) {
  return useQuery({
    queryKey: ['market-sentiment', marketId],
    queryFn: () => fetchLatestSentiment(marketId as string),
    enabled: Boolean(marketId),
    refetchInterval: REFETCH_INTERVAL_MS,
  })
}
