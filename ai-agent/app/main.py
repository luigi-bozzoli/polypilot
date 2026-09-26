"""PolyPilot ai-agent — application factory.

News fetch → summarize → sentiment score, published to RabbitMQ. Only ``/health``
is implemented today; the LangGraph pipeline behind ``/ai/analyze`` is Phase 4.
See ``CLAUDE.md``.
"""

from __future__ import annotations

import logging

from fastapi import FastAPI

from app.api import analyze, health
from app.config import get_settings
from app.exceptions import install_exception_handlers
from app.logging_config import configure_logging

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    settings = get_settings()
    configure_logging(settings.log_level)

    app = FastAPI(title="PolyPilot AI Agent")
    install_exception_handlers(app)

    app.include_router(health.router)
    app.include_router(analyze.router)

    logger.info(
        "ai-agent ready [rabbitmq_host=%s, anthropic_model=%s]",
        settings.rabbitmq_host,
        settings.anthropic_model,
    )
    return app


app = create_app()
