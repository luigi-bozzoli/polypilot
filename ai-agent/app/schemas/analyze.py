"""Request/response models for the sentiment-analysis trigger and pipeline."""

from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel


class AnalyzeRequest(BaseModel):
    """Body for ``POST /ai/analyze``.

    ``ai-agent`` has no DB access — the orchestrator must pass market context
    it already has, since ai-agent can't look it up itself.
    """

    market_id: str
    asset_symbol: str  # e.g. "BTC" — series.asset.symbol
    asset_display_name: str  # e.g. "Bitcoin" — series.asset.displayName
    market_question: str  # e.g. "Will BTC be up by 5PM ET?" — market.question


class AnalyzeAccepted(BaseModel):
    """Acknowledgement body for ``POST /ai/analyze``."""

    detail: str
    market_id: str


class Article(BaseModel):
    """One news item — maps 1:1 onto ``news_summaries.articles`` JSONB shape."""

    title: str
    url: str
    source: str
    published_at: str  # ISO-8601 UTC


class AnalysisResult(BaseModel):
    """Single structured-output Claude call producing both summary and sentiment
    (schema comment: "produced in the same LangGraph run but serve different
    consumers", 001_schema.sql:384-386)."""

    summary: str
    sentiment: Literal["BULLISH", "BEARISH", "NEUTRAL"]
    confidence: float  # 0..1
    reasoning: str


class AiSignalMessage(BaseModel):
    """Published to the ``ai.signals`` RabbitMQ queue. The orchestrator's
    ``AiSignalListener`` writes one ``news_summaries`` row and one
    ``sentiment_scores`` row from a single message."""

    market_id: str
    summary: str
    articles: list[Article]
    sentiment: Literal["BULLISH", "BEARISH", "NEUTRAL"]
    confidence: float
    reasoning: str
    article_count: int
    model_used: str
    raw_response: dict[str, Any]
    generated_at: str  # ISO-8601 UTC
