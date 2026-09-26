"""FastAPI-level tests for POST /ai/analyze and GET /health."""

from __future__ import annotations

from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_analyze_returns_ack_and_schedules_the_background_pipeline():
    with patch("app.services.analyze.AnalyzeService._run_pipeline") as mock_run:
        response = client.post(
            "/ai/analyze",
            json={
                "market_id": "m1",
                "asset_symbol": "BTC",
                "asset_display_name": "Bitcoin",
                "market_question": "Will BTC be up?",
            },
        )

    assert response.status_code == 200
    assert response.json() == {"detail": "analysis started", "market_id": "m1"}
    mock_run.assert_called_once()


def test_analyze_invalid_body_returns_422():
    response = client.post("/ai/analyze", json={"market_id": "m1"})

    assert response.status_code == 422


def test_health_returns_200_and_makes_no_network_calls():
    # pytest-socket's --disable-socket (pyproject.toml) would fail this test outright
    # if the handler tried to reach anything real.
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["service"] == "ai-agent"
    assert body["status"] == "ok"
