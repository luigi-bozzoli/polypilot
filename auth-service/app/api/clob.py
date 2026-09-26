"""``POST /auth/verify-and-derive`` — relay a ClobAuth message, return L2 creds."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends

from app.dependencies import get_clob_service
from app.schemas.clob import L2CredentialsResponse, VerifyAndDeriveRequest
from app.services.clob import ClobAuthService

router = APIRouter(prefix="/auth", tags=["clob"])


@router.post("/verify-and-derive", response_model=L2CredentialsResponse)
def verify_and_derive(
    request: VerifyAndDeriveRequest,
    service: Annotated[ClobAuthService, Depends(get_clob_service)],
) -> L2CredentialsResponse:
    return service.verify_and_derive(request)
