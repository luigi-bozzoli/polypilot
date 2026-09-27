import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { WagmiProvider } from 'wagmi'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { wagmiConfig } from './features/wallet/wagmiConfig'
import { AuthProvider } from './features/auth/AuthContext'

const queryClient = new QueryClient()

function renderApp() {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <WagmiProvider config={wagmiConfig}>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <BrowserRouter basename={import.meta.env.BASE_URL}>
              <App />
            </BrowserRouter>
          </AuthProvider>
        </QueryClientProvider>
      </WagmiProvider>
    </StrictMode>
  )
}

async function bootstrap() {
  // Literal comparison (not a variable/helper) so Vite dead-code-eliminates this whole branch,
  // and the dynamic import's chunk, out of real builds.
  if (import.meta.env.VITE_DEMO === 'true') {
    const { startDemo } = await import('./demo/start')
    await startDemo() // must finish (worker active) before first render
  }
  renderApp()
}

void bootstrap()
