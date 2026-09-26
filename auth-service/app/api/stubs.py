"""Phase 2 endpoints that are declared but not implemented yet.

Kept so the public surface (paths, 200 + ``{"detail": "not implemented yet"}``)
is stable for callers until the real implementations land.
"""

from __future__ import annotations

from fastapi import APIRouter

from app.schemas.common import NotImplementedResponse

router = APIRouter(prefix="/auth", tags=["stubs"])

_NOT_IMPLEMENTED = NotImplementedResponse(detail="not implemented yet")


@router.post("/sign-order", response_model=NotImplementedResponse)
def sign_order() -> NotImplementedResponse:
    """Phase 2: return an EIP-712 signed order payload for the Polymarket CLOB."""
    return _NOT_IMPLEMENTED


@router.post("/credentials", response_model=NotImplementedResponse)
def get_credentials() -> NotImplementedResponse:
    """Phase 2: derive L2 API key + secret from the L1 wallet private key."""
    return _NOT_IMPLEMENTED
