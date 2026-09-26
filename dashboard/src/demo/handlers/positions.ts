import { http, HttpResponse } from 'msw'
import { api, latency } from '../util'
import { POSITIONS } from '../fixtures/positions'

export const positionsHandlers = [
  http.get(api('/positions'), async ({ request }) => {
    await latency()
    const limit = Number(new URL(request.url).searchParams.get('limit') ?? '20')
    return HttpResponse.json(POSITIONS.slice(0, limit))
  }),
]
