"""Composition root: FastAPI dependency providers.

Route handlers depend on these; nothing is instantiated inline in a handler.
Providers are cached so services (and the HTTP client) are process singletons —
this also keeps ``HealthService`` uptime anchored to process start.
"""

from __future__ import annotations

from functools import lru_cache

import httpx

from app.config import get_settings
from app.services.clob import ClobAuthService
from app.services.health import HealthService
from app.services.siwe import SiweService

# Outbound calls to Polymarket are synchronous on purpose: the caller cannot
# proceed without the derived credentials.
_HTTP_TIMEOUT_SECONDS = 10.0


@lru_cache
def get_http_client() -> httpx.Client:
    return httpx.Client(timeout=_HTTP_TIMEOUT_SECONDS, follow_redirects=True)


@lru_cache
def get_health_service() -> HealthService:
    return HealthService(get_settings())


@lru_cache
def get_siwe_service() -> SiweService:
    return SiweService()


@lru_cache
def get_clob_service() -> ClobAuthService:
    return ClobAuthService(get_http_client(), get_settings())
