"""
DTO for Notification Event to publish to RabbitMQ
Matches the NotificationEvent.java structure from ISU-Backend
"""

from typing import Optional, Dict
from pydantic import BaseModel, Field
from enum import Enum


class TargetType(str, Enum):
    """Target Type Enum matching Java Constants.TargetType"""
    BOOKING = "BOOKING"
    REPORT = "REPORT"
    ACCOUNT = "ACCOUNT"
    SERVICE_REVIEWS = "SERVICE_REVIEWS"
    CONVERSATION = "CONVERSATION"
    PAYMENT = "PAYMENT"
    SERVICE_PACKAGES = "SERVICE_PACKAGES"


class NotificationEvent(BaseModel):
    """
    Notification Event DTO
    Matches Java NotificationEvent structure for RabbitMQ message
    """
    event_id: Optional[str] = Field(None, alias="eventId", description="Unique event ID")
    fcm_token: Optional[str] = Field(None, alias="fcmToken", description="FCM token of recipient")
    notification_title: str = Field(..., alias="notificationTitle", description="Notification title")
    notification_body: str = Field(..., alias="notificationBody", description="Notification body")
    target_type: TargetType = Field(..., alias="targetType", description="Target type")
    target_id: Optional[str] = Field(None, alias="targetId", description="ID of the target")
    recipient_id: Optional[str] = Field(None, alias="recipientId", description="ID of the recipient user")
    image_url: Optional[str] = Field(None, alias="imageUrl", description="Optional image URL")
    meta_data: Optional[Dict[str, str]] = Field(None, alias="metaData", description="Optional metadata")

    class Config:
        populate_by_name = True  # Allow both snake_case and camelCase
        use_enum_values = True  # Serialize enum as string value
        json_schema_extra = {
            "example": {
                "recipientId": "123e4567-e89b-12d3-a456-426614174000",
                "notificationTitle": "AI Assistant Reply",
                "notificationBody": "Your question has been answered!",
                "targetType": "CONVERSATION",
                "targetId": "session-123",
                "fcmToken": None,
                "imageUrl": None,
                "metaData": {
                    "sessionId": "session-123",
                    "questionPreview": "What is Tarot?"
                }
            }
        }
