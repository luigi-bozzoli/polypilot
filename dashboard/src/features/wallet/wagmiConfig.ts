import { createConfig, http } from 'wagmi'
import { mainnet, polygon } from 'wagmi/chains'
import { walletConnect } from 'wagmi/connectors'

/**
 * Chain ids the SIWE flow may declare. These MUST stay in lockstep with the
 * orchestrator's `siwe.allowed-chain-ids` (`orchestrator` application.yml →
 * `${SIWE_ALLOWED_CHAIN_IDS:1,137}` = Ethereum mainnet + Polygon). A SIWE
 * message naming any other chain id is rejected at
 * `POST /api/auth/siwe/verify`, so we never configure one here.
 */
export const ALLOWED_CHAIN_IDS: readonly number[] = [mainnet.id, polygon.id]

// Optional — only needed for the WalletConnect (mobile wallet) path.
const walletConnectProjectId = import.meta.env.VITE_WALLETCONNECT_PROJECT_ID ?? ''

export const wagmiConfig = createConfig({
  chains: [mainnet, polygon],
  // Browser-extension wallets are added automatically by wagmi's EIP-6963
  // discovery (`multiInjectedProviderDiscovery`, on by default), one connector
  // per installed wallet — so no explicit `injected()` is needed here, and
  // adding one only produces a duplicate generic "Injected" row in the picker.
  // The only connector we register by hand is WalletConnect for mobile wallets
  // via QR / deep link, and only when a project id is configured — otherwise it
  // would surface as a dead row that errors when picked.
  connectors: walletConnectProjectId
    ? [walletConnect({ projectId: walletConnectProjectId })]
    : [],
  transports: {
    [mainnet.id]: http(),
    [polygon.id]: http(),
  },
})
