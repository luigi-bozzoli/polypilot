"""Pins the deliberately-stubbed contract of the frozen order-placement endpoints
(see repo-root CLAUDE.md's Polymarket API policy and auth-service/CLAUDE.md) —
they must keep returning 200 "not implemented yet", not be built out further.
"""

from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_sign_order_stub_returns_not_implemented():
    response = client.post("/auth/sign-order")

    assert response.status_code == 200
    assert response.json() == {"detail": "not implemented yet"}


def test_credentials_stub_returns_not_implemented():
    response = client.post("/auth/credentials")

    assert response.status_code == 200
    assert response.json() == {"detail": "not implemented yet"}
