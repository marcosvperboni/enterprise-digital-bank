from __future__ import annotations

import json

from notification_service.kafka_consumer import handle_message
from notification_service.models import NotificationStatus
from notification_service.service import NotificationService


def test_handle_message_valid_event() -> None:
    service = NotificationService()
    raw = json.dumps(
        {"paymentId": "p1", "transactionId": "t1", "status": "APPROVED"}
    ).encode()

    notification = handle_message(raw, service)

    assert notification is not None
    assert notification.status == NotificationStatus.SENT
    assert service.get(notification.id).paymentId == "p1"


def test_handle_message_malformed_json_is_dropped() -> None:
    service = NotificationService()

    notification = handle_message(b"not json", service)

    assert notification is None
    assert service.list() == []


def test_handle_message_invalid_schema_is_dropped() -> None:
    service = NotificationService()
    raw = json.dumps({"paymentId": "p1"}).encode()

    notification = handle_message(raw, service)

    assert notification is None
