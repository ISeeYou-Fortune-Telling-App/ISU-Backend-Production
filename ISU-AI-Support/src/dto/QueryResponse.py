from pydantic import BaseModel

class QueryResponse(BaseModel):
    answer: str
    total_time: float