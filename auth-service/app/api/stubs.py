"""Pins the deliberately-stubbed contract of the frozen order-placement endpoints
(see repo-root CLAUDE.md's Polymarket API policy and auth-service/CLAUDE.md).

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
    """Deprecated/frozen — signs orders for the Polymarket CLOB. Do not implement further; see CLAUDE.md."""
    return _NOT_IMPLEMENTED


@router.post("/credentials", response_model=NotImplementedResponse)
def get_credentials() -> NotImplementedResponse:
    """Deprecated/frozen — derives L2 API key + secret from the L1 wallet private key. Do not implement further; see CLAUDE.md."""
    return _NOT_IMPLEMENTED
