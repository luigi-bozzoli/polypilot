"""Health-check response models."""

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
