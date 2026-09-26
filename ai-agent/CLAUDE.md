# CLAUDE.md — ai-agent

Python 3.12 / FastAPI. News fetch → summarize → sentiment score, published to
RabbitMQ (`ai.signals`). See the repo-root `CLAUDE.md` for the monorepo picture
and the shared Python-service conventions.

`/ai/analyze` runs the news → summarize/score → publish pipeline (Phase 4, see
`docs/news_summary.MD`). News source is GDELT DOC 2.0 only (free, unrestricted
commercial use, no API key — see `docs/news_sourcing.MD`); summarization and
sentiment scoring are one structured-output Anthropic Claude call per run
(`app/services/analyzer.py`), published to RabbitMQ (`ai.signals`) via `pika`.
The pipeline does not use LangGraph's graph machinery today — it's a plain
async function (`AnalyzeService._run_pipeline`); `langgraph` stays in
`requirements.txt` for when a genuinely branching flow needs it.

## Layout

Mirrors `auth-service` exactly (`logging_config.py` and the `exceptions.py`
`ServiceError` base are byte-identical copies — keep them in sync):

```
main.py                 # uvicorn entrypoint shim → `from app.main import app`
app/
  main.py               # create_app(): settings → logging → routers → exception handlers
  config.py             # Settings (pydantic-settings) + cached get_settings()
  logging_config.py     # configure_logging()
  exceptions.py         # ServiceError base + install_exception_handlers()
  dependencies.py       # composition root: cached Depends providers
  api/      health.py  analyze.py
  schemas/  health.py  analyze.py
  services/ health.py  analyze.py  news_fetcher.py  analyzer.py  publisher.py
```

Route handlers are two lines: resolve the service via `Depends`, call it, return
its typed result. All configuration comes from `app/config.py` — no `os.environ`
reads anywhere else.

## Endpoints

- `GET /health` — `app/services/health.py`. Reports `anthropic_configured`,
  `rabbitmq_host`, and the shared `metrics` block
  (`contracts/health-service-metrics.md`); the orchestrator fans this out and
  injects `latencyMs`.
- `POST /ai/analyze` — `app/services/analyze.py`. Body is `AnalyzeRequest`
  (`market_id`, `asset_symbol`, `asset_display_name`, `market_question` — no
  DB here, so the caller must pass context). Returns **200**
  `{"detail": "analysis started", "market_id": "…"}` immediately; the actual
  pipeline (GDELT fetch → Claude summarize+score → publish to `ai.signals`)
  runs on a `BackgroundTasks` task after the response is sent, matching the
  repo's fire-and-forget async boundary. A failure at any stage (GDELT
  unreachable, malformed response, Claude error) still publishes a message —
  degraded (`NEUTRAL`, low/zero confidence, reasoning noting the failure) but
  never silent — so a consumer can always see a run happened.

## Dependencies

- `httpx` for outbound HTTP (GDELT fetch, async).
- `langchain-anthropic` (`ChatAnthropic.with_structured_output`) for the
  summarize+sentiment call — one call per run, not two.
- `pika` for the `ai.signals` publish — a fresh `BlockingConnection` per
  publish (low volume, no pooling needed).
- `pydantic-settings` backs `app/config.py`. `OPENAI_API_KEY` is injected by
  docker-compose but not read — OpenAI is not used anywhere in this service.
