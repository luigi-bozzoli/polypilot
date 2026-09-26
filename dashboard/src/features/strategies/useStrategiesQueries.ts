import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createStrategy,
  deleteStrategy,
  fetchConditionFields,
  fetchStrategies,
  fetchStrategy,
  fetchStrategyOrders,
  setStrategyEnabled,
  updateStrategy,
} from './strategiesApi'
import type { CreateStrategyRequest, UpdateStrategyRequest } from './types'

/**
 * Single source of truth for the strategy condition picker — both `marketFields` and
 * `indicators` come from this one `GET /strategies/condition-fields` call. 
 */
export function useConditionFields() {
  return useQuery({
    queryKey: ['strategies', 'condition-fields'],
    queryFn: fetchConditionFields,
    staleTime: Infinity, // catalog is code-owned and rarely changes, per the contract
  })
}

export function useStrategiesList() {
  return useQuery({
    queryKey: ['strategies'],
    queryFn: fetchStrategies,
  })
}

export function useStrategyDetail(id: string | undefined) {
  return useQuery({
    queryKey: ['strategies', id],
    queryFn: () => fetchStrategy(id as string),
    enabled: !!id,
  })
}

export function useCreateStrategy() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: CreateStrategyRequest) => createStrategy(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['strategies'] })
    },
  })
}

export function useUpdateStrategy(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: UpdateStrategyRequest) => updateStrategy(id, request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['strategies'] })
      void queryClient.invalidateQueries({ queryKey: ['strategies', id] })
    },
  })
}

export function useSetStrategyEnabled(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (enabled: boolean) => setStrategyEnabled(id, enabled),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['strategies'] })
      void queryClient.invalidateQueries({ queryKey: ['strategies', id] })
    },
  })
}

/** Strategy-scoped recent orders, newest first — backs `StrategyRecentOrdersCard`. */
export function useStrategyOrders(strategyId: string) {
  return useQuery({
    queryKey: ['strategies', strategyId, 'orders'],
    queryFn: () => fetchStrategyOrders(strategyId),
  })
}

export function useDeleteStrategy() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteStrategy(id),
    onSuccess: (_data, id) => {
      void queryClient.invalidateQueries({ queryKey: ['strategies'] })
      queryClient.removeQueries({ queryKey: ['strategies', id] })
    },
  })
}
