"""``POST /ai/analyze`` — trigger the sentiment pipeline for one market.

Body carries market context (``AnalyzeRequest``) since ai-agent has no DB of
its own to look it up.
"""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, BackgroundTasks, Depends

from app.dependencies import get_analyze_service
from app.schemas.analyze import AnalyzeAccepted, AnalyzeRequest
from app.services.analyze import AnalyzeService

router = APIRouter(prefix="/ai", tags=["analyze"])


@router.post("/analyze", response_model=AnalyzeAccepted)
def analyze(
    request: AnalyzeRequest,
    background_tasks: BackgroundTasks,
    service: Annotated[AnalyzeService, Depends(get_analyze_service)],
) -> AnalyzeAccepted:
    return service.analyze(request, background_tasks)
