import { FormField } from '../auth/FormField'

type StepRiskScheduleProps = {
  maxBetSize: string
  maxDailyExposure: string
  stopLossThreshold: string
  cronExpression: string
  onMaxBetSizeChange: (value: string) => void
  onMaxDailyExposureChange: (value: string) => void
  onStopLossThresholdChange: (value: string) => void
  onCronExpressionChange: (value: string) => void
  errors: { maxBetSize?: string; maxDailyExposure?: string; stopLossThreshold?: string; cronExpression?: string }
}

const CRON_PRESETS = [
  { value: '0 */5 * * * *', label: 'Every 5 minutes' },
  { value: '0 */30 * * * *', label: 'Every 30 minutes' },
  { value: '0 0 * * * *', label: 'Every hour' },
]

export function StepRiskSchedule({
  maxBetSize,
  maxDailyExposure,
  stopLossThreshold,
  cronExpression,
  onMaxBetSizeChange,
  onMaxDailyExposureChange,
  onStopLossThresholdChange,
  onCronExpressionChange,
  errors,
}: StepRiskScheduleProps) {
  const dailyBets =
    Number(maxBetSize) > 0 && Number(maxDailyExposure) > 0 ? Math.floor(Number(maxDailyExposure) / Number(maxBetSize)) : null

  return (
    <div>
      <div className="grid grid-cols-2 gap-3.5">
        <FormField
          label="Max bet size (USD)"
          placeholder="100"
          value={maxBetSize}
          onChange={(e) => onMaxBetSizeChange(e.target.value)}
          error={errors.maxBetSize}
        />
        <FormField
          label="Max daily exposure (USD)"
          placeholder="1000"
          value={maxDailyExposure}
          onChange={(e) => onMaxDailyExposureChange(e.target.value)}
          error={errors.maxDailyExposure}
        />
      </div>

      <div className="grid grid-cols-2 gap-3.5">
        <FormField
          label="Stop-loss threshold (optional)"
          hint="0–1, fraction of position value"
          placeholder="0.15"
          value={stopLossThreshold}
          onChange={(e) => onStopLossThresholdChange(e.target.value)}
          error={errors.stopLossThreshold}
        />
        <div className="mb-4">
          <label htmlFor="cron-expression" className="mb-1.5 block text-[10px] uppercase tracking-wider text-text-muted">
            Run schedule (cron)
          </label>
          <select
            id="cron-expression"
            className="w-full rounded-md border border-border bg-bg3 px-3 py-2.5 font-mono text-[12px] text-text-primary outline-none focus:border-accent-border"
            value={CRON_PRESETS.some((p) => p.value === cronExpression) ? cronExpression : ''}
            onChange={(e) => onCronExpressionChange(e.target.value)}
          >
            <option value="" disabled>
              Choose a schedule…
            </option>
            {CRON_PRESETS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label} — {p.value}
              </option>
            ))}
          </select>
          {errors.cronExpression && <p className="mt-1 text-[11px] text-red">{errors.cronExpression}</p>}
        </div>
      </div>

      <div className="mb-1.5 text-[10px] uppercase tracking-wider text-text-muted">Risk preview</div>
      <div className="grid grid-cols-3 gap-3 rounded-lg border border-border bg-bg3 p-3.5">
        <div>
          <div className="mb-1 text-[10px] uppercase tracking-wider text-text-muted">Max per trade</div>
          <div className="font-mono text-[16px] text-text-primary">{maxBetSize ? `$${maxBetSize}` : '—'}</div>
        </div>
        <div>
          <div className="mb-1 text-[10px] uppercase tracking-wider text-text-muted">Daily cap</div>
          <div className="font-mono text-[16px] text-accent">{maxDailyExposure ? `$${maxDailyExposure}` : '—'}</div>
        </div>
        <div>
          <div className="mb-1 text-[10px] uppercase tracking-wider text-text-muted">Trades / day at cap</div>
          <div className={`font-mono text-[16px] ${dailyBets !== null && dailyBets < 3 ? 'text-yellow' : 'text-text-primary'}`}>
            {dailyBets ?? '—'}
          </div>
        </div>
      </div>
    </div>
  )
}
