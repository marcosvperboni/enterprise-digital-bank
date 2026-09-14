from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from notification_service.main import app
from notification_service.router import get_notification_service
from notification_service.service import NotificationService


@pytest.fixture()
def service() -> NotificationService:
    return NotificationService()


@pytest.fixture()
def client(service: NotificationService) -> TestClient:
    # Not using `with TestClient(...)` on purpose: that triggers the app's
    # lifespan, which starts the Kafka consumer and tries to reach a broker.
    # REST tests don't need it and it would just add connection-timeout delay.
    app.dependency_overrides[get_notification_service] = lambda: service
    test_client = TestClient(app)
    yield test_client
    app.dependency_overrides.clear()
