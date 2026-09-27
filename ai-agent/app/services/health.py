"""Builds the ``GET /health`` payload for ai-agent."""

from __future__ import annotations

import time

from app.config import Settings
from app.schemas.health import HealthResponse, ServiceMetrics


def _format_uptime(seconds: float) -> str:
    total = int(max(0, seconds))
    days, rem = divmod(total, 86_400)
    hours, rem = divmod(rem, 3_600)
    minutes, secs = divmod(rem, 60)
    if days:
        return f"{days}d {hours:02d}h"
    if hours:
        return f"{hours}h {minutes:02d}m"
    if minutes:
        return f"{minutes}m {secs:02d}s"
    return f"{secs}s"


class HealthService:
    """One instance per process, so ``uptime`` is measured from process start."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._started_at = time.monotonic()

    def snapshot(self) -> HealthResponse:
        key = self._settings.anthropic_api_key.strip()
        anthropic_configured = bool(key) and key != "replace_me"
        return HealthResponse(
            service="ai-agent",
            status="ok",
            anthropic_configured=anthropic_configured,
            rabbitmq_host=self._settings.rabbitmq_host,
            metrics=ServiceMetrics(
                uptime=_format_uptime(time.monotonic() - self._started_at),
                detail="anthropic ok" if anthropic_configured else "no api key",
            ),
        )
