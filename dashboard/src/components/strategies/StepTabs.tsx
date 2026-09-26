import { Check } from 'lucide-react'

const STEPS = [
  { num: 1, label: 'Basics' },
  { num: 2, label: 'Entry rules' },
  { num: 3, label: 'Risk & schedule' },
] as const

export function StepTabs({ current }: { current: 1 | 2 | 3 }) {
  return (
    <div className="flex flex-shrink-0 gap-0 border-b border-border px-6">
      {STEPS.map((step) => {
        const done = step.num < current
        const active = step.num === current
        return (
          <div
            key={step.num}
            className={`-mb-px flex items-center gap-1.5 border-b-2 px-4 py-3 text-[12px] ${
              active
                ? 'border-accent text-accent'
                : done
                  ? 'border-transparent text-text-secondary'
                  : 'border-transparent text-text-muted'
            }`}
          >
            <span
              className={`flex h-[18px] w-[18px] items-center justify-center rounded border font-mono text-[9px] ${
                done
                  ? 'border-accent bg-accent text-black'
                  : active
                    ? 'border-accent-border bg-accent-dim text-accent'
                    : 'border-border bg-bg4 text-text-muted'
              }`}
            >
              {done ? <Check className="h-2.5 w-2.5" aria-hidden="true" /> : step.num}
            </span>
            {step.label}
          </div>
        )
      })}
    </div>
  )
}
