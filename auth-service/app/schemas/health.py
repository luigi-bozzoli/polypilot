"""Health-check response models.

The ``metrics`` block feeds the dashboard's health page; the orchestrator fans
this response out and injects the probe ``latencyMs`` itself.
"""

from __future__ import annotations

from pydantic import BaseModel


class ServiceMetrics(BaseModel):
    uptime: str
    detail: str


class HealthResponse(BaseModel):
    service: str
    status: str
    private_key_configured: bool
    version: str
    metrics: ServiceMetrics
