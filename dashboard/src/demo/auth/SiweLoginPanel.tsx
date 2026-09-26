import { useState } from 'react'
import { Wallet } from 'lucide-react'
import { getSiweNonce, siweLogin } from '../../features/auth/authApi'
import { LoginResponse } from '../../features/auth/types'
import { Button } from '../../components/auth/Button'
import { DEMO_WALLET_ADDRESS } from '../fixtures/auth'

type SiweLoginPanelProps = {
  onSuccess: (result: LoginResponse) => void
}

type Status = 'idle' | 'connecting' | 'connected' | 'preparing' | 'awaiting-signature' | 'verifying'

function truncateAddress(address: string): string {
  return `${address.slice(0, 6)}…${address.slice(-4)}`
}

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Demo replacement for `components/auth/SiweLoginPanel.tsx` (aliased in `vite.config.ts`, only
 * for `mode === 'demo'`). Same props signature as the real component. No wagmi hook is called —
 * there is no wallet to connect to in a backend-less demo — but it still walks through the same
 * visible states the real flow has (connecting → signing → verifying, ~700-900ms each) and still
 * calls the real `getSiweNonce`/`siweLogin` API functions (answered by `demo/handlers/auth.ts`),
 * so the success path into `AuthContext` is exercised exactly as it is for real.
 */
export function SiweLoginPanel({ onSuccess }: SiweLoginPanelProps) {
  const [status, setStatus] = useState<Status>('idle')
  const [error, setError] = useState<string | null>(null)

  const busy = status !== 'idle' && status !== 'connected'

  const handleConnect = async () => {
    setError(null)
    setStatus('connecting')
    await wait(800)
    setStatus('connected')
  }

  const handleSignIn = async () => {
    setError(null)
    try {
      setStatus('preparing')
      await getSiweNonce()
      await wait(800)

      setStatus('awaiting-signature')
      await wait(800)

      setStatus('verifying')
      const result = await siweLogin('demo-siwe-message', 'demo-signature')
      setStatus('idle')
      onSuccess(result)
    } catch (err) {
      setStatus('connected')
      setError(err instanceof Error ? err.message : 'Wallet sign-in failed. Please try again.')
    }
  }

  const signInLabel = (() => {
    switch (status) {
      case 'preparing':
        return 'Preparing…'
      case 'awaiting-signature':
        return 'Check your wallet…'
      case 'verifying':
        return 'Signing in…'
      default:
        return 'Sign-In With Ethereum'
    }
  })()

  return (
    <div className="flex flex-col gap-3">
      {error && <p className="text-[12px] text-red">{error}</p>}

      {status === 'idle' || status === 'connecting' ? (
        <Button type="button" onClick={() => void handleConnect()} loading={status === 'connecting'}>
          {status !== 'connecting' && <Wallet className="h-4 w-4" aria-hidden="true" />}
          {status === 'connecting' ? 'Connecting…' : 'Connect Wallet'}
        </Button>
      ) : (
        <>
          <div className="flex items-center gap-2 rounded-md border border-border bg-bg3 px-3 py-2.5 font-mono text-[12px] text-text-secondary">
            <span className="h-2 w-2 flex-shrink-0 rounded-full bg-accent" />
            {truncateAddress(DEMO_WALLET_ADDRESS)}
          </div>

          <Button type="button" onClick={() => void handleSignIn()} loading={busy}>
            {!busy && <Wallet className="h-4 w-4" aria-hidden="true" />}
            {signInLabel}
          </Button>
        </>
      )}
    </div>
  )
}
