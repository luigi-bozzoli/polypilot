import { useQuery } from '@tanstack/react-query'
import { fetchPortfolioSummary } from './portfolioApi'

export function usePortfolioSummary() {
  return useQuery({
    queryKey: ['portfolio', 'summary'],
    queryFn: fetchPortfolioSummary,
  })
}
