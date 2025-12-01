from mongoengine import Document, StringField, BooleanField, DateTimeField
from datetime import datetime, timezone

class ai_sessions(Document):
    user_id = StringField(required=True)
    created_at = DateTimeField(default=lambda: datetime.now(timezone.utc))
    last_message = StringField()
    last_message_at = DateTimeField(default=lambda: datetime.now(timezone.utc))

class ai_messages(Document):
    session_id = StringField(required=True)
    sent_by_user = BooleanField(required=True)
    text_content = StringField()
    analysis_type = StringField()
    image_url = StringField()
    created_at = DateTimeField(default=lambda: datetime.now(timezone.utc))
    updated_at = DateTimeField(default=lambda: datetime.now(timezone.utc))