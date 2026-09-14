from __future__ import annotations

import logging

from notification_service.models import (
    Notification,
    NotificationChannel,
    NotificationCreate,
    NotificationStatus,
    PaymentEvent,
    PaymentStatus,
)
from notification_service.repository import NotificationRepository

logger = logging.getLogger("notification_service")

_MESSAGES = {
    PaymentStatus.APPROVED: "Your payment {paymentId} was approved.",
    PaymentStatus.REJECTED: "Your payment {paymentId} was rejected.",
}


class NotificationConflictError(Exception):
    """Raised when retrying a notification that isn't currently FAILED."""


class NotFoundError(Exception):
    """Raised when a notification id doesn't exist."""


class NotificationService:
    def __init__(self, repo: NotificationRepository | None = None) -> None:
        self.repo = repo or NotificationRepository()

    def handle_payment_event(self, event: PaymentEvent) -> Notification:
        message = _MESSAGES[event.status].format(paymentId=event.paymentId)
        return self._send(event.paymentId, message, NotificationChannel.EMAIL)

    def create(self, data: NotificationCreate) -> Notification:
        return self._send(data.paymentId, data.message, data.channel)

    def list(self, offset: int = 0, limit: int = 50) -> list[Notification]:
        return self.repo.list(offset=offset, limit=limit)

    def get(self, notification_id: int) -> Notification:
        notification = self.repo.get(notification_id)
        if notification is None:
            raise NotFoundError(f"notification {notification_id} not found")
        return notification

    def delete(self, notification_id: int) -> None:
        if not self.repo.delete(notification_id):
            raise NotFoundError(f"notification {notification_id} not found")

    def retry(self, notification_id: int) -> Notification:
        notification = self.get(notification_id)
        if notification.status is not NotificationStatus.FAILED:
            raise NotificationConflictError(
                f"notification {notification_id} is not FAILED"
            )
        # ponytail: retries are simulated as always succeeding (no real
        # provider to retry against). If flaky-retry behavior matters later,
        # route this through _simulate_delivery like the initial send.
        status = NotificationStatus.SENT
        logger.info(
            "retrying notification id=%s paymentId=%s channel=%s -> %s",
            notification.id,
            notification.paymentId,
            notification.channel,
            status,
        )
        return self.repo.update_status(notification_id, status)  # type: ignore[return-value]

    def _send(
        self, payment_id: str, message: str, channel: NotificationChannel
    ) -> Notification:
        status = self._simulate_delivery(message)
        logger.info(
            "sending notification paymentId=%s channel=%s status=%s message=%r",
            payment_id,
            channel,
            status,
            message,
        )
        return self.repo.add(payment_id, message, status, channel)

    @staticmethod
    def _simulate_delivery(message: str) -> NotificationStatus:
        # ponytail: no real email/SMS provider is wired up (out of scope for this
        # demo). Delivery is simulated and deterministically fails when the
        # message contains "simulate-failure", so tests/manual QA can exercise
        # the FAILED -> retry path without a fake provider abstraction.
        if "simulate-failure" in message.lower():
            return NotificationStatus.FAILED
        return NotificationStatus.SENT
