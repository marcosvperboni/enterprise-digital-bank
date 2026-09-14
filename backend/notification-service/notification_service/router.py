from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status

from notification_service.models import Notification, NotificationCreate
from notification_service.service import (
    NotFoundError,
    NotificationConflictError,
    NotificationService,
)

router = APIRouter(prefix="/api/notifications", tags=["notifications"])

_default_service = NotificationService()


def get_notification_service() -> NotificationService:
    return _default_service


@router.get("", response_model=list[Notification])
def list_notifications(
    offset: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=200),
    service: NotificationService = Depends(get_notification_service),
) -> list[Notification]:
    return service.list(offset=offset, limit=limit)


@router.get("/{notification_id}", response_model=Notification)
def get_notification(
    notification_id: int,
    service: NotificationService = Depends(get_notification_service),
) -> Notification:
    try:
        return service.get(notification_id)
    except NotFoundError as exc:
        raise HTTPException(status.HTTP_404_NOT_FOUND, str(exc)) from exc


@router.post("", response_model=Notification, status_code=status.HTTP_201_CREATED)
def create_notification(
    data: NotificationCreate,
    service: NotificationService = Depends(get_notification_service),
) -> Notification:
    return service.create(data)


@router.delete("/{notification_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_notification(
    notification_id: int,
    service: NotificationService = Depends(get_notification_service),
) -> Response:
    try:
        service.delete(notification_id)
    except NotFoundError as exc:
        raise HTTPException(status.HTTP_404_NOT_FOUND, str(exc)) from exc
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.put("/{notification_id}/retry", response_model=Notification)
def retry_notification(
    notification_id: int,
    service: NotificationService = Depends(get_notification_service),
) -> Notification:
    try:
        return service.retry(notification_id)
    except NotFoundError as exc:
        raise HTTPException(status.HTTP_404_NOT_FOUND, str(exc)) from exc
    except NotificationConflictError as exc:
        raise HTTPException(status.HTTP_409_CONFLICT, str(exc)) from exc
