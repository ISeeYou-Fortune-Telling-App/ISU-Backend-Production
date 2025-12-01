from pydantic import BaseModel
from typing import Optional

class PalmResponse(BaseModel):
    analysis: str
    status: str