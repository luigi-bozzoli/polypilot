"""Composition root: FastAPI dependency providers.

Route handlers depend on these; nothing is instantiated inline in a handler.
Providers are cached so services are process singletons — this also keeps
``HealthService`` uptime anchored to process start.
"""

from __future__ import annotations

from functools import lru_cache

from app.config import get_settings
from app.services.analyze import AnalyzeService
from app.services.health import HealthService


@lru_cache
def get_health_service() -> HealthService:
    return HealthService(get_settings())


@lru_cache
def get_analyze_service() -> AnalyzeService:
    return AnalyzeService(get_settings())
