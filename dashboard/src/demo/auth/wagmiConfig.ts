import { createConfig, http } from 'wagmi'
import { mainnet, polygon } from 'wagmi/chains'

/**
 * Demo replacement for `features/wallet/wagmiConfig.ts`, aliased in only when
 * `mode === 'demo'` (see `vite.config.ts`). `main.tsx`'s `WagmiProvider` still needs a real
 * `Config` object (it isn't itself aliased — see `src/demo/README.md`), but the demo login flow
 * (`demo/auth/SiweLoginPanel.tsx`) never calls a single wagmi hook, so this config is never
 * exercised. It differs from the real one only to make that airtight: no connectors (so nothing
 * is ever offered to connect to) and injected-wallet discovery turned off (so a real extension in
 * the visitor's browser, e.g. MetaMask, is never touched) — satisfying "no wallet-related network
 * activity" even if some future real-code path were mistakenly wired up to it.
 */
export const ALLOWED_CHAIN_IDS: typeof import('../../features/wallet/wagmiConfig').ALLOWED_CHAIN_IDS = [
  mainnet.id,
  polygon.id,
]

export const wagmiConfig: typeof import('../../features/wallet/wagmiConfig').wagmiConfig = createConfig({
  chains: [mainnet, polygon],
  connectors: [],
  multiInjectedProviderDiscovery: false,
  transports: {
    [mainnet.id]: http(),
    [polygon.id]: http(),
  },
})
