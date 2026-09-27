"""PolyPilot auth-service — application factory.

The **only** service allowed to touch private keys and raw wallet signatures.
It does cryptographic verification only and never enforces SIWE / application
policy — nonce, domain, expiry, chain id, user lookup and every authorization
decision live in the orchestrator..
"""

from __future__ import annotations

import logging

from fastapi import FastAPI

from app.api import clob, health, siwe, stubs
from app.config import get_settings
from app.exceptions import install_exception_handlers
from app.logging_config import configure_logging

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    settings = get_settings()
    configure_logging(settings.log_level)

    app = FastAPI(title="PolyPilot Auth Service")
    install_exception_handlers(app)

    app.include_router(health.router)
    app.include_router(siwe.router)
    app.include_router(clob.router)
    app.include_router(stubs.router)

    logger.info("auth-service ready [version=%s]", settings.service_version)
    return app


app = create_app()
