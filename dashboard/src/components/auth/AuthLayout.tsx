import { Bot } from 'lucide-react'
import type { ReactNode } from 'react'
import { AuthCard } from './AuthCard'

type AuthLayoutProps = {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}

export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg0 p-6">
      <AuthCard>
        <div className="flex items-center gap-3 border-b border-border px-6 py-5">
          <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg border border-accent-border bg-accent-dim">
            <Bot className="h-4 w-4 text-accent" aria-hidden="true" />
          </div>
          <div>
            <div className="text-[15px] font-medium text-text-primary">{title}</div>
            <div className="mt-0.5 text-[12px] text-text-muted">{subtitle}</div>
          </div>
        </div>

        <div className="px-6 py-6">{children}</div>

        <div className="border-t border-border bg-bg1 px-6 py-4 text-center text-[12px] text-text-muted">
          {footer}
        </div>
      </AuthCard>
    </div>
  )
}
