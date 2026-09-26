"""Shared fixtures. No test in this suite may reach the real network — see
pyproject.toml's --disable-socket; mock at the boundary (respx for GDELT/httpx,
unittest.mock for ChatAnthropic and pika) instead of hitting anything real.
"""

from __future__ import annotations

import pytest

from app.config import Settings

#  Settings' fields use validation_alias (e.g. anthropic_model -> AI_AGENT_MODEL) without
#  populate_by_name, and model_config has extra="ignore" — so overrides keyed by the plain
#  field name are silently dropped, not applied. Keys here must be the env-var alias names.
_DEFAULT_OVERRIDES = dict(
    ANTHROPIC_API_KEY="test-anthropic-key",
    AI_AGENT_MODEL="claude-haiku-4-5-20251001",
    RABBITMQ_HOST="rabbitmq-test",
    RABBITMQ_PORT=5672,
    RABBITMQ_USER="test-user",
    RABBITMQ_PASSWORD="test-pass",
    RABBITMQ_QUEUE_AI_SIGNALS="ai.signals",
    GDELT_BASE_URL="https://gdelt.test/api",
    NEWS_LOOKBACK_HOURS=24,
    NEWS_MAX_ARTICLES=20,
)


@pytest.fixture
def make_settings():
    """Factory for a Settings instance with test-safe defaults, real env/.env
    ignored (_env_file=None) so results don't depend on the machine running them.
    """

    def _make(**overrides) -> Settings:
        return Settings(_env_file=None, **{**_DEFAULT_OVERRIDES, **overrides})

    return _make


@pytest.fixture
def settings(make_settings) -> Settings:
    return make_settings()
