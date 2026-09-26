"""Regression coverage for the private_key_configured placeholder-detection fix.

See ai-agent/tests/test_health_service.py for the full explanation of the bug:
the old code used ``value.strip("replace_me")``, which strips a *character set*
from each end, not the substring "replace_me".
"""

from __future__ import annotations

import pytest

from app.services.health import HealthService


@pytest.mark.parametrize(
    ("value", "expected"),
    [
        ("", False),
        ("   ", False),
        ("replace_me", False),
        ("replace", True),
        ("0xabc123realkeymaterial", True),
    ],
)
def test_private_key_configured_detection(make_settings, value, expected):
    settings = make_settings(POLYMARKET_PRIVATE_KEY=value)

    snapshot = HealthService(settings).snapshot()

    assert snapshot.private_key_configured is expected
