import type { ReactNode } from 'react'

type AuthCardProps = {
  children: ReactNode
}

export function AuthCard({ children }: AuthCardProps) {
  return (
    <div className="w-full max-w-[420px] overflow-hidden rounded-2xl border border-border-mid bg-bg1 shadow-[0_24px_64px_rgba(0,0,0,0.6)]">
      {children}
    </div>
  )
}
