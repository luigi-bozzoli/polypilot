"""Response models shared across routers."""

from __future__ import annotations

from pydantic import BaseModel


class NotImplementedResponse(BaseModel):
    """Body returned by the deprecated/frozen ``/auth/sign-order`` and
    ``/auth/credentials`` stubs (see CLAUDE.md) — these are permanently
    stubbed, not pending implementation."""

    detail: str
