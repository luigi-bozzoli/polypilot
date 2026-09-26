import { useState } from 'react'
import { Wallet } from 'lucide-react'
import { useConnect, useConnection, useConnectors, useDisconnect } from 'wagmi'
import type { Connector } from 'wagmi'
import { useSiweLogin } from '../../features/auth/useSiweLogin'
import { LoginResponse } from '../../features/auth/types'
import { Button } from './Button'
import { FormError } from './FormError'

type SiweLoginPanelProps = {
  onSuccess: (result: LoginResponse) => void
}

function truncateAddress(address: string): string {
  return `${address.slice(0, 6)}…${address.slice(-4)}`
}

/**
 * Primary sign-in: Connect Wallet → Sign-In With Ethereum. On success it hands
 * the JWT to {@code onSuccess}, which stores it through the same
 * session / AuthContext path the admin form uses.
 */
export function SiweLoginPanel({ onSuccess }: SiweLoginPanelProps) {
  const { address, isConnected } = useConnection()
  // wagmi 3 deprecated the named `connectAsync` / `disconnect` aliases in favour
  // of the generic mutation `mutateAsync` / `mutate`; rename on destructure.
  const {
    mutateAsync: connectAsync,
    isPending: connecting,
    error: connectError,
    reset: resetConnect,
  } = useConnect()
  const connectors = useConnectors()
  const { mutate: disconnect } = useDisconnect()
  const { signIn, status, error: siweError, clearError } = useSiweLogin({ onSuccess })

  const [showPicker, setShowPicker] = useState(false)

  const signingIn = status !== 'idle'
  const busy = connecting || signingIn

  const runConnect = async (connector: Connector) => {
    resetConnect()
    clearError()
    setShowPicker(false)
    // Rejections surface via `connectError`; swallow so it isn't unhandled.
    await connectAsync({ connector }).catch(() => undefined)
  }

  const handleConnectClick = () => {
    const only = connectors[0]
    if (connectors.length === 1 && only) {
      void runConnect(only)
      return
    }
    setShowPicker((prev) => !prev)
  }

  const handleSignIn = () => {
    if (!address) return
    void signIn(address)
  }

  const signInLabel = (() => {
    switch (status) {
      case 'switching-chain':
        return 'Switch network in your wallet…'
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

  const errorMessage = siweError ?? connectError?.message ?? null

  return (
    <div className="flex flex-col gap-3">
      {errorMessage && <FormError message={errorMessage} />}

      {isConnected && address ? (
        <>
          <div className="flex items-center gap-2 rounded-md border border-border bg-bg3 px-3 py-2.5 font-mono text-[12px] text-text-secondary">
            <span className="h-2 w-2 flex-shrink-0 rounded-full bg-accent" />
            {truncateAddress(address)}
          </div>

          <Button type="button" onClick={handleSignIn} loading={busy}>
            {!busy && <Wallet className="h-4 w-4" aria-hidden="true" />}
            {signInLabel}
          </Button>

          <button
            type="button"
            onClick={() => disconnect()}
            disabled={busy}
            className="text-[11px] text-text-muted transition-colors hover:text-text-primary disabled:cursor-not-allowed disabled:opacity-60"
          >
            Use a different wallet
          </button>
        </>
      ) : (
        <>
          <Button type="button" onClick={handleConnectClick} loading={connecting}>
            {!connecting && <Wallet className="h-4 w-4" aria-hidden="true" />}
            {connecting ? 'Connecting…' : 'Connect Wallet'}
          </Button>

          {showPicker && connectors.length > 1 && (
            <div className="overflow-hidden rounded-md border border-border-mid bg-bg2">
              {connectors.map((connector) => (
                <button
                  key={connector.uid}
                  type="button"
                  onClick={() => runConnect(connector)}
                  className="flex w-full items-center gap-2 px-3 py-2.5 text-left text-[13px] text-text-primary transition-colors hover:bg-bg3"
                >
                  {connector.icon ? (
                    <img
                      src={connector.icon}
                      alt=""
                      className="h-4 w-4 flex-shrink-0"
                      aria-hidden="true"
                    />
                  ) : (
                    <Wallet
                      className="h-4 w-4 flex-shrink-0 text-text-secondary"
                      aria-hidden="true"
                    />
                  )}
                  {connector.name}
                </button>
              ))}
            </div>
          )}

          {connectors.length === 0 && (
            <p className="text-[11px] text-text-muted">
              No wallet detected — install a browser wallet such as MetaMask, or configure
              WalletConnect to use a mobile wallet.
            </p>
          )}
        </>
      )}
    </div>
  )
}
