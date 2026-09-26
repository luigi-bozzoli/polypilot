import { useRef, useState } from 'react'
import { Wallet } from 'lucide-react'
import { useConnect, useConnection, useConnectors, useDisconnect } from 'wagmi'
import type { Connector } from 'wagmi'
import { useOnClickOutside } from '../../hooks/useOnClickOutside'

function truncateAddress(address: string): string {
  return `${address.slice(0, 6)}…${address.slice(-4)}`
}

export function ConnectWalletButton() {
  const { address, isConnected } = useConnection()
  const { mutate: connect, isPending, error } = useConnect()
  const connectors = useConnectors()
  const { mutate: disconnect } = useDisconnect()
  const [showPicker, setShowPicker] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useOnClickOutside(containerRef, () => setShowPicker(false))

  if (isConnected && address) {
    return (
      <button
        type="button"
        onClick={() => disconnect()}
        title="Click to disconnect"
        className="flex items-center gap-2 rounded-md border border-border-mid bg-bg3 px-3 py-2 text-[13px] font-medium font-sans text-text-primary transition-colors hover:bg-bg4"
      >
        <span className="h-2 w-2 rounded-full bg-accent" />
        {truncateAddress(address)}
      </button>
    )
  }

  const handleConnect = (connector: Connector) => {
    setShowPicker(false)
    connect({ connector })
  }

  const handleButtonClick = () => {
    if (connectors.length > 1) {
      setShowPicker((prev) => !prev)
      return
    }
    if (connectors[0]) handleConnect(connectors[0])
  }

  return (
    <div ref={containerRef} className="relative flex flex-col items-end gap-1">
      <button
        type="button"
        onClick={handleButtonClick}
        disabled={isPending}
        className="flex items-center gap-2 rounded-md border border-accent bg-accent px-3 py-2 text-[13px] font-medium font-sans text-black transition-colors hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
      >
        <Wallet className="h-4 w-4" aria-hidden="true" />
        {isPending ? 'Connecting…' : 'Connect Wallet'}
      </button>

      {showPicker && connectors.length > 1 && (
        <div className="absolute top-full z-10 mt-1 w-56 overflow-hidden rounded-md border border-border-mid bg-bg2 shadow-[0_12px_32px_rgba(0,0,0,0.5)]">
          {connectors.map((connector) => (
            <button
              key={connector.uid}
              type="button"
              onClick={() => handleConnect(connector)}
              className="flex w-full items-center gap-2 px-3 py-2.5 text-left text-[13px] font-sans text-text-primary transition-colors hover:bg-bg3"
            >
              {connector.icon ? (
                <img src={connector.icon} alt="" className="h-4 w-4 flex-shrink-0" aria-hidden="true" />
              ) : (
                <Wallet className="h-4 w-4 flex-shrink-0 text-text-secondary" aria-hidden="true" />
              )}
              {connector.name}
            </button>
          ))}
        </div>
      )}

      {error && (
        <span className="text-[11px] text-red">
          {error.message.includes('provider') ? 'No wallet provider found — install MetaMask' : error.message}
        </span>
      )}
    </div>
  )
}
