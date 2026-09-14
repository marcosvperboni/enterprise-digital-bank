from __future__ import annotations

from datetime import datetime, timezone
from enum import StrEnum
from typing import Annotated

from pydantic import BaseModel, Field


class PaymentStatus(StrEnum):
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"


class PaymentEvent(BaseModel):
    paymentId: str
    transactionId: str
    status: PaymentStatus


class NotificationChannel(StrEnum):
    EMAIL = "EMAIL"
    SMS = "SMS"


class NotificationStatus(StrEnum):
    SENT = "SENT"
    FAILED = "FAILED"


class NotificationCreate(BaseModel):
    paymentId: str
    message: Annotated[str, Field(min_length=1)]
    channel: NotificationChannel = NotificationChannel.EMAIL


class Notification(BaseModel):
    id: int
    paymentId: str
    message: str
    channel: NotificationChannel = NotificationChannel.EMAIL
    status: NotificationStatus
    createdAt: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
