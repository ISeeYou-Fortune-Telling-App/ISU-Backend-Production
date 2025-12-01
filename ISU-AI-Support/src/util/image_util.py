import base64

class ImageUtil:
    @staticmethod
    def encode_image(image_path: str) -> str:
        """Mã hóa file ảnh thành chuỗi base64."""
        with open(image_path, "rb") as f:
            return base64.b64encode(f.read()).decode("utf-8")
    
    @staticmethod
    def encode_image_bytes(image_bytes: bytes) -> str:
        """Mã hóa bytes của ảnh thành chuỗi base64."""
        return base64.b64encode(image_bytes).decode("utf-8")