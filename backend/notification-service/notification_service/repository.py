from __future__ import annotations

import sqlite3
import threading
from datetime import datetime

from notification_service.models import (
    Notification,
    NotificationChannel,
    NotificationStatus,
)

_SCHEMA = """
CREATE TABLE IF NOT EXISTS notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    paymentId TEXT NOT NULL,
    message TEXT NOT NULL,
    channel TEXT NOT NULL,
    status TEXT NOT NULL,
    createdAt TEXT NOT NULL
)
"""


class NotificationRepository:
    """SQLite-backed notification store.

    ponytail: single connection + global lock, fine for this portfolio demo's
    request volume; swap for a pool if this ever sees real concurrency.
    """

    def __init__(self, db_path: str = ":memory:") -> None:
        self._conn = sqlite3.connect(db_path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        self._lock = threading.Lock()
        with self._lock:
            self._conn.execute(_SCHEMA)
            self._conn.commit()

    def add(
        self,
        payment_id: str,
        message: str,
        status: NotificationStatus,
        channel: NotificationChannel = NotificationChannel.EMAIL,
    ) -> Notification:
        created_at = datetime.now().astimezone()
        with self._lock:
            cursor = self._conn.execute(
                "INSERT INTO notifications (paymentId, message, channel, status, createdAt) "
                "VALUES (?, ?, ?, ?, ?)",
                (payment_id, message, channel.value, status.value, created_at.isoformat()),
            )
            self._conn.commit()
            row_id = cursor.lastrowid
        return self.get(row_id)  # type: ignore[return-value]

    def get(self, notification_id: int) -> Notification | None:
        with self._lock:
            row = self._conn.execute(
                "SELECT * FROM notifications WHERE id = ?", (notification_id,)
            ).fetchone()
        return _row_to_model(row) if row else None

    def list(self, offset: int = 0, limit: int = 50) -> list[Notification]:
        with self._lock:
            rows = self._conn.execute(
                "SELECT * FROM notifications ORDER BY id LIMIT ? OFFSET ?",
                (limit, offset),
            ).fetchall()
        return [_row_to_model(row) for row in rows]

    def delete(self, notification_id: int) -> bool:
        with self._lock:
            cursor = self._conn.execute(
                "DELETE FROM notifications WHERE id = ?", (notification_id,)
            )
            self._conn.commit()
        return cursor.rowcount > 0

    def update_status(
        self, notification_id: int, status: NotificationStatus
    ) -> Notification | None:
        with self._lock:
            self._conn.execute(
                "UPDATE notifications SET status = ? WHERE id = ?",
                (status.value, notification_id),
            )
            self._conn.commit()
        return self.get(notification_id)


def _row_to_model(row: sqlite3.Row) -> Notification:
    return Notification(
        id=row["id"],
        paymentId=row["paymentId"],
        message=row["message"],
        channel=NotificationChannel(row["channel"]),
        status=NotificationStatus(row["status"]),
        createdAt=datetime.fromisoformat(row["createdAt"]),
    )
