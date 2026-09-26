import { useQuery } from '@tanstack/react-query'
import { fetchAuditLogs, fetchMarketDecisions, fetchStrategyDecisions } from './auditApi'

/** Global audit feed, newest first — backs the Overview "Recent engine decisions" widget. */
export function useAuditLogs(size = 6) {
  return useQuery({
    queryKey: ['audit', 'logs', size],
    queryFn: () => fetchAuditLogs(size),
  })
}

export function useMarketDecisions(marketId: string | undefined) {
  return useQuery({
    queryKey: ['audit', 'market-decisions', marketId],
    queryFn: () => fetchMarketDecisions(marketId as string),
    enabled: Boolean(marketId),
  })
}

export function useStrategyDecisions(strategyId: string | undefined) {
  return useQuery({
    queryKey: ['audit', 'strategy-decisions', strategyId],
    queryFn: () => fetchStrategyDecisions(strategyId as string),
    enabled: Boolean(strategyId),
  })
}
