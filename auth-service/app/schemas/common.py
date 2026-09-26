"""Response models shared across routers."""

from __future__ import annotations

from pydantic import BaseModel


class NotImplementedResponse(BaseModel):
    """Body returned by endpoints that are declared but not built yet."""

    detail: str
