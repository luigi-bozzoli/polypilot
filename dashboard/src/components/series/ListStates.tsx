import { Loader2 } from 'lucide-react'
import type { ReactNode } from 'react'

function Panel({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-lg border border-border bg-bg1 px-4 py-8 text-center text-[13px] text-text-secondary">
      {children}
    </div>
  )
}

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <Panel>
      <Loader2 className="mx-auto mb-2 h-4 w-4 animate-spin text-text-muted" aria-hidden="true" />
      {label}
    </Panel>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="rounded-lg border border-red-border bg-red-dim px-4 py-6 text-center text-[13px] text-red">
      <p>{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 rounded border border-red-border px-3 py-1 text-[12px] text-red hover:bg-red-dim"
        >
          Retry
        </button>
      )}
    </div>
  )
}

export function Empty({ message }: { message: string }) {
  return <Panel>{message}</Panel>
}
