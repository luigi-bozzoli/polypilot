"""End-to-end AnalyzeService._run_pipeline with GDELT, Claude and RabbitMQ all
mocked at their module boundaries — the only place the full
fetch -> analyze -> publish chain is exercised together."""

from __future__ import annotations

from unittest.mock import patch

import pytest

from app.schemas.analyze import AnalysisResult, AnalyzeRequest, Article
from app.services.analyze import AnalyzeService


def _request() -> AnalyzeRequest:
    return AnalyzeRequest(
        market_id="m1",
        asset_symbol="BTC",
        asset_display_name="Bitcoin",
        market_question="Will BTC be up by 5PM ET?",
    )


@pytest.mark.asyncio
async def test_full_pipeline_with_articles_publishes_expected_message(settings):
    articles = [
        Article(
            title="BTC rallies",
            url="https://news.example/1",
            source="news.example",
            published_at="2025-01-01T00:00:00+00:00",
        )
    ]
    result = AnalysisResult(
        summary="Bullish coverage",
        sentiment="BULLISH",
        confidence=0.75,
        reasoning="Strong positive coverage",
    )

    service = AnalyzeService(settings)
    with (
        patch("app.services.analyze.news_fetcher.fetch_articles", return_value=articles),
        patch("app.services.analyze.analyze", return_value=(result, {"raw": "response"})),
        patch("app.services.analyze.publisher.publish_signal") as mock_publish,
    ):
        await service._run_pipeline(_request())

    mock_publish.assert_called_once()
    message = mock_publish.call_args.args[1]
    assert message.market_id == "m1"
    assert message.summary == "Bullish coverage"
    assert message.sentiment == "BULLISH"
    assert message.confidence == 0.75
    assert message.article_count == 1
    assert message.articles == articles
    assert message.model_used == settings.anthropic_model
    assert message.raw_response == {"raw": "response"}


@pytest.mark.asyncio
async def test_full_pipeline_with_no_articles_still_publishes_neutral_message(settings):
    service = AnalyzeService(settings)
    with (
        patch("app.services.analyze.news_fetcher.fetch_articles", return_value=[]),
        patch("app.services.analyze.analyze") as mock_analyze,
        patch("app.services.analyze.publisher.publish_signal") as mock_publish,
    ):
        await service._run_pipeline(_request())

    mock_analyze.assert_not_called()
    mock_publish.assert_called_once()
    message = mock_publish.call_args.args[1]
    assert message.sentiment == "NEUTRAL"
    assert message.confidence == 0.0
    assert message.article_count == 0
