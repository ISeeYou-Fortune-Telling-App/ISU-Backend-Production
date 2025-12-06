# Import các thư viện cần thiết
from fastapi import FastAPI, UploadFile, File, Form, Request        # Tạo web API
from fastapi.responses import StreamingResponse            # Hỗ trợ streaming response
import uvicorn                     # Máy chủ web để chạy API
from contextlib import asynccontextmanager
from fastapi.middleware.cors import CORSMiddleware
from controller.rag_controller import RAGController
from dto.QueryRequest import QueryRequest
from dto.QueryResponse import QueryResponse
from dto.UpdateResponse import UpdateResponse
from dto.PalmResponse import PalmResponse
import os
from mongoengine import connect

# Khởi tạo controller
rag_controller = RAGController()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Quản lý vòng đời của ứng dụng
    """
    # Startup
    print("Initializing RAG system...")
    await rag_controller.initialize_system()
    
    # Connect to MongoDB
    mongo_host = os.getenv("MONGODB_HOST", "localhost")
    mongo_port = os.getenv("MONGODB_PORT", "27017")
    mongo_db = os.getenv("MONGODB_DB", "isu_ai_support")
    mongo_user = os.getenv("MONGODB_USER", "")
    mongo_password = os.getenv("MONGODB_PASSWORD", "")
    
    # MongoDB connection is handled by CoreService using PyMongo
    print("MongoDB connection will be established by services")
    
    yield
    # Shutdown (nếu cần cleanup)
    print("Shutting down RAG system...")

# Khởi tạo ứng dụng FastAPI (tạo website API)
app = FastAPI(
    title="LightRAG API",                         # Tên ứng dụng
    description="API for querying RAG system",    # Mô tả chức năng
    version="1.0.0",                              # Phiên bản
    lifespan=lifespan                             # Sử dụng lifespan thay vì on_event
)

# Cấu hình CORS để cho phép truy cập từ mọi nguồn
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    """
    Endpoint kiểm tra sức khỏe cơ bản - trang chủ
    """
    return rag_controller.get_basic_info()

@app.post("/query", response_model=QueryResponse)
async def query_rag(query_request: QueryRequest, request: Request):
    """
    Endpoint chính để hỏi đáp với hệ thống RAG
    Đây là nơi người dùng gửi câu hỏi và nhận câu trả lời
    
    Args:
        query_request: QueryRequest chứa câu hỏi và các tham số
        request: FastAPI Request để extract JWT token
        
    Returns:
        QueryResponse với câu trả lời
    """
    return await rag_controller.process_query(query_request, request)

@app.get("/health")
async def health_check():
    """
    Endpoint kiểm tra sức khỏe chi tiết của hệ thống
    """
    return await rag_controller.get_health_status()

@app.post("/reindex")
async def reindex_data():
    """
    Buộc đánh chỉ mục lại dữ liệu
    Dùng khi muốn cập nhật dữ liệu mới
    """
    return await rag_controller.reindex_data()

@app.post("/analyze-palm", response_model=QueryResponse)
async def analyze_palm(
    request: Request,
    file: UploadFile = File(..., description="Image file of the palm to analyze"),
    user_id: str = Form(None, description="User ID (optional if JWT token provided)"),
    session_id: str = Form(None, description="Session ID (optional, will create new if not provided)"),
    selected_option: int = Form(default=2, description="Search quality: 1=Fast, 2=Balanced, 3=High quality"),
):
    return await rag_controller.analyze_palm(file, user_id, session_id, selected_option, request)

@app.post("/analyze-face", response_model=QueryResponse)
async def analyze_face(
    request: Request,
    file: UploadFile = File(..., description="Image file of the face to analyze for physiognomy"),
    user_id: str = Form(None, description="User ID (optional if JWT token provided)"),
    session_id: str = Form(None, description="Session ID (optional, will create new if not provided)"),
    selected_option: int = Form(default=2, description="Search quality: 1=Fast, 2=Balanced, 3=High quality"),
):
    return await rag_controller.analyze_face(file, user_id, session_id, selected_option, request)

@app.delete("/sessions/{session_id}")
async def delete_session(session_id: str):
    """
    Xóa một session và tất cả messages trong session đó
    
    Args:
        session_id: ID của session cần xóa
        
    Returns:
        dict: Thông báo kết quả xóa
    """
    return rag_controller.delete_session_by_id(session_id)

@app.get("/sessions")
async def get_sessions_by_user(request: Request, user_id: str = None):
    """
    Lấy tất cả sessions của một user
    
    Args:
        request: FastAPI Request để extract JWT token
        user_id: ID của user (query parameter, optional if JWT provided)
        
    Returns:
        dict: Danh sách các sessions
        
    Note:
        JWT token user_id được ưu tiên cao hơn query parameter user_id
    """
    return rag_controller.get_all_sessions_by_user_id(user_id, request)

@app.get("/sessions/{session_id}/messages")
async def get_messages_by_session(session_id: str):
    """
    Lấy tất cả messages trong một session
    
    Args:
        session_id: ID của session
        
    Returns:
        dict: Danh sách các messages
    """
    return rag_controller.get_all_messages_by_session_id(session_id)

if __name__ == "__main__":
    # Chạy máy chủ web
    print("Starting LightRAG HTTP Server...")
    port = int(os.getenv("SERVER_PORT", "8001"))  # Use SERVER_PORT env var, default to 8001
    uvicorn.run(
        "main:app",                           # Tên module:ứng dụng
        host="0.0.0.0",                       # Lắng nghe tất cả địa chỉ IP
        port=port,                            # Cổng từ env hoặc mặc định
        reload=False,                         # Không tự động reload
        log_level="info"                      # Mức độ log
    )
