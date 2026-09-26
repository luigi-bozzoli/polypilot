import type { CheckStatus, InfraStatus } from './types'

export type StatusTone = 'green' | 'yellow' | 'red'

/**
 * Maps a backend status string to the mock's 3-tone status scale
 * (`.dot.green/.yellow/.red`). `"ok"` is the only success value the real
 * `/api/health` emits today; `"unreachable"` comes from the orchestrator's own
 * probe fallback. Anything else (including absent) is shown as degraded
 * rather than guessed as healthy or down.
 */
export function toneForStatus(status: string | undefined): StatusTone {
  if (status === 'ok') return 'green'
  if (status === 'unreachable') return 'red'
  return 'yellow'
}

/** Infrastructure component status (`/api/health/infrastructure`). */
export function toneForInfraStatus(status: InfraStatus | undefined): StatusTone {
  if (status === 'ready') return 'green'
  if (status === 'unreachable') return 'red'
  return 'yellow' // degraded, or an unrecognised value
}

/** Deploy / schema check status (`/api/health/checks`). */
export function toneForCheckStatus(status: CheckStatus | undefined): StatusTone {
  if (status === 'pass') return 'green'
  if (status === 'fail') return 'red'
  return 'yellow'
}
