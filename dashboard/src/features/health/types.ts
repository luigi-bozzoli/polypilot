/** Per-service metrics row (uptime / latency / one detail fragment). */
export type ServiceMetrics = {
  /** Pre-formatted wall-clock uptime, e.g. "3d 04h". */
  uptime: string
  /** Health-probe round-trip in ms; null when not measured (e.g. self). */
  latencyMs: number | null
  /** One short service-specific status fragment, already formatted. */
  detail: string
}

/**
 * A downstream service entry: its own /health payload as returned today, now
 * with an optional `metrics` block. Extra service-specific fields (db,
 * scheduler, version, queue, …) still come through untyped.
 */
export type ServiceStatus = {
  status: string
  metrics?: ServiceMetrics
  [key: string]: unknown
}

export type HealthResponse = {
  service: string
  status: string
  metrics?: ServiceMetrics
  'auth-service': ServiceStatus
  'ai-agent': ServiceStatus
  /** db, scheduler, and any future top-level orchestrator fields. */
  [key: string]: unknown
}

export type CheckStatus = 'pass' | 'fail'

export type Check = {
  /** Machine-readable check name, shown as the card label. */
  name: string
  status: CheckStatus
  /** Single formatted line of detail, already display-ready. */
  detail: string
}

export type ChecksResponse = {
  checks: Check[]
}

export type InfraStatus = 'ready' | 'degraded' | 'unreachable'

export type InfraComponent = {
  /** Display name, e.g. "Postgres 16". */
  name: string
  status: InfraStatus
  /** Optional display word when it differs from `status` (e.g. "connected"). */
  label?: string
  /** Port the service is reachable on inside polypilot-net. */
  port: number
  /** Single formatted line of service-specific detail, already display-ready. */
  detail: string
}

export type InfrastructureResponse = {
  components: InfraComponent[]
}
