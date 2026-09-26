"""Publish a completed analysis to RabbitMQ (``ai.signals``).

One message per market per run. A new ``pika.BlockingConnection`` is opened
per publish — simplest correct approach at this traffic (one message per
tracked market per ``news-sync`` tick, not a high-throughput producer); a
pooled/long-lived connection would be over-engineering here and this repo has
no existing async-AMQP pattern to extend.
"""

from __future__ import annotations

import json
import logging

import pika

from app.config import Settings
from app.schemas.analyze import AiSignalMessage

logger = logging.getLogger(__name__)

# Must match the orchestrator's declaration of this same queue
# (orchestrator/.../ai/config/RabbitConfig.java) — RabbitMQ rejects a
# re-declare whose arguments don't match what's already on the queue.
_DEAD_LETTER_EXCHANGE = "ai.signals.dlx"
_DEAD_LETTER_ROUTING_KEY = "ai.signals.dlq"


def publish_signal(settings: Settings, message: AiSignalMessage) -> None:
    credentials = pika.PlainCredentials(settings.rabbitmq_user, settings.rabbitmq_password)
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=settings.rabbitmq_host, port=settings.rabbitmq_port, credentials=credentials
        )
    )
    try:
        channel = connection.channel()
        channel.queue_declare(
            queue=settings.rabbitmq_queue_ai_signals,
            durable=True,
            arguments={
                "x-dead-letter-exchange": _DEAD_LETTER_EXCHANGE,
                "x-dead-letter-routing-key": _DEAD_LETTER_ROUTING_KEY,
            },
        )
        channel.basic_publish(
            exchange="",
            routing_key=settings.rabbitmq_queue_ai_signals,
            body=json.dumps(message.model_dump()),
            properties=pika.BasicProperties(
                content_type="application/json",
                delivery_mode=2,  # persistent — survives a RabbitMQ restart
            ),
        )
        logger.info("Published ai.signals message for market [%s]", message.market_id)
    finally:
        connection.close()
