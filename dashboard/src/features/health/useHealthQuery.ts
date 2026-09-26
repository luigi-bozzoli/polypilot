import { useQuery } from '@tanstack/react-query'
import { fetchHealth, fetchHealthChecks, fetchInfrastructure } from './healthApi'

/** Matches the mock's "polls every 5 s" subtitle. */
const HEALTH_REFETCH_MS = 5_000
/** Infra probes are heavier (conn-count query, INFO, queue read) — looser poll. */
const INFRA_REFETCH_MS = 15_000

/**
 * `autoProbe` gates every automatic request: when false the interval is off and
 * focus/reconnect refetches are suppressed too, so nothing hits the network
 * until the user re-enables it or triggers a manual refresh. See `useAutoProbe`.
 */
export function useHealthQuery(autoProbe: boolean) {
  return useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
    refetchInterval: autoProbe ? HEALTH_REFETCH_MS : false,
    refetchOnWindowFocus: autoProbe,
    refetchOnReconnect: autoProbe,
  })
}

export function useInfrastructureQuery(autoProbe: boolean) {
  return useQuery({
    queryKey: ['health', 'infrastructure'],
    queryFn: fetchInfrastructure,
    refetchInterval: autoProbe ? INFRA_REFETCH_MS : false,
    refetchOnWindowFocus: autoProbe,
    refetchOnReconnect: autoProbe,
  })
}

/** Deploy/schema checks don't change between boots — fetch once, don't poll. */
export function useHealthChecksQuery() {
  return useQuery({
    queryKey: ['health', 'checks'],
    queryFn: fetchHealthChecks,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })
}
