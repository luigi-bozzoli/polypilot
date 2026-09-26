from __future__ import annotations

import json
from unittest.mock import MagicMock, patch

import pytest

from app.schemas.analyze import AiSignalMessage
from app.services import publisher


def _message() -> AiSignalMessage:
    return AiSignalMessage(
        market_id="m1",
        summary="s",
        articles=[],
        sentiment="NEUTRAL",
        confidence=0.0,
        reasoning="r",
        article_count=0,
        model_used="claude-haiku-4-5-20251001",
        raw_response={},
        generated_at="2025-01-01T00:00:00+00:00",
    )


def _mock_connection() -> tuple[MagicMock, MagicMock]:
    channel = MagicMock()
    connection = MagicMock()
    connection.channel.return_value = channel
    return connection, channel


def test_queue_declared_durable_with_dead_letter_args(settings):
    connection, channel = _mock_connection()

    with patch("app.services.publisher.pika.BlockingConnection", return_value=connection):
        publisher.publish_signal(settings, _message())

    channel.queue_declare.assert_called_once()
    kwargs = channel.queue_declare.call_args.kwargs
    assert kwargs["queue"] == settings.rabbitmq_queue_ai_signals
    assert kwargs["durable"] is True
    assert kwargs["arguments"]["x-dead-letter-exchange"] == "ai.signals.dlx"
    assert kwargs["arguments"]["x-dead-letter-routing-key"] == "ai.signals.dlq"


def test_publishes_persistent_json_body_matching_the_message(settings):
    connection, channel = _mock_connection()
    message = _message()

    with patch("app.services.publisher.pika.BlockingConnection", return_value=connection):
        publisher.publish_signal(settings, message)

    channel.basic_publish.assert_called_once()
    kwargs = channel.basic_publish.call_args.kwargs
    assert kwargs["routing_key"] == settings.rabbitmq_queue_ai_signals
    assert json.loads(kwargs["body"]) == message.model_dump()
    assert kwargs["properties"].delivery_mode == 2
    assert kwargs["properties"].content_type == "application/json"


def test_connection_is_closed_even_when_publish_fails(settings):
    connection, channel = _mock_connection()
    channel.basic_publish.side_effect = RuntimeError("boom")

    with patch("app.services.publisher.pika.BlockingConnection", return_value=connection):
        with pytest.raises(RuntimeError):
            publisher.publish_signal(settings, _message())

    connection.close.assert_called_once()


def test_connection_construction_failure_propagates(settings):
    # Pins the current code's actual behavior: pika.BlockingConnection(...) itself
    # raising happens before there's any connection object to close, so this is a
    # bare propagation, not a retry or a swallowed failure.
    with patch(
        "app.services.publisher.pika.BlockingConnection", side_effect=ConnectionError("no broker")
    ):
        with pytest.raises(ConnectionError):
            publisher.publish_signal(settings, _message())
