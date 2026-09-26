import { http, HttpResponse } from 'msw'
import { api, latency } from '../util'
import { buildAuditLogPage } from '../fixtures/audit'

export const auditHandlers = [
  http.get(api('/audit-logs'), async ({ request }) => {
    await latency()
    const url = new URL(request.url)
    const size = Number(url.searchParams.get('size') ?? '20')
    const page = Number(url.searchParams.get('page') ?? '0')
    return HttpResponse.json(buildAuditLogPage(size, page))
  }),
]
