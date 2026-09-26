import { useQuery } from '@tanstack/react-query'
import { fetchSeriesDetail, fetchSeriesList } from './seriesApi'

/** Prices and volume move constantly; refetch on a short interval. */
const REFETCH_INTERVAL_MS = 15_000

export function useSeriesList() {
  return useQuery({
    queryKey: ['series'],
    queryFn: fetchSeriesList,
    refetchInterval: REFETCH_INTERVAL_MS,
  })
}

export function useSeriesDetail(id: string | undefined) {
  return useQuery({
    queryKey: ['series', id],
    queryFn: () => fetchSeriesDetail(id as string),
    enabled: Boolean(id),
    refetchInterval: REFETCH_INTERVAL_MS,
  })
}
