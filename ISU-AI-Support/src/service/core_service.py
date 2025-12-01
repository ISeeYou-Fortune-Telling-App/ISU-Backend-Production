from pymongo import MongoClient
import os
from datetime import datetime, timezone
from bson import ObjectId

class CoreService:
    _client = None
    _db = None

    @staticmethod
    def _get_db():
        if CoreService._db is None:
            mongo_host = os.getenv("MONGODB_HOST", "localhost")
            mongo_port = os.getenv("MONGODB_PORT", "27017")
            mongo_db = os.getenv("MONGODB_DB", "isu_ai_support")
            mongo_user = os.getenv("MONGODB_USER", "")
            mongo_password = os.getenv("MONGODB_PASSWORD", "")

            mongo_uri = f"mongodb://{mongo_user}:{mongo_password}@{mongo_host}:{mongo_port}/{mongo_db}?authSource=admin" if mongo_user and mongo_password else f"mongodb://{mongo_host}:{mongo_port}/{mongo_db}"
            CoreService._client = MongoClient(mongo_uri)
            CoreService._db = CoreService._client[mongo_db]
        return CoreService._db

    @staticmethod
    def create_new_session(user_id: str):
        db = CoreService._get_db()
        session_doc = {
            'user_id': user_id,
            'created_at': datetime.now(timezone.utc),
            'last_message': '',
            'last_message_at': datetime.now(timezone.utc)
        }
        result = db.ai_sessions.insert_one(session_doc)
        return str(result.inserted_id)

    @staticmethod
    def create_new_message(session_id: str, sent_by_user: bool, text_content: str, analysis_type: str = "", image_url: str = ""):
        db = CoreService._get_db()
        message_doc = {
            'session_id': session_id,
            'sent_by_user': sent_by_user,
            'text_content': text_content,
            'analysis_type': analysis_type,
            'image_url': image_url,
            'created_at': datetime.now(timezone.utc),
            'updated_at': datetime.now(timezone.utc)
        }
        result = db.ai_messages.insert_one(message_doc)

        # Update session's last_message_at
        db.ai_sessions.update_one(
            {'_id': ObjectId(session_id)},
            {
                '$set': {
                    'last_message_at': message_doc['created_at'],
                    'last_message': text_content[:100]  # store first 100 chars
                }
            }
        )
        return str(result.inserted_id)

    @staticmethod
    def delete_session_by_id(session_id: str):
        db = CoreService._get_db()
        # Delete all messages in the session
        db.ai_messages.delete_many({'session_id': session_id})
        # Delete the session
        db.ai_sessions.delete_one({'_id': ObjectId(session_id)})
        return True

    @staticmethod
    def get_all_sessions_by_user_id(user_id: str):
        db = CoreService._get_db()
        sessions = list(db.ai_sessions.find({'user_id': user_id}).sort('last_message_at', -1))
        return [
            {
                'id': str(s['_id']),
                'user_id': s['user_id'],
                'created_at': s['created_at'],
                'last_message_at': s['last_message_at'],
                'last_message': s.get('last_message', '')
            } for s in sessions
        ]

    @staticmethod
    def get_all_messages_by_session_id(session_id: str):
        db = CoreService._get_db()
        messages = list(db.ai_messages.find({'session_id': session_id}).sort('created_at', 1))
        return [
            {
                'id': str(m['_id']),
                'session_id': m['session_id'],
                'sent_by_user': m['sent_by_user'],
                'text_content': m['text_content'],
                'analysis_type': m.get('analysis_type', ''),
                'image_url': m.get('image_url', ''),
                'created_at': m['created_at'],
                'updated_at': m['updated_at']
            } for m in messages
        ]