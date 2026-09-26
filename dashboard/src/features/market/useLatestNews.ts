import { useQuery } from '@tanstack/react-query'
import { fetchLatestNews } from './marketApi'

/** Matches the `news-sync` scheduler cadence (15 min) — this data changes far
 *  less often than price history, so a short refetch interval would be waste. */
const REFETCH_INTERVAL_MS = 15 * 60_000

/** Latest news summary for one market. Mirrors `useMarketPriceHistory`. */
export function useLatestNews(marketId: string | undefined) {
  return useQuery({
    queryKey: ['market-news', marketId],
    queryFn: () => fetchLatestNews(marketId as string),
    enabled: Boolean(marketId),
    refetchInterval: REFETCH_INTERVAL_MS,
  })
}
