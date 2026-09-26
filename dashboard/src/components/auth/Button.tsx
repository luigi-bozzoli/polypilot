import { Loader2 } from 'lucide-react'
import type { ButtonHTMLAttributes } from 'react'

type ButtonVariant = 'primary' | 'secondary'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  loading?: boolean
}

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-accent text-black border-accent hover:opacity-90',
  secondary: 'bg-bg3 text-text-secondary border-border-mid hover:bg-bg4 hover:text-text-primary',
}

export function Button({ variant = 'primary', loading = false, disabled, className = '', children, ...rest }: ButtonProps) {
  return (
    <button
      className={`w-full flex items-center justify-center gap-2 rounded-md border px-4 py-2.5 text-[13px] font-medium font-sans transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${variantClasses[variant]} ${className}`}
      disabled={disabled || loading}
      {...rest}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
      {children}
    </button>
  )
}
