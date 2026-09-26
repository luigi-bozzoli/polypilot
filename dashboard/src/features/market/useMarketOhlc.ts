import { useQuery } from '@tanstack/react-query'
import { fetchMarketOhlc, fetchTimeframes } from './marketApi'

/** Candles move at most once per closed bar — poll a bit slower than price-history's 15s. */
const REFETCH_INTERVAL_MS = 20_000

/**
 * OHLC candles for one market's underlying asset. Mirrors `useMarketPriceHistory`:
 * keyed query, short refetch interval (so the forming candle updates live), disabled
 * until the id is known.
 */
export function useMarketOhlc(marketId: string | undefined, timeframe: string, limit?: number) {
  return useQuery({
    queryKey: ['market-ohlc', marketId, timeframe, limit],
    queryFn: () => fetchMarketOhlc(marketId as string, timeframe, limit),
    enabled: Boolean(marketId),
    refetchInterval: REFETCH_INTERVAL_MS,
  })
}

/**
 * Enabled timeframe options for the interval selector. Static DB config, not
 * market-scoped — a long `staleTime` avoids refetching it on every render.
 */
export function useTimeframes() {
  return useQuery({
    queryKey: ['timeframes'],
    queryFn: fetchTimeframes,
    staleTime: 5 * 60_000,
  })
}
