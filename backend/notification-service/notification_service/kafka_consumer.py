from __future__ import annotations

import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from notification_service import config
from notification_service.models import Notification, PaymentEvent
from notification_service.service import NotificationService

logger = logging.getLogger("notification_service.kafka")


def handle_message(raw_value: bytes, service: NotificationService) -> Notification | None:
    """Parse one Kafka message payload and hand it to the notification service.

    Kept separate from the consume loop so it's testable without a broker:
    call it directly with raw bytes and a service instance.
    """
    try:
        payload = json.loads(raw_value)
        event = PaymentEvent.model_validate(payload)
    except (json.JSONDecodeError, ValidationError):
        logger.warning("dropping malformed payment-events message: %r", raw_value)
        return None
    return service.handle_payment_event(event)


async def consume_forever(
    service: NotificationService,
    bootstrap_servers: str = config.KAFKA_BOOTSTRAP_SERVERS,
    topic: str = config.KAFKA_TOPIC,
    group_id: str = config.KAFKA_GROUP_ID,
    max_retries: int = 10,
    retry_backoff_seconds: float = 3.0,
) -> None:
    """Run the aiokafka consumer loop until cancelled.

    Retries the initial connection with backoff (Kafka in the compose stack
    is only "started", not guaranteed ready, when this service boots) before
    giving up so the FastAPI app can still serve REST traffic if Kafka is
    truly unreachable (e.g. in tests or local dev without a broker running).
    """
    consumer = AIOKafkaConsumer(
        topic,
        bootstrap_servers=bootstrap_servers,
        group_id=group_id,
        enable_auto_commit=True,
    )
    for attempt in range(1, max_retries + 1):
        try:
            await consumer.start()
            break
        except asyncio.CancelledError:
            raise
        except Exception:
            if attempt == max_retries:
                logger.warning(
                    "could not connect to Kafka at %s after %d attempts, notification consumer disabled",
                    bootstrap_servers,
                    max_retries,
                    exc_info=True,
                )
                return
            logger.warning(
                "Kafka not ready at %s (attempt %d/%d), retrying in %.0fs",
                bootstrap_servers,
                attempt,
                max_retries,
                retry_backoff_seconds,
            )
            await asyncio.sleep(retry_backoff_seconds)

    try:
        async for message in consumer:
            try:
                handle_message(message.value, service)
            except Exception:
                logger.exception("failed to process payment-events message")
    except asyncio.CancelledError:
        raise
    finally:
        await consumer.stop()
