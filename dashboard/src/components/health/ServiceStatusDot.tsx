import type { StatusTone } from '../../features/health/statusTone'

const TONE_CLASSES: Record<StatusTone, string> = {
  green: 'bg-accent',
  yellow: 'bg-yellow',
  red: 'bg-red',
}

/** Small status dot (mock `.dot`). */
export function ServiceStatusDot({ tone }: { tone: StatusTone }) {
  return <span className={`h-[9px] w-[9px] shrink-0 rounded-full ${TONE_CLASSES[tone]}`} aria-hidden="true" />
}
