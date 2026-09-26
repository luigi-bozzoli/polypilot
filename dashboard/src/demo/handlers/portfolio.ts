import { http, HttpResponse } from 'msw'
import { api, latency } from '../util'
import { PORTFOLIO_SUMMARY } from '../fixtures/portfolio'

export const portfolioHandlers = [
  http.get(api('/portfolio/summary'), async () => {
    await latency()
    return HttpResponse.json(PORTFOLIO_SUMMARY)
  }),
]
