import { authGet } from '../../lib/http'
import { ChecksResponse, HealthResponse, InfrastructureResponse } from './types'

/**
 * Aggregate health probe — orchestrator status + metrics, fanned out to both
 * Python services. Requires a session (see `SecurityConfig`, which now gates
 * `/health/**`). GET /api/health
 */
export function fetchHealth(): Promise<HealthResponse> {
  return authGet<HealthResponse>('/api/health')
}

/**
 * Static-per-deploy schema / boot checks. Called once per session rather than
 * on the 5s poll. GET /api/health/checks
 */
export function fetchHealthChecks(): Promise<ChecksResponse> {
  return authGet<ChecksResponse>('/api/health/checks')
}

/**
 * Postgres / Redis / RabbitMQ status on polypilot-net. Heavier probes, polled
 * on a looser interval than /api/health. GET /api/health/infrastructure
 */
export function fetchInfrastructure(): Promise<InfrastructureResponse> {
  return authGet<InfrastructureResponse>('/api/health/infrastructure')
}
