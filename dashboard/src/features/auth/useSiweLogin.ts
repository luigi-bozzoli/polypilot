import { useCallback, useState } from 'react'
import { useConnection, useSignMessage, useSwitchChain } from 'wagmi'
import { mainnet } from 'wagmi/chains'
import { createSiweMessage } from 'viem/siwe'
import { getSiweNonce, siweLogin } from './authApi'
import { LoginResponse } from './types'
import { ALLOWED_CHAIN_IDS } from '../wallet/wagmiConfig'

export type SiweLoginStatus =
  | 'idle'
  | 'switching-chain'
  | 'preparing'
  | 'awaiting-signature'
  | 'verifying'

type Options = {
  /** Called with the JWT payload once verification succeeds. Same shape as password login. */
  onSuccess: (result: LoginResponse) => void
}

/**
 * Steps 4–8 of the SIWE login flow: nonce → SIWE message → `personal_sign` →
 * `POST /api/auth/siwe/verify`. Connecting the wallet (steps 1–3) is the
 * caller's job; it hands us the connected `address`.
 *
 * The chain id baked into the message is read live from the connector
 * (`connector.getChainId()` → an `eth_chainId` call), never from a value
 * captured at render time. `personal_sign` is chain-agnostic, so a wallet
 * sitting on an unsupported chain would otherwise sign a message that names
 * mainnet without any error. We re-read it after signing too, to catch a
 * network switch made while the wallet prompt was open.
 *
 * The message's `domain` / `uri` are taken from the running origin so each
 * environment presents exactly what its backend `siwe.domain` / `siwe.uri`
 * expects — nothing is hard-coded. The nonce is used once and never stored.
 */
export function useSiweLogin({ onSuccess }: Options) {
  // wagmi 3 deprecated the named `signMessageAsync` / `switchChainAsync` aliases
  // in favour of the generic mutation `mutateAsync`; rename on destructure so the
  // rest of the hook reads the same.
  const { connector } = useConnection()
  const { mutateAsync: signMessageAsync } = useSignMessage()
  const { mutateAsync: switchChainAsync } = useSwitchChain()
  const [status, setStatus] = useState<SiweLoginStatus>('idle')
  const [error, setError] = useState<string | null>(null)

  const signIn = useCallback(
    async (address: `0x${string}`) => {
      setError(null)
      try {
        if (!connector) {
          throw new Error('Connect a wallet before signing in.')
        }

        // The orchestrator only accepts SIWE messages for its allowed chains.
        // Ask the wallet which chain it is actually on right now.
        let activeChainId = await connector.getChainId()
        if (!ALLOWED_CHAIN_IDS.includes(activeChainId)) {
          setStatus('switching-chain')
          try {
            const switched = await switchChainAsync({ chainId: mainnet.id })
            activeChainId = switched.id
          } catch {
            throw new Error('Switch your wallet to Ethereum or Polygon to sign in.')
          }
        }

        setStatus('preparing')
        const nonce = await getSiweNonce()

        const issuedAt = new Date()
        const expirationTime = new Date(issuedAt.getTime() + 5 * 60_000)
        const message = createSiweMessage({
          address,
          chainId: activeChainId,
          domain: window.location.host,
          uri: window.location.origin,
          nonce,
          version: '1',
          issuedAt,
          expirationTime,
        })

        setStatus('awaiting-signature')
        const signature = await signMessageAsync({ message })

        // The wallet can switch networks while the signature prompt is open.
        // The signed message pins `activeChainId`, so bail rather than send a
        // message that misreports the chain it was signed on.
        const chainIdAfterSigning = await connector.getChainId()
        if (chainIdAfterSigning !== activeChainId) {
          throw new Error(
            'Your wallet switched networks during signing. Please try again.',
          )
        }

        setStatus('verifying')
        const result = await siweLogin(message, signature)

        setStatus('idle')
        onSuccess(result)
      } catch (err) {
        setStatus('idle')
        setError(toMessage(err))
      }
    },
    [connector, onSuccess, signMessageAsync, switchChainAsync],
  )

  return { signIn, status, error, clearError: () => setError(null) }
}

function toMessage(err: unknown): string {
  if (err && typeof err === 'object') {
    const name = 'name' in err ? String((err as { name?: unknown }).name) : ''
    const msg = 'message' in err ? String((err as { message?: unknown }).message) : ''
    if (
      name === 'UserRejectedRequestError' ||
      /user rejected|user denied|rejected the request|request rejected/i.test(msg)
    ) {
      return 'Signature request rejected in your wallet.'
    }
    if (msg) return msg
  }
  return 'Wallet sign-in failed. Please try again.'
}
