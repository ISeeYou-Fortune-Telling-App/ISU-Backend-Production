from pydantic import BaseModel, Field
from typing import Optional, Literal

class QueryRequest(BaseModel):
    question: str
    user_id: str 
    session_id: Optional[str] = None
    selected_option: Literal[1, 2, 3] = Field(
        default=2,
        description="""
        Chế độ tìm kiếm:
        1: Nhanh nhất - Độ chính xác và độ rộng thấp nhất
        2: Trung bình - Cân bằng giữa tốc độ và chất lượng
        3: Chất lượng cao nhất - Độ rộng, độ sâu và độ chính xác cao nhất nhưng thời gian lâu nhất
        """
    )
    force_reindex: Optional[bool] = False
    image_url: Optional[str] = None