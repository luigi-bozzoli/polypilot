from __future__ import annotations

import httpx
import pytest
import respx

from app.services.news_fetcher import fetch_articles

GDELT_URL = "https://gdelt.test/api"


@pytest.mark.asyncio
@respx.mock
async def test_success_parses_articles(settings):
    respx.get(GDELT_URL).mock(
        return_value=httpx.Response(
            200,
            json={
                "articles": [
                    {
                        "title": "BTC rallies on ETF inflows",
                        "url": "https://news.example/1",
                        "domain": "news.example",
                        "seendate": "20250101T120000Z",
                    },
                    {  # blank title -> dropped
                        "title": "   ",
                        "url": "https://news.example/2",
                        "domain": "news.example",
                        "seendate": "20250101T120000Z",
                    },
                    {  # missing url -> dropped
                        "title": "No URL here",
                        "url": "",
                        "domain": "news.example",
                        "seendate": "20250101T120000Z",
                    },
                ]
            },
        )
    )

    articles = await fetch_articles(settings, "BTC", "Bitcoin")

    assert len(articles) == 1
    assert articles[0].title == "BTC rallies on ETF inflows"
    assert articles[0].source == "news.example"
    assert articles[0].url == "https://news.example/1"


@pytest.mark.asyncio
@respx.mock
async def test_malformed_body_returns_empty_list(settings):
    respx.get(GDELT_URL).mock(return_value=httpx.Response(200, content=b"not json"))

    articles = await fetch_articles(settings, "BTC", "Bitcoin")

    assert articles == []


@pytest.mark.asyncio
@respx.mock
async def test_empty_articles_field_returns_empty_list(settings):
    respx.get(GDELT_URL).mock(return_value=httpx.Response(200, json={"articles": []}))

    articles = await fetch_articles(settings, "BTC", "Bitcoin")

    assert articles == []


@pytest.mark.asyncio
@respx.mock
async def test_http_5xx_retries_three_times_with_linear_backoff_then_empty(settings, monkeypatch):
    sleep_calls: list[float] = []

    async def fake_sleep(seconds: float) -> None:
        sleep_calls.append(seconds)

    monkeypatch.setattr("app.services.news_fetcher.asyncio.sleep", fake_sleep)
    route = respx.get(GDELT_URL).mock(return_value=httpx.Response(500))

    articles = await fetch_articles(settings, "BTC", "Bitcoin")

    assert articles == []
    assert route.call_count == 3
    assert sleep_calls == [1.0, 2.0]  # linear backoff, only before attempts 2 and 3


@pytest.mark.asyncio
@respx.mock
async def test_timeout_retries_three_times_then_empty(settings, monkeypatch):
    monkeypatch.setattr("app.services.news_fetcher.asyncio.sleep", _noop_sleep)
    route = respx.get(GDELT_URL).mock(side_effect=httpx.TimeoutException("timed out"))

    articles = await fetch_articles(settings, "BTC", "Bitcoin")

    assert articles == []
    assert route.call_count == 3


@pytest.mark.asyncio
@respx.mock
async def test_fail_then_succeed_returns_articles(settings, monkeypatch):
    monkeypatch.setattr("app.services.news_fetcher.asyncio.sleep", _noop_sleep)
    route = respx.get(GDELT_URL).mock(
        side_effect=[
            httpx.Response(500),
            httpx.Response(
                200,
                json={
                    "articles": [
                        {
                            "title": "Recovered on retry",
                            "url": "https://news.example/1",
                            "domain": "news.example",
                            "seendate": "20250101T120000Z",
                        }
                    ]
                },
            ),
        ]
    )

    articles = await fetch_articles(settings, "BTC", "Bitcoin")

    assert len(articles) == 1
    assert articles[0].title == "Recovered on retry"
    assert route.call_count == 2


async def _noop_sleep(seconds: float) -> None:
    return None
