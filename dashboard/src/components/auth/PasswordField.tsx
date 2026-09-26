import { Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import { FormField } from './FormField'
import type { InputHTMLAttributes } from 'react'

type PasswordFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'id'> & {
  label: string
  hint?: string
  error?: string
  id: string
}

export function PasswordField({ label, hint, error, id, ...rest }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)

  return (
    <FormField
      id={id}
      label={label}
      hint={hint}
      error={error}
      type={visible ? 'text' : 'password'}
      suffix={
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-pressed={visible}
          aria-label={visible ? 'Hide password' : 'Show password'}
          className="flex h-6 w-6 items-center justify-center rounded text-text-muted hover:text-text-primary"
        >
          {visible ? <EyeOff className="h-3.5 w-3.5" aria-hidden="true" /> : <Eye className="h-3.5 w-3.5" aria-hidden="true" />}
        </button>
      }
      {...rest}
    />
  )
}
