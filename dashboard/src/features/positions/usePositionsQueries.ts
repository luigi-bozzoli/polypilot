import { useQuery } from '@tanstack/react-query'
import { fetchOpenPositions } from './positionsApi'

/** Caller's open positions, newest-opened first — backs the Overview positions widget. */
export function useOpenPositions(limit = 4) {
  return useQuery({
    queryKey: ['positions', 'open', limit],
    queryFn: () => fetchOpenPositions(limit),
  })
}
