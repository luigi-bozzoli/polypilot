/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * WalletConnect Cloud project id, used by the `walletConnect` wagmi connector
   * for mobile-wallet sign-in. Optional: when unset, only the injected
   * (browser-extension) connector is offered. See the dashboard README.
   */
  readonly VITE_WALLETCONNECT_PROJECT_ID?: string
  /**
   * Demo build-mode switch, set via `.env.demo` (`--mode demo`). Compared with
   * the literal `=== 'true'` at every branch point so Vite can dead-code-eliminate
   * the demo import chain out of real builds. See `dashboard/src/demo/README.md`.
   */
  readonly VITE_DEMO?: 'true' | 'false'
  /** Repo URL shown by the demo banner's "View source" link. Demo builds only. */
  readonly VITE_REPO_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
