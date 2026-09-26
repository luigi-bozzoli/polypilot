import type { InputHTMLAttributes, ReactNode } from 'react'
import { useId } from 'react'

type FormFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  hint?: string
  error?: string
  suffix?: ReactNode
}

export function FormField({ label, hint, error, suffix, id, className = '', ...rest }: FormFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const hintId = hint ? `${inputId}-hint` : undefined
  const errorId = error ? `${inputId}-error` : undefined

  return (
    <div className="mb-4">
      <label htmlFor={inputId} className="mb-1.5 block text-[10px] uppercase tracking-wider text-text-muted">
        {label}
      </label>
      <div className="relative">
        <input
          id={inputId}
          className={`w-full rounded-md border bg-bg3 px-3 py-2.5 font-mono text-[13px] text-text-primary outline-none transition-colors placeholder:text-text-muted focus:border-accent-border ${
            error ? 'border-red-border' : 'border-border'
          } ${suffix ? 'pr-12' : ''} ${className}`}
          aria-invalid={error ? true : undefined}
          aria-describedby={[hintId, errorId].filter(Boolean).join(' ') || undefined}
          {...rest}
        />
        {suffix && (
          <span className="absolute right-2 top-1/2 -translate-y-1/2 text-[11px] text-text-muted">{suffix}</span>
        )}
      </div>
      {hint && !error && (
        <p id={hintId} className="mt-1 text-[11px] text-text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="mt-1 text-[11px] text-red">
          {error}
        </p>
      )}
    </div>
  )
}
