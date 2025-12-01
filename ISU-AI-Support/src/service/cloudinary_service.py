"""
Cloudinary Service - Upload và quản lý hình ảnh trên Cloudinary
"""

import os
import cloudinary
import cloudinary.uploader
from typing import Optional
import uuid


class CloudinaryService:
    """
    Service để upload và quản lý hình ảnh trên Cloudinary
    """
    
    _initialized = False
    
    @classmethod
    def initialize(cls):
        """
        Khởi tạo Cloudinary configuration từ biến môi trường
        """
        if cls._initialized:
            return
        
        cloud_name = os.getenv("CLOUDINARY_CLOUD_NAME")
        api_key = os.getenv("CLOUDINARY_API_KEY")
        api_secret = os.getenv("CLOUDINARY_API_SECRET")
        
        if not all([cloud_name, api_key, api_secret]):
            raise ValueError("Missing Cloudinary configuration. Please set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET in .env file")
        
        cloudinary.config(
            cloud_name=cloud_name,
            api_key=api_key,
            api_secret=api_secret,
            secure=True
        )
        
        cls._initialized = True
        print(f"Cloudinary initialized with cloud_name: {cloud_name}")
    
    @classmethod
    def upload_image(cls, image_bytes: bytes, folder: str = "ai-analysis", public_id: Optional[str] = None) -> str:
        """
        Upload hình ảnh lên Cloudinary
        
        Args:
            image_bytes: Nội dung file ảnh dạng bytes
            folder: Thư mục lưu trữ trên Cloudinary
            public_id: ID công khai cho ảnh (optional, sẽ tự generate nếu không có)
            
        Returns:
            str: URL của ảnh đã upload
        """
        cls.initialize()
        
        # Generate unique public_id if not provided
        if not public_id:
            public_id = f"{folder}/{uuid.uuid4()}"
        
        try:
            # Upload ảnh lên Cloudinary
            result = cloudinary.uploader.upload(
                image_bytes,
                public_id=public_id,
                folder=folder,
                resource_type="image",
                overwrite=True,
                transformation=[
                    {"quality": "auto:good"},
                    {"fetch_format": "auto"}
                ]
            )
            
            # Trả về secure URL
            return result.get("secure_url", result.get("url"))
            
        except Exception as e:
            print(f"Error uploading image to Cloudinary: {e}")
            raise
    
    @classmethod
    def upload_palm_image(cls, image_bytes: bytes) -> str:
        """
        Upload hình ảnh lòng bàn tay lên Cloudinary
        
        Args:
            image_bytes: Nội dung file ảnh dạng bytes
            
        Returns:
            str: URL của ảnh đã upload
        """
        return cls.upload_image(image_bytes, folder="palm-analysis")
    
    @classmethod
    def upload_face_image(cls, image_bytes: bytes) -> str:
        """
        Upload hình ảnh khuôn mặt lên Cloudinary
        
        Args:
            image_bytes: Nội dung file ảnh dạng bytes
            
        Returns:
            str: URL của ảnh đã upload
        """
        return cls.upload_image(image_bytes, folder="face-analysis")
    
    @classmethod
    def delete_image(cls, public_id: str) -> bool:
        """
        Xóa hình ảnh từ Cloudinary
        
        Args:
            public_id: Public ID của ảnh cần xóa
            
        Returns:
            bool: True nếu xóa thành công
        """
        cls.initialize()
        
        try:
            result = cloudinary.uploader.destroy(public_id)
            return result.get("result") == "ok"
        except Exception as e:
            print(f"Error deleting image from Cloudinary: {e}")
            return False
