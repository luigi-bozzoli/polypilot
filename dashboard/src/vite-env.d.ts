/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * WalletConnect Cloud project id, used by the `walletConnect` wagmi connector
   * for mobile-wallet sign-in. Optional: when unset, only the injected
   * (browser-extension) connector is offered. See the dashboard README.
   */
  readonly VITE_WALLETCONNECT_PROJECT_ID?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
