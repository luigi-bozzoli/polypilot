"""GDELT DOC 2.0 news search — the sole configured news source.

Free, unrestricted for commercial/redistribution use, no API key, updated
about every 15 minutes. Returns article
*metadata* only (title/url/domain/seendate) — no full article text.
"""

from __future__ import annotations

import asyncio
import logging
from datetime import UTC, datetime

import httpx

from app.config import Settings
from app.schemas.analyze import Article

logger = logging.getLogger(__name__)

_MAX_ATTEMPTS = 3
_BACKOFF_SECONDS = 1.0


def _parse_seendate(raw: str) -> str:
    """GDELT's ``seendate`` is ``YYYYMMDDHHMMSS`` UTC-ish; normalize to ISO-8601.

    Falls back to "now" if the format ever changes underneath us — a
    best-effort display timestamp, not something worth failing the pipeline
    over.
    """
    try:
        return datetime.strptime(raw, "%Y%m%dT%H%M%SZ").replace(tzinfo=UTC).isoformat()
    except ValueError:
        return datetime.now(UTC).isoformat()


async def fetch_articles(
    settings: Settings, asset_symbol: str, asset_display_name: str
) -> list[Article]:
    """Query GDELT for recent coverage of an asset.

    Zero articles found is a valid, non-error outcome (e.g. an obscure or
    newly-listed asset may genuinely have no coverage in the lookback window)
    — callers must still proceed and publish a "no coverage" result rather
    than skip the run.

    GDELT unreachable / malformed response is also swallowed here (logged,
    returns ``[]``) — a transient outage on a free, no-SLA API must not crash
    a scheduled run; the caller treats it identically to "no articles".
    """
    params = {
        "query": f'"{asset_display_name}" OR "{asset_symbol}"',
        "mode": "artlist",
        "maxrecords": settings.news_max_articles,
        "timespan": f"{settings.news_lookback_hours}h",
        "format": "json",
    }

    last_error: Exception | None = None
    for attempt in range(1, _MAX_ATTEMPTS + 1):
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(settings.gdelt_base_url, params=params)
                response.raise_for_status()
                payload = response.json()
            break
        except (httpx.HTTPError, ValueError) as exc:
            last_error = exc
            logger.warning(
                "GDELT fetch attempt %s/%s failed for %s: %s",
                attempt,
                _MAX_ATTEMPTS,
                asset_symbol,
                exc,
            )
            if attempt < _MAX_ATTEMPTS:
                await asyncio.sleep(_BACKOFF_SECONDS * attempt)
    else:
        logger.error("GDELT fetch exhausted retries for %s: %s", asset_symbol, last_error)
        return []

    raw_articles = payload.get("articles", []) if isinstance(payload, dict) else []
    articles: list[Article] = []
    for item in raw_articles:
        title = (item.get("title") or "").strip()
        url = item.get("url") or ""
        if not title or not url:
            continue
        articles.append(
            Article(
                title=title,
                url=url,
                source=item.get("domain") or "unknown",
                published_at=_parse_seendate(item.get("seendate") or ""),
            )
        )
    return articles
