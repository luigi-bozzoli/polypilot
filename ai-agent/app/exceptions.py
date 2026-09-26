"""Domain exceptions and their single, centralized FastAPI handler.

Services raise a :class:`ServiceError` subclass; the handler renders it as
``{"detail": ...}`` with the carried status code — the exact shape FastAPI's
own ``HTTPException`` produces, so responses are unchanged.

``ai-agent`` has no error paths yet; this mirrors ``auth-service`` so the two
services stay structurally identical as the LangGraph pipeline lands.
"""

from __future__ import annotations

import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)


class ServiceError(Exception):
    """Base class for expected, client-facing failures."""

    status_code: int = 500
    detail: str = "Internal server error"

    def __init__(self, detail: str | None = None, *, status_code: int | None = None) -> None:
        if detail is not None:
            self.detail = detail
        if status_code is not None:
            self.status_code = status_code
        super().__init__(self.detail)


def install_exception_handlers(app: FastAPI) -> None:
    """Register the :class:`ServiceError` handler on ``app``."""

    @app.exception_handler(ServiceError)
    async def _handle_service_error(request: Request, exc: ServiceError) -> JSONResponse:
        if exc.status_code >= 500:
            logger.error("service error [%s] %s", exc.status_code, exc.detail)
        else:
            logger.info("request rejected [%s] %s", exc.status_code, exc.detail)
        return JSONResponse(status_code=exc.status_code, content={"detail": exc.detail})
