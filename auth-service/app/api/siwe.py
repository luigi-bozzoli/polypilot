"""``POST /auth/siwe/verify`` — recover the address behind a SIWE signature."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends

from app.dependencies import get_siwe_service
from app.schemas.siwe import SiweVerifyRequest, SiweVerifyResponse
from app.services.siwe import SiweService

router = APIRouter(prefix="/auth", tags=["siwe"])


@router.post("/siwe/verify", response_model=SiweVerifyResponse)
def verify_siwe(
    request: SiweVerifyRequest,
    service: Annotated[SiweService, Depends(get_siwe_service)],
) -> SiweVerifyResponse:
    """A malformed or unrecoverable signature yields HTTP 400."""
    return service.recover_address(request)
