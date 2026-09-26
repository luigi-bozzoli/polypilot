from __future__ import annotations

from app.config import Settings


def test_default_model_is_claude_haiku(monkeypatch):
    monkeypatch.delenv("AI_AGENT_MODEL", raising=False)
    settings = Settings(_env_file=None)
    assert settings.anthropic_model == "claude-haiku-4-5-20251001"


def test_ai_agent_model_env_var_overrides_default(monkeypatch):
    monkeypatch.setenv("AI_AGENT_MODEL", "claude-opus-4-1-20250805")
    settings = Settings(_env_file=None)
    assert settings.anthropic_model == "claude-opus-4-1-20250805"


def test_no_default_contains_a_retired_model_string(monkeypatch):
    monkeypatch.delenv("AI_AGENT_MODEL", raising=False)
    settings = Settings(_env_file=None)
    assert "claude-3" not in settings.anthropic_model
