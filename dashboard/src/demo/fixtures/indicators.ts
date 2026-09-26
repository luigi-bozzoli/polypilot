import type {
  CompareOperator,
  ConditionFieldsCatalog,
  IndicatorApiView,
  IndicatorCatalogEntry,
  IndicatorCatalogView,
  IndicatorOutputView,
  IndicatorParameterView,
  MarketFieldCatalogEntry,
} from '../../features/strategies/types'

const ALL_OPERATORS: CompareOperator[] = ['EQ', 'NEQ', 'GT', 'GTE', 'LT', 'LTE']

type IndicatorDef = {
  key: string
  name: string
  abbreviation: string
  category: IndicatorApiView['category']
  description: string
  parameters: IndicatorParameterView[]
  outputs: IndicatorOutputView[]
}

const periodParam = (key: string, name: string, def: string): IndicatorParameterView => ({
  key,
  name,
  description: `${name}, in candles`,
  dataType: 'INTEGER',
  required: true,
  default: def,
  constraints: { min: 2, max: 500 },
  universal: false,
})

const INDICATOR_DEFS: IndicatorDef[] = [
  {
    key: 'sma',
    name: 'Simple Moving Average',
    abbreviation: 'SMA',
    category: 'TREND',
    description: 'Unweighted mean of the last N closes.',
    parameters: [periodParam('period', 'Period', '20')],
    outputs: [{ key: 'value', name: 'SMA', valueScale: 'PRICE', default: true }],
  },
  {
    key: 'ema',
    name: 'Exponential Moving Average',
    abbreviation: 'EMA',
    category: 'TREND',
    description: 'Moving average weighted toward recent closes.',
    parameters: [periodParam('period', 'Period', '12')],
    outputs: [{ key: 'value', name: 'EMA', valueScale: 'PRICE', default: true }],
  },
  {
    key: 'rsi',
    name: 'Relative Strength Index',
    abbreviation: 'RSI',
    category: 'MOMENTUM',
    description: 'Momentum oscillator measuring speed and change of price moves.',
    parameters: [periodParam('period', 'Period', '14')],
    outputs: [{ key: 'value', name: 'RSI', valueScale: 'OSCILLATOR_0_100', default: true }],
  },
  {
    key: 'macd',
    name: 'Moving Average Convergence Divergence',
    abbreviation: 'MACD',
    category: 'MOMENTUM',
    description: 'Difference between a fast and slow EMA, with a signal line.',
    parameters: [
      periodParam('fastPeriod', 'Fast period', '12'),
      periodParam('slowPeriod', 'Slow period', '26'),
      periodParam('signalPeriod', 'Signal period', '9'),
    ],
    outputs: [
      { key: 'macd', name: 'MACD line', valueScale: 'UNBOUNDED', default: true },
      { key: 'signal', name: 'Signal line', valueScale: 'UNBOUNDED', default: false },
      { key: 'histogram', name: 'Histogram', valueScale: 'UNBOUNDED', default: false },
    ],
  },
  {
    key: 'atr',
    name: 'Average True Range',
    abbreviation: 'ATR',
    category: 'VOLATILITY',
    description: 'Average of true range over N candles — a volatility measure.',
    parameters: [periodParam('period', 'Period', '14')],
    outputs: [{ key: 'value', name: 'ATR', valueScale: 'PRICE', default: true }],
  },
  {
    key: 'volume_ma',
    name: 'Volume Moving Average',
    abbreviation: 'Vol MA',
    category: 'VOLUME',
    description: 'Moving average of traded volume over N candles.',
    parameters: [periodParam('period', 'Period', '20')],
    outputs: [{ key: 'value', name: 'Volume MA', valueScale: 'VOLUME', default: true }],
  },
]

/** GET /api/indicators — the real orchestrator endpoint, not scoped to strategy-builder use. */
export const INDICATOR_CATALOG: IndicatorCatalogView = {
  indicators: INDICATOR_DEFS.map(
    (d): IndicatorApiView => ({
      key: d.key,
      name: d.name,
      abbreviation: d.abbreviation,
      category: d.category,
      description: d.description,
      parameters: d.parameters,
      outputs: d.outputs,
    }),
  ),
}

const MARKET_FIELDS: MarketFieldCatalogEntry[] = [
  { field: 'up_price', name: 'Up price', dataType: 'NUMBER', valueScale: 'OSCILLATOR_0_1', operators: ALL_OPERATORS },
  { field: 'down_price', name: 'Down price', dataType: 'NUMBER', valueScale: 'OSCILLATOR_0_1', operators: ALL_OPERATORS },
  { field: 'volume_24h', name: 'Volume (24h)', dataType: 'NUMBER', valueScale: 'VOLUME', operators: ALL_OPERATORS },
  { field: 'liquidity', name: 'Liquidity', dataType: 'NUMBER', valueScale: 'VOLUME', operators: ALL_OPERATORS },
  { field: 'sentiment', name: 'Sentiment', dataType: 'ENUM', allowedValues: ['BULLISH', 'NEUTRAL', 'BEARISH'], operators: ['EQ', 'NEQ'] },
  { field: 'sentiment.confidence', name: 'Sentiment confidence', dataType: 'NUMBER', valueScale: 'OSCILLATOR_0_1', operators: ALL_OPERATORS },
]

/** GET /strategies/condition-fields — feeds the strategy condition builder. */
export const CONDITION_FIELDS_CATALOG: ConditionFieldsCatalog = {
  marketFields: MARKET_FIELDS,
  indicators: INDICATOR_DEFS.map(
    (d): IndicatorCatalogEntry => ({
      indicatorKey: d.key,
      name: d.name,
      abbreviation: d.abbreviation,
      category: d.category,
      parameters: d.parameters,
      outputs: d.outputs,
      operators: ALL_OPERATORS,
    }),
  ),
}
