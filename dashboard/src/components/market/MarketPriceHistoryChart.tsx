import { useEffect, useRef, useState, type ReactNode } from 'react'
import {
  AreaSeries,
  ColorType,
  CrosshairMode,
  createChart,
  type AreaData,
  type IChartApi,
  type ISeriesApi,
  type MouseEventParams,
  type Time,
  type UTCTimestamp,
  type WhitespaceData,
} from 'lightweight-charts'
import { SectionLabel } from './primitives'
import type { MarketPricePoint } from '../../features/market/types'
import {
  PRICE_HISTORY_WINDOWS,
  useMarketPriceHistory,
  type PriceHistoryWindow,
} from '../../features/market/useMarketPriceHistory'

/**
 * "Probability history · price_snapshots · last 12h" (mock `13-market-detail.html`).
 *
 * Renders the market's UP-outcome probability (points[].upPrice × 100, in ¢) as
 * a TradingView Lightweight Charts area series, fed by
 * `GET /api/market/:id/price-history` via `useMarketPriceHistory`. A segmented
 * control switches the look-back window; `null` upPrice breaks the line (gap),
 * per `contracts/market-price-history.md`.
 *
 * Lightweight Charts is Apache-2.0 and requires a visible TradingView
 * attribution — the link below the chart card satisfies that (its own
 * on-canvas logo is disabled via `layout.attributionLogo`).
 */

const CHART_HEIGHT = 180

/** A `Time` from our timestamp-based data is always UNIX seconds (UTC). */
function utcHm(time: Time): string {
  const seconds = typeof time === 'number' ? time : Number(time)
  return new Date(seconds * 1000).toLocaleTimeString('en-GB', {
    timeZone: 'UTC',
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** Contract points → strictly-ascending, de-duped area/whitespace data. */
function toSeriesData(points: MarketPricePoint[]): (AreaData<UTCTimestamp> | WhitespaceData<UTCTimestamp>)[] {
  const data: (AreaData<UTCTimestamp> | WhitespaceData<UTCTimestamp>)[] = []
  let lastTime = Number.NEGATIVE_INFINITY
  for (const point of points) {
    const time = Math.floor(Date.parse(point.at) / 1000)
    if (!Number.isFinite(time) || time <= lastTime) continue
    lastTime = time
    data.push(
      point.upPrice == null
        ? { time: time as UTCTimestamp }
        : { time: time as UTCTimestamp, value: point.upPrice * 100 },
    )
  }
  return data
}

function PriceHistoryPlot({ points }: { points: MarketPricePoint[] }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const tooltipRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Area'> | null>(null)

  // Create the chart once; data is pushed by the effect below so a refetch
  // doesn't tear down the canvas (keeps pan/zoom, avoids a flash every 15s).
  useEffect(() => {
    const el = containerRef.current
    if (!el) return

    const chart = createChart(el, {
      autoSize: true,
      layout: {
        background: { type: ColorType.Solid, color: '#15181d' },
        textColor: '#7a8394',
        fontFamily: '"IBM Plex Mono", monospace',
        fontSize: 10,
        attributionLogo: false,
      },
      grid: {
        vertLines: { color: '#232830' },
        horzLines: { color: '#232830' },
      },
      rightPriceScale: { borderColor: 'rgba(255,255,255,0.07)' },
      timeScale: {
        borderColor: 'rgba(255,255,255,0.07)',
        timeVisible: true,
        secondsVisible: false,
        tickMarkFormatter: (time: Time) => utcHm(time),
      },
      crosshair: {
        mode: CrosshairMode.Magnet,
        vertLine: { color: 'rgba(255,255,255,0.12)', labelBackgroundColor: '#1c2028' },
        horzLine: { color: 'rgba(255,255,255,0.12)', labelBackgroundColor: '#1c2028' },
      },
      localization: {
        locale: 'en-GB',
        priceFormatter: (value: number) => `${value.toFixed(1)}¢`,
        timeFormatter: (time: Time) => `${utcHm(time)} UTC`,
      },
    })

    const series = chart.addSeries(AreaSeries, {
      lineColor: '#00d4a8',
      lineWidth: 2,
      topColor: 'rgba(0,212,168,0.28)',
      bottomColor: 'rgba(0,212,168,0)',
      priceLineVisible: false,
      crosshairMarkerBorderColor: '#00d4a8',
      crosshairMarkerBackgroundColor: '#00d4a8',
    })

    const onCrosshairMove = (param: MouseEventParams) => {
      const tip = tooltipRef.current
      if (!tip) return
      const activeSeries = seriesRef.current
      if (
        !activeSeries ||
        param.time == null ||
        !param.point ||
        param.point.x < 0 ||
        param.point.y < 0
      ) {
        tip.style.display = 'none'
        return
      }
      const row = param.seriesData.get(activeSeries)
      const value = row && 'value' in row ? (row.value as number) : undefined
      if (value == null) {
        tip.style.display = 'none'
        return
      }
      tip.style.display = 'block'
      tip.textContent = `${utcHm(param.time)} UTC · ${value.toFixed(1)}¢`
      const width = tip.offsetWidth
      const clamped = Math.min(Math.max(param.point.x - width / 2, 4), el.clientWidth - width - 4)
      tip.style.left = `${clamped}px`
    }
    chart.subscribeCrosshairMove(onCrosshairMove)

    chartRef.current = chart
    seriesRef.current = series

    return () => {
      chart.unsubscribeCrosshairMove(onCrosshairMove)
      chart.remove()
      chartRef.current = null
      seriesRef.current = null
    }
  }, [])

  useEffect(() => {
    const series = seriesRef.current
    if (!series) return
    series.setData(toSeriesData(points))
    chartRef.current?.timeScale().fitContent()
  }, [points])

  return (
    <div className="relative">
      <div ref={containerRef} style={{ height: CHART_HEIGHT }} className="w-full" />
      <div
        ref={tooltipRef}
        style={{ display: 'none' }}
        className="pointer-events-none absolute top-1 z-10 rounded border border-border-mid bg-bg3 px-2 py-1 font-mono text-[10px] text-text-primary"
      />
    </div>
  )
}

function ChartMessage({ children }: { children: ReactNode }) {
  return (
    <div
      style={{ height: CHART_HEIGHT }}
      className="flex flex-col items-center justify-center gap-1 text-center font-mono text-[11px] text-text-muted"
    >
      {children}
    </div>
  )
}

export function MarketPriceHistoryChart({ marketId }: { marketId: string }) {
  const [window, setWindow] = useState<PriceHistoryWindow>('12h')
  const { data, isLoading, isError, error, refetch } = useMarketPriceHistory(marketId, window)

  const points = data?.points ?? []
  const showChart = !isLoading && !isError && points.length > 0

  return (
    <div>
      <div className="mb-[11px] flex items-center justify-between gap-3">
        <SectionLabel>Probability history · price_snapshots · last {window}</SectionLabel>
        <div className="flex gap-1">
          {PRICE_HISTORY_WINDOWS.map((w) => (
            <button
              key={w}
              type="button"
              onClick={() => setWindow(w)}
              aria-pressed={w === window}
              className={`rounded border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider transition-colors ${
                w === window
                  ? 'border-accent-border bg-accent-dim text-accent'
                  : 'border-border bg-bg2 text-text-muted hover:text-text-secondary'
              }`}
            >
              {w}
            </button>
          ))}
        </div>
      </div>

      <div className="rounded-[9px] border border-border bg-bg2 p-3">
        {isLoading && <ChartMessage>loading price history…</ChartMessage>}

        {isError && (
          <ChartMessage>
            <span className="text-red">{(error as Error).message}</span>
            <button
              type="button"
              onClick={() => void refetch()}
              className="underline hover:text-text-secondary"
            >
              retry
            </button>
          </ChartMessage>
        )}

        {!isLoading && !isError && points.length === 0 && (
          <ChartMessage>no price history yet</ChartMessage>
        )}

        {showChart && <PriceHistoryPlot key={window} points={points} />}
      </div>

      <a
        href="https://www.tradingview.com/lightweight-charts/"
        target="_blank"
        rel="noreferrer"
        className="mt-2 block font-mono text-[10px] text-text-muted hover:text-text-secondary"
      >
        Charts by TradingView — Lightweight Charts™
      </a>
    </div>
  )
}
