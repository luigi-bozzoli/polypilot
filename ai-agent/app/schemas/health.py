"""Health-check response models.

The ``metrics`` block feeds the dashboard's health page; the orchestrator fans
this response out and injects the probe ``latencyMs`` itself. Shape is fixed by
``contracts/health-service-metrics.md``.
"""

from __future__ import annotations

from pydantic import BaseModel


class ServiceMetrics(BaseModel):
    uptime: str
    detail: str


class HealthResponse(BaseModel):
    service: str
    status: str
    anthropic_configured: bool
    rabbitmq_host: str
    metrics: ServiceMetrics
