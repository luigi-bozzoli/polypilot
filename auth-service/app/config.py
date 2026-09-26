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
    """auth-service settings. Field values come from the aliased env vars."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    clob_base_url: str = Field(
        default="https://clob.polymarket.com",
        validation_alias="POLYMARKET_CLOB_URL",
    )
    polymarket_private_key: str = Field(default="", validation_alias="POLYMARKET_PRIVATE_KEY")
    service_version: str = Field(default="0.1.0", validation_alias="SERVICE_VERSION")
    log_level: str = Field(default="INFO", validation_alias="LOG_LEVEL")


@lru_cache
def get_settings() -> Settings:
    """Return the process-wide settings singleton."""
    return Settings()
