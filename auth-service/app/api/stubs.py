"""Pins the deliberately-stubbed contract of the frozen order-placement endpoints.

These must keep returning 200 "not implemented yet" — do not build them out
further.
"""

from __future__ import annotations

from fastapi import APIRouter

from app.schemas.common import NotImplementedResponse

router = APIRouter(prefix="/auth", tags=["stubs"])

_NOT_IMPLEMENTED = NotImplementedResponse(detail="not implemented yet")


@router.post("/sign-order", response_model=NotImplementedResponse)
def sign_order() -> NotImplementedResponse:
    """Deprecated/frozen — signs Polymarket CLOB orders."""
    return _NOT_IMPLEMENTED


@router.post("/credentials", response_model=NotImplementedResponse)
def get_credentials() -> NotImplementedResponse:
    """Deprecated/frozen — derives L2 creds from the L1 key."""
    return _NOT_IMPLEMENTED
