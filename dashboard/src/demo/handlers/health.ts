import { http, HttpResponse } from 'msw'
import { api, latency } from '../util'
import { CHECKS_RESPONSE, HEALTH_RESPONSE, INFRASTRUCTURE_RESPONSE } from '../fixtures/health'

export const healthHandlers = [
  http.get(api('/health'), async () => {
    await latency()
    return HttpResponse.json(HEALTH_RESPONSE)
  }),
  http.get(api('/health/checks'), async () => {
    await latency()
    return HttpResponse.json(CHECKS_RESPONSE)
  }),
  http.get(api('/health/infrastructure'), async () => {
    await latency()
    return HttpResponse.json(INFRASTRUCTURE_RESPONSE)
  }),
]
