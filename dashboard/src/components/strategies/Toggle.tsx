/**
 * Shared track/knob slide switch (`role="switch"`) — used by `StrategyDetailPage`
 * (enabled toggle, and the read-only dry-run/live indicator) and `StepEntryRules`
 * (dry-run toggle in the create-strategy flow). Previously three near-identical,
 * independently hand-rolled copies; two of them (`StrategyDetailPage`'s) omitted
 * `left-0.5` on the knob, so its position was undefined ("auto") inside the
 * parent's `flex` layout instead of pinned to the track — a known CSS quirk for
 * absolutely-positioned children of flex containers — which is what pushed the
 * knob out of the track. Fixed here once, for every occurrence.
 *
 * Omit `onChange` for a read-only indicator (`StrategyDetailPage`'s live-trading
 * status, which this page offers no control for) — it renders as a plain
 * `<span aria-disabled>` instead of a `<button>`, matching the previous
 * `StateIndicator`/`EnabledToggle` split.
 */
type ToggleProps = {
  checked: boolean
  onChange?: () => void
  disabled?: boolean
  className?: string
  'aria-label'?: string
}

export function Toggle({ checked, onChange, disabled = false, className = '', ...aria }: ToggleProps) {
  const trackClassName = `relative inline-block h-[18px] w-[34px] flex-shrink-0 rounded-full border p-0 transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
    checked ? 'border-accent bg-accent' : 'border-border-mid bg-bg4'
  } ${className}`

  const knob = (
    <span
      className={`absolute left-0.5 top-0.5 h-3 w-3 rounded-full transition-transform ${
        checked ? 'translate-x-4 bg-black' : 'translate-x-0 bg-text-muted'
      }`}
    />
  )

  if (!onChange) {
    return (
      <span role="switch" aria-checked={checked} aria-disabled="true" className={trackClassName} {...aria}>
        {knob}
      </span>
    )
  }

  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={onChange}
      className={trackClassName}
      {...aria}
    >
      {knob}
    </button>
  )
}
