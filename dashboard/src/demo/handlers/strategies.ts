import { http, HttpResponse } from 'msw'
import type { CreateStrategyRequest, UpdateStrategyRequest } from '../../features/strategies/types'
import { api, latency } from '../util'
import { CONDITION_FIELDS_CATALOG } from '../fixtures/indicators'
import { buildStrategyOrders } from '../fixtures/strategies'
import { buildStrategyDecisions } from '../fixtures/audit'
import * as db from '../db'

const NOT_FOUND = () => HttpResponse.json({ detail: 'Strategy not found' }, { status: 404 })

export const strategiesHandlers = [
  http.get(api('/strategies/condition-fields'), async () => {
    await latency()
    return HttpResponse.json(CONDITION_FIELDS_CATALOG)
  }),

  http.get(api('/strategies'), async () => {
    await latency()
    return HttpResponse.json(db.listStrategies())
  }),

  http.get(api('/strategies/:id'), async ({ params }) => {
    await latency()
    const strategy = db.getStrategy(String(params.id))
    if (!strategy) return NOT_FOUND()
    return HttpResponse.json(strategy)
  }),

  http.post(api('/strategies'), async ({ request }) => {
    await latency()
    const body = (await request.json()) as CreateStrategyRequest
    return HttpResponse.json(db.createStrategy(body), { status: 201 })
  }),

  http.put(api('/strategies/:id'), async ({ params, request }) => {
    await latency()
    const body = (await request.json()) as UpdateStrategyRequest
    const updated = db.updateStrategy(String(params.id), body)
    if (!updated) return NOT_FOUND()
    return HttpResponse.json(updated)
  }),

  http.delete(api('/strategies/:id'), async ({ params }) => {
    await latency()
    const deleted = db.deleteStrategy(String(params.id))
    if (!deleted) return NOT_FOUND()
    return new HttpResponse(null, { status: 204 })
  }),

  http.put(api('/strategies/:id/enabled'), async ({ params, request }) => {
    await latency()
    const body = (await request.json()) as { enabled: boolean }
    const updated = db.setStrategyEnabled(String(params.id), body.enabled)
    if (!updated) return NOT_FOUND()
    return HttpResponse.json(updated)
  }),

  http.get(api('/strategies/:id/orders'), async ({ params, request }) => {
    await latency()
    const id = String(params.id)
    const limit = Number(new URL(request.url).searchParams.get('limit') ?? '20')
    if (!db.getStrategy(id)) return HttpResponse.json([])
    return HttpResponse.json(db.isSeedStrategy(id) ? buildStrategyOrders(id, limit) : [])
  }),

  http.get(api('/strategies/:id/decisions'), async ({ params, request }) => {
    await latency()
    const id = String(params.id)
    const limit = Number(new URL(request.url).searchParams.get('limit') ?? '20')
    if (!db.getStrategy(id)) return HttpResponse.json([])
    return HttpResponse.json(db.isSeedStrategy(id) ? buildStrategyDecisions(id, limit) : [])
  }),
]
