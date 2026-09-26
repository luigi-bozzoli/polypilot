"""Builds the ``GET /health`` payload for auth-service."""

from __future__ import annotations

import time

from app.config import Settings
from app.schemas.health import HealthResponse, ServiceMetrics


def _format_uptime(seconds: float) -> str:
    """Short, display-ready uptime — see ``contracts/health-service-metrics.md``."""
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
        key = self._settings.polymarket_private_key.strip()
        private_key_configured = bool(key) and key != "replace_me"
        return HealthResponse(
            service="auth-service",
            status="ok",
            private_key_configured=private_key_configured,
            version=self._settings.service_version,
            metrics=ServiceMetrics(
                uptime=_format_uptime(time.monotonic() - self._started_at),
                detail="web3 ok",
            ),
        )
