"""Typed application configuration.

All runtime configuration is read once, here, from environment variables (or a
local ``.env`` for out-of-Docker runs). Nothing else in the service touches
``os.environ`` directly.
"""

from __future__ import annotations

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """ai-agent settings. Field values come from the aliased env vars."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    anthropic_api_key: str = Field(default="", validation_alias="ANTHROPIC_API_KEY")
    # claude-3-5-sonnet-20241022 (the prior default) was retired 2025-10-22 — see
    # https://platform.claude.com/docs/en/about-claude/model-deprecations. A retired
    # model returns an API error, which _run_pipeline silently degrades to a neutral,
    # zero-confidence result, so a stale default here fails invisibly, not loudly.
    anthropic_model: str = Field(
        default="claude-haiku-4-5-20251001", validation_alias="AI_AGENT_MODEL"
    )
    rabbitmq_host: str = Field(default="not set", validation_alias="RABBITMQ_HOST")
    rabbitmq_port: int = Field(default=5672, validation_alias="RABBITMQ_PORT")
    rabbitmq_user: str = Field(default="", validation_alias="RABBITMQ_USER")
    rabbitmq_password: str = Field(default="", validation_alias="RABBITMQ_PASSWORD")
    rabbitmq_queue_ai_signals: str = Field(
        default="ai.signals", validation_alias="RABBITMQ_QUEUE_AI_SIGNALS"
    )
    log_level: str = Field(default="INFO", validation_alias="LOG_LEVEL")

    # News sourcing (docs/news_sourcing.MD): GDELT DOC 2.0 — free, unrestricted
    # commercial use, no API key. Revisit that doc before swapping sources.
    gdelt_base_url: str = Field(
        default="https://api.gdeltproject.org/api/v2/doc/doc",
        validation_alias="GDELT_BASE_URL",
    )
    news_lookback_hours: int = Field(default=24, validation_alias="NEWS_LOOKBACK_HOURS")
    news_max_articles: int = Field(default=20, validation_alias="NEWS_MAX_ARTICLES")


@lru_cache
def get_settings() -> Settings:
    """Return the process-wide settings singleton."""
    return Settings()
