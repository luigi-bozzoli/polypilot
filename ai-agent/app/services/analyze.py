"""Sentiment-analysis pipeline.

fetch news (GDELT) -> summarize + score (one Claude call) -> publish
(RabbitMQ ``ai.signals``). The orchestrator calls ``POST /ai/analyze``
fire-and-forget: this returns a 202-equivalent immediately and the actual
pipeline runs on a FastAPI background task, matching the repo's documented
async boundary (root CLAUDE.md: "AI sentiment is fire-and-forget over
RabbitMQ").
"""

from __future__ import annotations

import logging
from datetime import UTC, datetime

from fastapi import BackgroundTasks

from app.config import Settings
from app.schemas.analyze import AiSignalMessage, AnalysisResult, AnalyzeAccepted, AnalyzeRequest
from app.services import news_fetcher, publisher
from app.services.analyzer import analyze

logger = logging.getLogger(__name__)


class AnalyzeService:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def analyze(
        self, request: AnalyzeRequest, background_tasks: BackgroundTasks
    ) -> AnalyzeAccepted:
        background_tasks.add_task(self._run_pipeline, request)
        return AnalyzeAccepted(detail="analysis started", market_id=request.market_id)

    async def _run_pipeline(self, request: AnalyzeRequest) -> None:
        settings = self._settings
        try:
            articles = await news_fetcher.fetch_articles(
                settings, request.asset_symbol, request.asset_display_name
            )
        except Exception:
            logger.exception("Unexpected news-fetch failure for market [%s]", request.market_id)
            articles = []

        try:
            if not articles:
                result, raw_response = self._empty_result(), {}
            else:
                result, raw_response = analyze(
                    settings, request.market_question, request.asset_display_name, articles
                )
        except Exception as exc:
            logger.error(
                "Claude analysis failed for market [%s] [model=%s]: %s: %s",
                request.market_id,
                settings.anthropic_model,
                type(exc).__name__,
                exc,
                exc_info=True,
            )
            result, raw_response = self._failed_result(), {}

        message = AiSignalMessage(
            market_id=request.market_id,
            summary=result.summary,
            articles=articles,
            sentiment=result.sentiment,
            confidence=result.confidence,
            reasoning=result.reasoning,
            article_count=len(articles),
            model_used=settings.anthropic_model,
            raw_response=raw_response,
            generated_at=datetime.now(UTC).isoformat(),
        )

        try:
            publisher.publish_signal(settings, message)
        except Exception:
            # Never let a broker hiccup crash the background task — the next
            # news-sync tick will simply retry since no fresh row got written.
            logger.exception(
                "Failed to publish ai.signals message for market [%s]", request.market_id
            )

    @staticmethod
    def _empty_result() -> AnalysisResult:
        return AnalysisResult(
            summary="No news coverage found for this asset in the lookback window.",
            sentiment="NEUTRAL",
            confidence=0.0,
            reasoning="Zero articles returned by GDELT for this query.",
        )

    @staticmethod
    def _failed_result() -> AnalysisResult:
        return AnalysisResult(
            summary="Analysis failed — see reasoning.",
            sentiment="NEUTRAL",
            confidence=0.0,
            reasoning="The sentiment-analysis model call failed for this run.",
        )
