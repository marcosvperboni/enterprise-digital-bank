from __future__ import annotations

from fastapi.testclient import TestClient

from notification_service.models import NotificationStatus
from notification_service.service import NotificationService


def _create(client: TestClient, message: str = "hello", channel: str = "EMAIL") -> dict:
    response = client.post(
        "/api/notifications",
        json={"paymentId": "p1", "message": message, "channel": channel},
    )
    assert response.status_code == 201
    return response.json()


def test_create_notification(client: TestClient) -> None:
    body = _create(client)

    assert body["paymentId"] == "p1"
    assert body["message"] == "hello"
    assert body["channel"] == "EMAIL"
    assert body["status"] == "SENT"
    assert "id" in body and "createdAt" in body


def test_create_notification_invalid_returns_400(client: TestClient) -> None:
    response = client.post("/api/notifications", json={"paymentId": "p1"})

    assert response.status_code == 400


def test_list_notifications(client: TestClient) -> None:
    _create(client, message="one")
    _create(client, message="two")

    response = client.get("/api/notifications")

    assert response.status_code == 200
    body = response.json()
    assert len(body) == 2
    assert [n["message"] for n in body] == ["one", "two"]


def test_list_notifications_pagination(client: TestClient) -> None:
    for i in range(5):
        _create(client, message=f"msg{i}")

    response = client.get("/api/notifications", params={"offset": 2, "limit": 2})

    assert response.status_code == 200
    body = response.json()
    assert [n["message"] for n in body] == ["msg2", "msg3"]


def test_get_notification(client: TestClient) -> None:
    created = _create(client)

    response = client.get(f"/api/notifications/{created['id']}")

    assert response.status_code == 200
    assert response.json()["id"] == created["id"]


def test_get_notification_not_found(client: TestClient) -> None:
    response = client.get("/api/notifications/999")

    assert response.status_code == 404


def test_delete_notification(client: TestClient) -> None:
    created = _create(client)

    response = client.delete(f"/api/notifications/{created['id']}")
    assert response.status_code == 204

    response = client.get(f"/api/notifications/{created['id']}")
    assert response.status_code == 404


def test_delete_notification_not_found(client: TestClient) -> None:
    response = client.delete("/api/notifications/999")

    assert response.status_code == 404


def test_retry_failed_notification_succeeds(
    client: TestClient, service: NotificationService
) -> None:
    created = _create(client, message="simulate-failure please")
    assert created["status"] == "FAILED"

    response = client.put(f"/api/notifications/{created['id']}/retry")

    assert response.status_code == 200
    assert response.json()["status"] == "SENT"
    assert service.get(created["id"]).status == NotificationStatus.SENT


def test_retry_non_failed_notification_returns_409(client: TestClient) -> None:
    created = _create(client)
    assert created["status"] == "SENT"

    response = client.put(f"/api/notifications/{created['id']}/retry")

    assert response.status_code == 409


def test_retry_not_found_returns_404(client: TestClient) -> None:
    response = client.put("/api/notifications/999/retry")

    assert response.status_code == 404
