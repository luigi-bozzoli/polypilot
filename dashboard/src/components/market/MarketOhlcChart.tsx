import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  createChart,
  type CandlestickData,
  type IChartApi,
  type ISeriesApi,
  type MouseEventParams,
  type Time,
  type UTCTimestamp,
} from 'lightweight-charts'
import { SectionLabel } from './primitives'
import type { OhlcCandle } from '../../features/market/types'
import { useMarketOhlc, useTimeframes } from '../../features/market/useMarketOhlc'

/**
 * "OHLC · Binance · last N candles" — the market's underlying asset price, distinct
 * from the probability-history chart above it (that one plots `price_snapshots`,
 * this one plots `ohlc_candles` via the series' linked ticker). Same TradingView
 * Lightweight Charts instance-lifecycle pattern as `MarketPriceHistoryChart`
 * (chart created once in a ref, data pushed on refetch so pan/zoom survives), a
 * `CandlestickSeries` instead of an `AreaSeries`, and a timeframe selector sourced
 * from `GET /api/reference/timeframes` instead of a hardcoded window list.
 * Fed by `GET /api/market/:id/ohlc` via `useMarketOhlc`. Contract:
 * `contracts/market-ohlc.md`.
 *
 * Lightweight Charts is Apache-2.0 and requires a visible TradingView
 * attribution — the link below the chart card satisfies that (its own
 * on-canvas logo is disabled via `layout.attributionLogo`).
 */

const CHART_HEIGHT = 220
const CANDLE_LIMIT = 300
const DEFAULT_TIMEFRAME = '1h'

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

/** Crypto prices span orders of magnitude — more precision below $1. */
function formatPrice(value: number): string {
  return value >= 1
    ? `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    : `$${value.toFixed(6)}`
}

/** Contract candles → strictly-ascending, de-duped candlestick data. */
function toCandlestickData(candles: OhlcCandle[]): CandlestickData<UTCTimestamp>[] {
  const data: CandlestickData<UTCTimestamp>[] = []
  let lastTime = Number.NEGATIVE_INFINITY
  for (const candle of candles) {
    const time = Math.floor(Date.parse(candle.openTime) / 1000)
    if (!Number.isFinite(time) || time <= lastTime) continue
    lastTime = time
    data.push({
      time: time as UTCTimestamp,
      open: candle.open,
      high: candle.high,
      low: candle.low,
      close: candle.close,
    })
  }
  return data
}

function OhlcPlot({ candles }: { candles: OhlcCandle[] }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const tooltipRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null)

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
        mode: CrosshairMode.Normal,
        vertLine: { color: 'rgba(255,255,255,0.12)', labelBackgroundColor: '#1c2028' },
        horzLine: { color: 'rgba(255,255,255,0.12)', labelBackgroundColor: '#1c2028' },
      },
      localization: {
        locale: 'en-GB',
        priceFormatter: (value: number) => formatPrice(value),
        timeFormatter: (time: Time) => `${utcHm(time)} UTC`,
      },
    })

    const series = chart.addSeries(CandlestickSeries, {
      upColor: '#00d4a8',
      downColor: '#f05252',
      borderUpColor: '#00d4a8',
      borderDownColor: '#f05252',
      wickUpColor: '#00d4a8',
      wickDownColor: '#f05252',
      priceLineVisible: false,
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
      if (!row || !('open' in row)) {
        tip.style.display = 'none'
        return
      }
      tip.style.display = 'block'
      tip.textContent =
        `${utcHm(param.time)} UTC · O ${formatPrice(row.open)} H ${formatPrice(row.high)} ` +
        `L ${formatPrice(row.low)} C ${formatPrice(row.close)}`
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
    series.setData(toCandlestickData(candles))
    chartRef.current?.timeScale().fitContent()
  }, [candles])

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

export function MarketOhlcChart({ marketId }: { marketId: string }) {
  const [timeframe, setTimeframe] = useState(DEFAULT_TIMEFRAME)
  const { data: timeframes } = useTimeframes()
  const { data, isLoading, isError, error, refetch } = useMarketOhlc(marketId, timeframe, CANDLE_LIMIT)

  // Once the enabled-timeframe list loads, fall back to its first entry if our
  // default isn't among them (e.g. "1h" disabled) rather than requesting a
  // timeframe the server would reject.
  const options = useMemo(() => timeframes ?? [], [timeframes])
  useEffect(() => {
    if (options.length > 0 && !options.some((t) => t.code === timeframe)) {
      setTimeframe(options[0].code)
    }
  }, [options, timeframe])

  const candles = data?.candles ?? []
  const noLinkedAsset = data != null && data.symbol == null
  const showChart = !isLoading && !isError && !noLinkedAsset && candles.length > 0

  return (
    <div>
      <div className="mb-[11px] flex items-center justify-between gap-3">
        <SectionLabel>
          OHLC · Binance{data?.symbol ? ` · ${data.symbol}` : ''} · {timeframe}
        </SectionLabel>
        {options.length > 0 && (
          <div className="flex flex-wrap justify-end gap-1">
            {options.map((t) => (
              <button
                key={t.code}
                type="button"
                onClick={() => setTimeframe(t.code)}
                aria-pressed={t.code === timeframe}
                title={t.label}
                className={`rounded border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider transition-colors ${
                  t.code === timeframe
                    ? 'border-accent-border bg-accent-dim text-accent'
                    : 'border-border bg-bg2 text-text-muted hover:text-text-secondary'
                }`}
              >
                {t.code}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="rounded-[9px] border border-border bg-bg2 p-3">
        {isLoading && <ChartMessage>loading candles…</ChartMessage>}

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

        {!isLoading && !isError && noLinkedAsset && (
          <ChartMessage>this series has no linked asset</ChartMessage>
        )}

        {!isLoading && !isError && !noLinkedAsset && candles.length === 0 && (
          <ChartMessage>no candle data yet</ChartMessage>
        )}

        {showChart && <OhlcPlot key={timeframe} candles={candles} />}
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
