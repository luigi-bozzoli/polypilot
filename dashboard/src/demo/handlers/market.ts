import { http, HttpResponse } from 'msw'
import { api, latency } from '../util'
import {
  buildLatestNews,
  buildLatestSentiment,
  buildMarketOrders,
  buildOhlc,
  buildPriceHistory,
  findMarket,
  findSeriesDetail,
  SERIES_LIST,
  TIMEFRAMES,
} from '../fixtures/market'
import { buildMarketDecisions } from '../fixtures/audit'

export const marketHandlers = [
  http.get(api('/market/series'), async () => {
    await latency()
    return HttpResponse.json(SERIES_LIST)
  }),

  http.get(api('/market/series/:id'), async ({ params }) => {
    await latency()
    const detail = findSeriesDetail(String(params.id))
    if (!detail) return HttpResponse.json({ detail: 'Series not found' }, { status: 404 })
    return HttpResponse.json(detail)
  }),

  http.get(api('/reference/timeframes'), async () => {
    await latency()
    return HttpResponse.json(TIMEFRAMES)
  }),

  http.get(api('/market/:marketId/price-history'), async ({ params, request }) => {
    await latency()
    const marketId = String(params.marketId)
    if (!findMarket(marketId)) return HttpResponse.json({ detail: 'Market not found' }, { status: 404 })
    const window = new URL(request.url).searchParams.get('window') ?? '24h'
    return HttpResponse.json(buildPriceHistory(marketId, window))
  }),

  http.get(api('/market/:marketId/ohlc'), async ({ params, request }) => {
    await latency()
    const marketId = String(params.marketId)
    const found = findMarket(marketId)
    if (!found) return HttpResponse.json({ detail: 'Market not found' }, { status: 404 })
    const url = new URL(request.url)
    const timeframe = url.searchParams.get('timeframe') ?? '1h'
    const limit = Number(url.searchParams.get('limit') ?? '200')
    return HttpResponse.json(buildOhlc(marketId, found.series.ticker as 'BTC' | 'ETH', timeframe, limit))
  }),

  http.get(api('/market/:marketId/orders'), async ({ params }) => {
    await latency()
    const marketId = String(params.marketId)
    if (!findMarket(marketId)) return HttpResponse.json({ detail: 'Market not found' }, { status: 404 })
    return HttpResponse.json(buildMarketOrders(marketId))
  }),

  http.get(api('/market/:marketId/news/latest'), async ({ params }) => {
    await latency()
    const marketId = String(params.marketId)
    const news = buildLatestNews(marketId)
    if (!news) return new HttpResponse(null, { status: 204 })
    return HttpResponse.json(news)
  }),

  http.get(api('/market/:marketId/sentiment/latest'), async ({ params }) => {
    await latency()
    const marketId = String(params.marketId)
    const sentiment = buildLatestSentiment(marketId)
    if (!sentiment) return new HttpResponse(null, { status: 204 })
    return HttpResponse.json(sentiment)
  }),

  http.get(api('/market/:marketId/decisions'), async ({ params, request }) => {
    await latency()
    const marketId = String(params.marketId)
    if (!findMarket(marketId)) return HttpResponse.json({ detail: 'Market not found' }, { status: 404 })
    const limit = Number(new URL(request.url).searchParams.get('limit') ?? '20')
    return HttpResponse.json(buildMarketDecisions(marketId, limit))
  }),
]
