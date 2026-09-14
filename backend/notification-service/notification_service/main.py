from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from notification_service.kafka_consumer import consume_forever
from notification_service.router import get_notification_service, router

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("notification_service")


@asynccontextmanager
async def lifespan(app: FastAPI):
    service = get_notification_service()
    consumer_task = asyncio.create_task(consume_forever(service))
    yield
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass


app = FastAPI(title="notification-service", lifespan=lifespan)
app.include_router(router)


@app.exception_handler(RequestValidationError)
async def validation_error_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={"detail": exc.errors()},
    )


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
