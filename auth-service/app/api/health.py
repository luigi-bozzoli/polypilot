"""``GET /health`` — liveness/readiness snapshot for the orchestrator fan-out."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends

from app.dependencies import get_health_service
from app.schemas.health import HealthResponse
from app.services.health import HealthService

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
def get_health(
    service: Annotated[HealthService, Depends(get_health_service)],
) -> HealthResponse:
    return service.snapshot()
