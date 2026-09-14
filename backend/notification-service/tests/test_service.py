from __future__ import annotations

import pytest

from notification_service.models import NotificationStatus, PaymentEvent, PaymentStatus
from notification_service.service import NotificationService


@pytest.fixture()
def service() -> NotificationService:
    return NotificationService()


def test_handle_payment_event_approved(service: NotificationService) -> None:
    event = PaymentEvent(paymentId="p1", transactionId="t1", status=PaymentStatus.APPROVED)

    notification = service.handle_payment_event(event)

    assert notification.paymentId == "p1"
    assert notification.status == NotificationStatus.SENT
    assert "approved" in notification.message.lower()
    assert "p1" in notification.message


def test_handle_payment_event_rejected(service: NotificationService) -> None:
    event = PaymentEvent(paymentId="p2", transactionId="t2", status=PaymentStatus.REJECTED)

    notification = service.handle_payment_event(event)

    assert notification.paymentId == "p2"
    assert notification.status == NotificationStatus.SENT
    assert "rejected" in notification.message.lower()
