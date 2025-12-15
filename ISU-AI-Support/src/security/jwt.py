"""
JWT Security Module - Xử lý JWT authentication
"""

import os
import jwt
from typing import Optional
from fastapi import Request


class JWT:
    """
    JWT utility class for extracting user information from JWT tokens
    """
    
    def __init__(self):
        self.secret_key = os.getenv("JWT_SECRET_KEY", "default_secret_key")
        self.algorithm = os.getenv("JWT_ALGORITHM", "HS256")

    def get_user_id_from_header(self, request: Request) -> Optional[str]:
        """
        Extract user_id from Authorization header if Bearer JWT token is provided
        
        Args:
            request: FastAPI Request object
            
        Returns:
            User ID from JWT token or None if not provided/invalid
        """
        try:
            auth_header = request.headers.get("Authorization")
            print(f"[JWT DEBUG] Authorization header: {auth_header}")  # Debug log

            if auth_header and auth_header.startswith("Bearer "):
                token = auth_header[7:]  # Remove "Bearer " prefix
                print(f"[JWT DEBUG] Extracted token: {token[:20]}...")  # Debug log (first 20 chars)
                user_id = self.get_user_id_from_jwt_token(token)
                print(f"[JWT DEBUG] Decoded user_id: {user_id}")  # Debug log
                return user_id
            else:
                print(f"[JWT DEBUG] No valid Bearer token in Authorization header")  # Debug log

            return None
        except Exception as e:
            print(f"[JWT DEBUG] Exception during JWT extraction: {e}")  # Debug log
            return None

    def get_user_id_from_jwt_token(self, token: str) -> Optional[str]:
        """
        Decode JWT token and extract user_id
        
        Args:
            token: JWT token string
            
        Returns:
            User ID from token payload or None if invalid
        """
        try:
            print(f"[JWT DEBUG] Decoding token with secret_key={self.secret_key[:10]}... algorithm={self.algorithm}")
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            print(f"[JWT DEBUG] Decoded payload: {payload}")
            # Try common claim names for user_id
            user_id = payload.get("user_id") or payload.get("sub") or payload.get("uid")
            print(f"[JWT DEBUG] Extracted user_id from payload: {user_id}")
            return user_id
        except jwt.ExpiredSignatureError as e:
            # Token has expired
            print(f"[JWT DEBUG] Token expired: {e}")
            return None
        except jwt.InvalidTokenError as e:
            # Invalid token
            print(f"[JWT DEBUG] Invalid token: {e}")
            return None
        except Exception as e:
            print(f"[JWT DEBUG] Unexpected error: {e}")
            return None


# Singleton instance for easy import
jwt_service = JWT()