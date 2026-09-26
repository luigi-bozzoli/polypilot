import type { ChecksResponse, HealthResponse, InfrastructureResponse } from '../../features/health/types'

export const HEALTH_RESPONSE: HealthResponse = {
  service: 'orchestrator',
  status: 'UP',
  metrics: { uptime: '3d 04h', latencyMs: null, detail: 'self' },
  db: { status: 'UP', detail: 'Postgres 16 · 4/10 connections' },
  scheduler: { status: 'UP', detail: '3 jobs scheduled' },
  'auth-service': {
    status: 'UP',
    metrics: { uptime: '3d 04h', latencyMs: 18, detail: 'web3.py 6.x' },
  },
  'ai-agent': {
    status: 'UP',
    metrics: { uptime: '3d 04h', latencyMs: 42, detail: 'LangGraph stub — /ai/analyze not implemented' },
  },
}

export const CHECKS_RESPONSE: ChecksResponse = {
  checks: [
    { name: '/actuator/health', status: 'pass', detail: 'UP — no unauthenticated failures' },
    { name: 'spring.sql.init', status: 'pass', detail: 'schema initialized on boot' },
    { name: 'JPA validate', status: 'pass', detail: '14 entities validated against schema' },
  ],
}

/** Redis is shown `degraded` on purpose — exercises the non-`ready` infra state at least once. */
export const INFRASTRUCTURE_RESPONSE: InfrastructureResponse = {
  components: [
    { name: 'Postgres 16', status: 'ready', label: 'connected', port: 5432, detail: '4/10 connections' },
    { name: 'Redis 7', status: 'degraded', label: 'high latency', port: 6379, detail: 'PING 220ms (usually <5ms)' },
    { name: 'RabbitMQ 3', status: 'ready', label: 'connected', port: 5672, detail: 'ai.signals queue · 0 pending' },
  ],
}
