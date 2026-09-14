from __future__ import annotations

import os

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_TOPIC = os.getenv("KAFKA_TOPIC", "payment-events")
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "notification-service")
PORT = int(os.getenv("PORT", "8085"))
