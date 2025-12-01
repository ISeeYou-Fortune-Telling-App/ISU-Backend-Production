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
        auth_header = request.headers.get("Authorization")
        
        if auth_header and auth_header.startswith("Bearer "):
            token = auth_header[7:]  # Remove "Bearer " prefix
            return self.get_user_id_from_jwt_token(token)
        
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
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            # Try common claim names for user_id
            user_id = payload.get("user_id") or payload.get("sub") or payload.get("uid")
            return user_id
        except jwt.ExpiredSignatureError:
            # Token has expired
            return None
        except jwt.InvalidTokenError:
            # Invalid token
            return None


# Singleton instance for easy import
jwt_service = JWT()