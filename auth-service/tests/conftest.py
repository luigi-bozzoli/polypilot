"""Shared fixtures. No test in this suite may reach the real network — see
pyproject.toml's --disable-socket.
"""

from __future__ import annotations

import pytest

from app.config import Settings

#  Settings' fields use validation_alias without populate_by_name, and model_config
#  has extra="ignore" — overrides keyed by the plain field name are silently dropped.
#  Keys here must be the env-var alias names.
_DEFAULT_OVERRIDES = dict(
    POLYMARKET_CLOB_URL="https://clob.test",
    POLYMARKET_PRIVATE_KEY="",
    SERVICE_VERSION="0.0.0-test",
    LOG_LEVEL="INFO",
)


@pytest.fixture
def make_settings():
    def _make(**overrides) -> Settings:
        return Settings(_env_file=None, **{**_DEFAULT_OVERRIDES, **overrides})

    return _make


@pytest.fixture
def settings(make_settings) -> Settings:
    return make_settings()
