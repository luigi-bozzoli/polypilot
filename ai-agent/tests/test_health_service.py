"""Regression coverage for the anthropic_configured placeholder-detection fix.

The old code used ``value.strip("replace_me")``, which strips a *character set*
(any of r,e,p,l,a,c,_,m from either end), not the substring "replace_me" — so a
value made up only of those characters (like whitespace-free "replace" itself,
or anything that happens to start/end with them) could be wrongly stripped to
empty, and whitespace-only values were never caught at all since none of those
characters are spaces.
"""

from __future__ import annotations

import pytest

from app.services.health import HealthService


@pytest.mark.parametrize(
    ("value", "expected"),
    [
        ("", False),
        ("   ", False),  # whitespace-only: the old strip(chars) form never stripped this
        ("replace_me", False),  # the actual placeholder
        ("replace", True),  # NOT the placeholder, but old code stripped it to "" anyway
        ("sk-ant-api03-real-looking-key", True),
    ],
)
def test_anthropic_configured_detection(make_settings, value, expected):
    settings = make_settings(ANTHROPIC_API_KEY=value)

    snapshot = HealthService(settings).snapshot()

    assert snapshot.anthropic_configured is expected
