import { http, HttpResponse } from 'msw'
import { api, latency } from '../util'
import { DEMO_WALLET_ADDRESS, demoPasswordLogin, demoWalletLogin } from '../fixtures/auth'

/**
 * `POST /api/auth/login` accepts any credentials in demo mode. `demo/auth/SiweLoginPanel.tsx`
 * (aliased in for the wallet flow) still calls the real `authApi.getSiweNonce`/`authApi.siweLogin`
 * functions against these two handlers, rather than fabricating a `LoginResponse` locally, so the
 * request/response path through `AuthContext` is identical to the real flow.
 */
export const authHandlers = [
  http.post(api('/auth/login'), async () => {
    await latency()
    return HttpResponse.json(demoPasswordLogin())
  }),
  http.get(api('/auth/siwe/nonce'), async () => {
    await latency()
    return HttpResponse.json({ nonce: 'demo-nonce' })
  }),
  http.post(api('/auth/siwe/verify'), async () => {
    await latency()
    return HttpResponse.json(demoWalletLogin(DEMO_WALLET_ADDRESS))
  }),
]
