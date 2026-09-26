from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_returns_200_and_makes_no_network_calls():
    # pytest-socket's --disable-socket (pyproject.toml) would fail this test outright
    # if the handler tried to reach anything real.
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["service"] == "auth-service"
    assert body["status"] == "ok"
