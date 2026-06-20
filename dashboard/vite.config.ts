import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',   // expose to Docker port mapping
    port: 5173,
    hmr: {
      clientPort: 5173  // tells the browser which port to use for HMR websocket
    },
    proxy: {
      // All /api/* calls from the browser are forwarded to the orchestrator
      // inside the Docker network. The browser never talks to it directly.
      '/api': {
        target: 'http://orchestrator:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
