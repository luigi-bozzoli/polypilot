import { useQuery } from '@tanstack/react-query'
import { fetchMarketOrders } from './marketApi'

/** Market-scoped recent orders, newest first. Mirrors `useLatestNews`. */
export function useMarketOrders(marketId: string | undefined, limit = 20) {
  return useQuery({
    queryKey: ['market-orders', marketId, limit],
    queryFn: () => fetchMarketOrders(marketId as string, limit),
    enabled: Boolean(marketId),
  })
}
