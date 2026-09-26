import { http, HttpResponse } from 'msw'
import { api, latency } from '../util'
import { INDICATOR_CATALOG } from '../fixtures/indicators'

export const indicatorHandlers = [
  http.get(api('/indicators'), async () => {
    await latency()
    return HttpResponse.json(INDICATOR_CATALOG)
  }),
]
