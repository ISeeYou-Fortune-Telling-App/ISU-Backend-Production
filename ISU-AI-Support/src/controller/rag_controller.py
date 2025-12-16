"""
Controller Layer - Xử lý HTTP requests và responses
Chứa các endpoint API và logic điều khiển luồng xử lý
"""

import os
from typing import AsyncGenerator, Optional
from fastapi import HTTPException, UploadFile, Request
from service.rag_service import RAGService
from dto.QueryRequest import QueryRequest
from dto.QueryResponse import QueryResponse
from util.text_search_util import ValidationUtil, LogUtil
from service.core_service import CoreService
from service.cloudinary_service import CloudinaryService
from security.jwt import jwt_service
from service.rabbitmq_publisher import get_rabbitmq_publisher
from dto.NotificationEvent import NotificationEvent, TargetType


class RAGController:
    """
    Controller xử lý các yêu cầu HTTP cho hệ thống RAG
    Tách biệt logic điều khiển với logic nghiệp vụ
    """
    
    def __init__(self, data_path: str = None):
        # Use environment variables or default paths that work in Docker
        if data_path is None:
            data_path = os.getenv("DATA_PATH", "/app/data/")

        self.rag_service = RAGService(data_path)
        # Get RabbitMQ publisher instance
        self.rabbitmq_publisher = get_rabbitmq_publisher()
        LogUtil.log_info(f"RAG Controller initialized with {data_path}", "CONTROLLER")

    def get_user_id(self, request: Request, user_id: Optional[str] = None) -> str:
        """
        Extract user_id from JWT token in Authorization header if provided,
        otherwise fall back to the user_id parameter
        
        Args:
            request: FastAPI Request object
            user_id: Optional user_id passed directly
            
        Returns:
            User ID from JWT token or fallback user_id
            
        Raises:
            HTTPException: If no valid user_id can be determined
        """
        # Try to get user_id from JWT token first
        jwt_user_id = jwt_service.get_user_id_from_header(request)
        
        if jwt_user_id:
            LogUtil.log_info(f"User ID extracted from JWT token: {jwt_user_id}", "CONTROLLER")
            return jwt_user_id
        
        # Fall back to passed user_id
        if user_id and len(user_id.strip()) > 0:
            return user_id
        
        LogUtil.log_warning("No valid user_id provided (neither JWT nor parameter)", "CONTROLLER")
        raise HTTPException(status_code=401, detail="user_id is required via JWT token or parameter")

    async def initialize_system(self):
        """
        Khởi tạo hệ thống RAG khi ứng dụng bắt đầu
        """
        try:
            await self.rag_service.initialize()
            LogUtil.log_info("RAG system initialized successfully", "CONTROLLER")
        except Exception as e:
            LogUtil.log_error("Failed to initialize RAG system", "CONTROLLER", e)
            raise

    async def process_query(self, query_request: QueryRequest, http_request: Request = None) -> QueryResponse:
        """
        Xử lý yêu cầu truy vấn từ người dùng
        
        Args:
            query_request: Yêu cầu truy vấn chứa câu hỏi và tham số
            http_request: FastAPI Request để extract JWT token (optional)
            
        Returns:
            QueryResponse: Phản hồi chứa câu trả lời
            
        Raises:
            HTTPException: Nếu có lỗi trong quá trình xử lý
        """
        try:
            total_start_time = LogUtil.get_current_time()

            # Bước 0: Extract user_id from JWT token if http_request provided
            # JWT user_id has HIGHEST priority - always overrides parameter user_id
            if http_request:
                jwt_user_id = jwt_service.get_user_id_from_header(http_request)
                if jwt_user_id:
                    if query_request.user_id and query_request.user_id != jwt_user_id:
                        LogUtil.log_warning(
                            f"JWT user_id ({jwt_user_id}) differs from parameter user_id ({query_request.user_id}). Using JWT user_id.",
                            "CONTROLLER"
                        )
                    query_request.user_id = jwt_user_id
                    LogUtil.log_info(f"Using user_id from JWT token: {jwt_user_id}", "CONTROLLER")

            # Bước 1: Validate đầu vào
            if not query_request.question or len(query_request.question.strip()) == 0:
                LogUtil.log_warning("Empty question provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="Question cannot be empty")
            
            if query_request.selected_option not in [1, 2, 3]:
                LogUtil.log_warning(f"Invalid selected_option: {query_request.selected_option}", "CONTROLLER")
                raise HTTPException(status_code=400, detail="selected_option must be 1, 2, or 3")
            
            if not query_request.user_id or len(query_request.user_id.strip()) == 0:
                LogUtil.log_warning("Empty user_id provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="user_id cannot be empty")
            
            # Nếu session_id là None, tạo session mới
            if query_request.session_id is None:
                from service.core_service import CoreService
                query_request.session_id = CoreService.create_new_session(query_request.user_id)
                LogUtil.log_info(f"Created new session {query_request.session_id} for user {query_request.user_id}", "CONTROLLER")

            # Bước 2: Log thông tin truy vấn
            option_names = {1: "Nhanh", 2: "Trung bình", 3: "Chất lượng cao"}
            LogUtil.log_info(
                f"Processing query (option {query_request.selected_option} - {option_names[query_request.selected_option]}): {query_request.question[:50]}...", 
                "CONTROLLER"
            )
            
            # Bước 3: Gọi service để xử lý
            answer = await self.rag_service.get_answer(query_request)
            
            # Bước 4: Tạo phản hồi
            response = QueryResponse(
                answer=answer,
                total_time=LogUtil.get_current_time() - total_start_time
            )
            
            # Bước 5: Gửi push notification qua RabbitMQ (async - không block response)
            try:
                self._send_ai_response_notification(
                    user_id=query_request.user_id,
                    session_id=query_request.session_id,
                    question=query_request.question,
                    answer=answer
                )
            except Exception as notification_error:
                # Log error but don't fail the request
                LogUtil.log_error(
                    "Failed to send push notification, but query completed successfully",
                    "CONTROLLER",
                    notification_error
                )

            LogUtil.log_info("Query processed successfully", "CONTROLLER")
            return response
            
        except HTTPException:
            # Re-raise HTTP exceptions (validation errors)
            raise
        except Exception as e:
            # Log và chuyển đổi các exception khác thành HTTP 500
            LogUtil.log_error("Error processing query", "CONTROLLER", e)
            raise HTTPException(
                status_code=500,
                detail=f"Internal server error: {str(e)}"
            )

    def _send_ai_response_notification(
        self,
        user_id: str,
        session_id: str,
        question: str,
        answer: str
    ):
        """
        Gửi push notification khi AI hoàn thành trả lời

        Args:
            user_id: ID của user
            session_id: ID của session chat
            question: Câu hỏi gốc
            answer: Câu trả lời từ AI
        """
        try:
            # Tạo preview cho notification body
            answer_preview = answer[:100] + "..." if len(answer) > 100 else answer
            question_preview = question[:50] + "..." if len(question) > 50 else question

            # Tạo NotificationEvent với TargetType.CONVERSATION (thay vì AI_CHAT)
            notification_event = NotificationEvent(
                recipient_id=user_id,
                notification_title="🤖 AI Assistant đã trả lời",
                notification_body=answer_preview,
                target_type=TargetType.CONVERSATION,  # Sử dụng CONVERSATION thay vì AI_CHAT
                target_id=session_id,
                fcm_token=None,  # Will be looked up by Notification Service
                meta_data={
                    "sessionId": session_id,
                    "questionPreview": question_preview,
                    "answerLength": str(len(answer))
                }
            )

            # Publish lên RabbitMQ
            success = self.rabbitmq_publisher.publish_notification_event(notification_event)

            if success:
                LogUtil.log_info(
                    f"Push notification sent to user {user_id} for session {session_id}",
                    "CONTROLLER"
                )
            else:
                LogUtil.log_warning(
                    f"Failed to send push notification to user {user_id}",
                    "CONTROLLER"
                )

        except Exception as e:
            LogUtil.log_error(
                f"Error sending push notification: {str(e)}",
                "CONTROLLER",
                e
            )

    async def get_health_status(self) -> dict:
        """
        Lấy trạng thái sức khỏe của hệ thống
        
        Returns:
            dict: Thông tin trạng thái hệ thống
        """
        try:
            service_status = self.rag_service.get_status()
            return {
                "status": "healthy",
                **service_status
            }
        except Exception as e:
            LogUtil.log_error("Error getting health status", "CONTROLLER", e)
            return {
                "status": "unhealthy",
                "error": str(e)
            }

    async def reindex_data(self) -> dict:
        """
        Buộc đánh chỉ mục lại dữ liệu
        
        Returns:
            dict: Kết quả của quá trình đánh chỉ mục
            
        Raises:
            HTTPException: Nếu có lỗi trong quá trình đánh chỉ mục
        """
        try:
            LogUtil.log_info("Starting data reindexing", "CONTROLLER")
            await self.rag_service.initialize(force_reindex=True)
            LogUtil.log_info("Data reindexing completed successfully", "CONTROLLER")
            return {
                "message": "Data reindexed successfully", 
                "status": "success"
            }
        except Exception as e:
            LogUtil.log_error("Error reindexing data", "CONTROLLER", e)
            raise HTTPException(
                status_code=500,
                detail=f"Error reindexing data: {str(e)}"
            )


    def get_basic_info(self) -> dict:
        """
        Lấy thông tin cơ bản của API
        
        Returns:
            dict: Thông tin cơ bản
        """
        return {
            "message": "LightRAG API is running", 
            "status": "healthy",
            "version": "1.0.0"
        }

    async def analyze_palm(self, file: UploadFile, user_id: str = None, session_id: str = None, selected_option: int = 2, http_request: Request = None) -> QueryResponse:
        """
        Xử lý yêu cầu phân tích lòng bàn tay từ file upload
        
        Args:
            file: File ảnh được upload
            user_id: ID của user (optional nếu JWT token được cung cấp)
            session_id: ID của session (optional, sẽ tạo mới nếu không có)
            selected_option: Chế độ tìm kiếm (1=Nhanh, 2=Trung bình, 3=Chất lượng cao)
            http_request: FastAPI Request để extract JWT token
            
        Returns:
            QueryResponse: Phản hồi chứa kết quả phân tích
            
        Raises:
            HTTPException: Nếu có lỗi trong quá trình xử lý
        """
        try:
            # Bước 0: Extract user_id from JWT token if http_request provided
            # JWT user_id has HIGHEST priority - always overrides parameter user_id
            if http_request:
                jwt_user_id = jwt_service.get_user_id_from_header(http_request)
                if jwt_user_id:
                    if user_id and user_id != jwt_user_id:
                        LogUtil.log_warning(
                            f"JWT user_id ({jwt_user_id}) differs from parameter user_id ({user_id}). Using JWT user_id.",
                            "CONTROLLER"
                        )
                    user_id = jwt_user_id
                    LogUtil.log_info(f"Using user_id from JWT token: {jwt_user_id}", "CONTROLLER")

            # Bước 1: Validate đầu vào
            if not file:
                LogUtil.log_warning("No file provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="Image file is required")
            
            if not user_id or len(user_id.strip()) == 0:
                LogUtil.log_warning("Empty user_id provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="user_id cannot be empty")
            
            if selected_option not in [1, 2, 3]:
                LogUtil.log_warning(f"Invalid selected_option: {selected_option}", "CONTROLLER")
                raise HTTPException(status_code=400, detail="selected_option must be 1, 2, or 3")

            # Bước 2: Kiểm tra loại file
            if not file.content_type or not file.content_type.startswith("image/"):
                LogUtil.log_warning(f"Invalid file type: {file.content_type}", "CONTROLLER")
                raise HTTPException(status_code=400, detail="File must be an image")

            # Bước 3: Đọc nội dung file
            image_bytes = await file.read()
            if not image_bytes:
                LogUtil.log_warning("Empty file provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="Image file is empty")

            # Bước 4: Log thông tin yêu cầu
            option_names = {1: "Nhanh", 2: "Trung bình", 3: "Chất lượng cao"}
            LogUtil.log_info(
                f"Processing palm analysis for file: {file.filename} (option {selected_option} - {option_names[selected_option]})", 
                "CONTROLLER"
            )
            
            # Bước 5: Upload ảnh lên Cloudinary
            try:
                image_url = CloudinaryService.upload_palm_image(image_bytes)
                LogUtil.log_info(f"Palm image uploaded to Cloudinary: {image_url}", "CONTROLLER")
            except Exception as e:
                LogUtil.log_warning(f"Failed to upload image to Cloudinary: {e}", "CONTROLLER")
                image_url = ""  # Continue without image URL if upload fails
            
            # Bước 6: Gọi service để xử lý
            analysis = self.rag_service.analyze_palm_details(image_bytes)
            
            LogUtil.log_info("Palm analysis processed successfully", "CONTROLLER")

            # Tạo query với keywords song ngữ rõ ràng để RAG tìm thấy chunks liên quan
            question = f"""Dựa vào kiến thức Xem Tướng Bàn Tay, Chỉ Tay Học (Palmistry) trong cơ sở dữ liệu, hãy phân tích chi tiết lòng bàn tay.

MÔ TẢ LÒNG BÀN TAY:
{analysis}

YÊU CẦU PHÂN TÍCH CHI TIẾT:

1. PHÂN TÍCH CÁC ĐƯỜNG CHỈ TAY CHÍNH:
   - Đường Đời (Life Line): độ dài, độ sâu, độ rõ nét, điểm bắt đầu/kết thúc, độ cong
   - Đường Trí Tuệ (Head Line): độ dài, độ rõ, hướng đi (thẳng/cong), ý nghĩa về trí tuệ và tư duy
   - Đường Tình Cảm (Heart Line): độ dài, độ rõ, hướng đi, ý nghĩa về tình yêu và cảm xúc
   - Đường Vận Mệnh (Fate Line): nếu có, phân tích về sự nghiệp và định mệnh

2. PHÂN TÍCH HÌNH DẠNG BÀN TAY:
   - Hình dạng tổng thể (vuông/dài/hình nón...)
   - Độ dài các ngón tay so với lòng bàn tay
   - Hình dạng đầu ngón tay
   - Ý nghĩa về tính cách và năng lực

3. PHÂN TÍCH CÁC GÒ TRÊN BÀN TAY (Mount/Gò):
   - Gò Kim Tinh (Mount of Venus)
   - Gò Mộc Tinh (Mount of Jupiter)  
   - Gò Thổ Tinh (Mount of Saturn)
   - Các gò khác nếu có
   - Độ nổi bật và ý nghĩa

4. PHÂN TÍCH CÁC DẤU HIỆU ĐẶC BIỆT:
   - Đường đứt quãng, đường đôi, đường nhánh
   - Các dấu sao, chấm, hòn đảo
   - Màu sắc và kết cấu da

5. TỔNG HỢP NHẬN ĐỊNH:
   - Tính cách: điểm mạnh, điểm yếu
   - Vận mệnh: xu hướng cuộc đời
   - Tình duyên: tình cảm, hôn nhân
   - Sự nghiệp: công việc, thành công
   - Sức khỏe: thể chất, tinh thần
   - Tài lộc: khả năng tích lũy tài sản

6. LỜI KHUYÊN:
   - Hướng phát triển phù hợp
   - Điều cần chú ý và cải thiện

Lưu ý: Hãy dựa vào kiến thức về Chỉ Tay Học, Xem Tướng Bàn Tay, Ba Đường Chỉ Tay Chính, Các Gò Trên Bàn Tay trong cơ sở dữ liệu để đưa ra phân tích đầy đủ và chi tiết."""

            return await self.process_query(QueryRequest(
                question=question,
                user_id=user_id,
                session_id=session_id,
                selected_option=selected_option,
                force_reindex=False,
                image_url=image_url
            ))
            
        except HTTPException:
            # Re-raise HTTP exceptions (validation errors)
            raise
        except Exception as e:
            # Log và chuyển đổi các exception khác thành HTTP 500
            LogUtil.log_error("Error processing palm analysis", "CONTROLLER", e)
            raise HTTPException(
                status_code=500,
                detail=f"Internal server error: {str(e)}"
            )

    async def analyze_face(self, file: UploadFile, user_id: str = None, session_id: str = None, selected_option: int = 2, http_request: Request = None) -> QueryResponse:
        """
        Xử lý yêu cầu phân tích nhân tướng học từ file upload
        
        Args:
            file: File ảnh được upload
            user_id: ID của user (optional nếu JWT token được cung cấp)
            session_id: ID của session (optional, sẽ tạo mới nếu không có)
            selected_option: Chế độ tìm kiếm (1=Nhanh, 2=Trung bình, 3=Chất lượng cao)
            http_request: FastAPI Request để extract JWT token
            
        Returns:
            QueryResponse: Phản hồi chứa kết quả phân tích nhân tướng học
            
        Raises:
            HTTPException: Nếu có lỗi trong quá trình xử lý
        """
        try:
            # Bước 0: Extract user_id from JWT token if http_request provided
            # JWT user_id has HIGHEST priority - always overrides parameter user_id
            if http_request:
                jwt_user_id = jwt_service.get_user_id_from_header(http_request)
                if jwt_user_id:
                    if user_id and user_id != jwt_user_id:
                        LogUtil.log_warning(
                            f"JWT user_id ({jwt_user_id}) differs from parameter user_id ({user_id}). Using JWT user_id.",
                            "CONTROLLER"
                        )
                    user_id = jwt_user_id
                    LogUtil.log_info(f"Using user_id from JWT token: {jwt_user_id}", "CONTROLLER")

            # Bước 1: Validate đầu vào
            if not file:
                LogUtil.log_warning("No file provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="Image file is required")
            
            if not user_id or len(user_id.strip()) == 0:
                LogUtil.log_warning("Empty user_id provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="user_id cannot be empty")
            
            if selected_option not in [1, 2, 3]:
                LogUtil.log_warning(f"Invalid selected_option: {selected_option}", "CONTROLLER")
                raise HTTPException(status_code=400, detail="selected_option must be 1, 2, or 3")

            # Bước 2: Kiểm tra loại file
            if not file.content_type or not file.content_type.startswith("image/"):
                LogUtil.log_warning(f"Invalid file type: {file.content_type}", "CONTROLLER")
                raise HTTPException(status_code=400, detail="File must be an image")

            # Bước 3: Đọc nội dung file
            image_bytes = await file.read()
            if not image_bytes:
                LogUtil.log_warning("Empty file provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="Image file is empty")

            # Bước 4: Log thông tin yêu cầu
            option_names = {1: "Nhanh", 2: "Trung bình", 3: "Chất lượng cao"}
            LogUtil.log_info(
                f"Processing face analysis for file: {file.filename} (option {selected_option} - {option_names[selected_option]})", 
                "CONTROLLER"
            )
            
            # Bước 5: Upload ảnh lên Cloudinary
            try:
                image_url = CloudinaryService.upload_face_image(image_bytes)
                LogUtil.log_info(f"Face image uploaded to Cloudinary: {image_url}", "CONTROLLER")
            except Exception as e:
                LogUtil.log_warning(f"Failed to upload image to Cloudinary: {e}", "CONTROLLER")
                image_url = ""  # Continue without image URL if upload fails
            
            # Bước 6: Gọi service để xử lý
            analysis = self.rag_service.analyze_face_details(image_bytes)
            
            LogUtil.log_info("Face analysis processed successfully", "CONTROLLER")

            # Tạo query với keywords rõ ràng để RAG tìm thấy chunks liên quan
            question = f"""Hãy phân tích nhân tướng học dựa vào kiến thức về Xem Tướng Mặt (Physiognomy) trong cơ sở dữ liệu.

Thông tin chi tiết về khuôn mặt:
{analysis}

Yêu cầu phân tích:
- Phân tích NGŨ QUAN: Mắt, Mũi, Miệng, Tai, Lông mày
- Phân tích 12 CUNG VỊ trên khuôn mặt
- Phân tích hình dạng khuôn mặt và tỷ lệ
- Đưa ra nhận định về tính cách, vận mệnh, tình duyên, sự nghiệp, tài lộc, sức khỏe
- Đưa ra lời khuyên phù hợp

Hãy dựa vào kiến thức Nhân Tướng Học trong cơ sở dữ liệu để phân tích chi tiết."""

            return await self.process_query(QueryRequest(
                question=question,
                user_id=user_id,
                session_id=session_id,
                selected_option=selected_option,
                force_reindex=False,
                image_url=image_url
            ))
            
        except HTTPException:
            # Re-raise HTTP exceptions (validation errors)
            raise
        except Exception as e:
            # Log và chuyển đổi các exception khác thành HTTP 500
            LogUtil.log_error("Error processing face analysis", "CONTROLLER", e)
            raise HTTPException(
                status_code=500,
                detail=f"Internal server error: {str(e)}"
            )

    def delete_session_by_id(self, session_id: str) -> dict:
        """
        Xóa session và tất cả messages trong session đó
        
        Args:
            session_id: ID của session cần xóa
            
        Returns:
            dict: Thông báo kết quả xóa
            
        Raises:
            HTTPException: Nếu có lỗi trong quá trình xóa
        """
        try:
            # Validate session_id
            if not session_id or len(session_id.strip()) == 0:
                LogUtil.log_warning("Empty session_id provided for deletion", "CONTROLLER")
                raise HTTPException(status_code=400, detail="session_id cannot be empty")
            
            # Thực hiện xóa
            result = CoreService.delete_session_by_id(session_id)
            
            if result:
                LogUtil.log_info(f"Session {session_id} deleted successfully", "CONTROLLER")
                return {"message": "Session deleted successfully", "session_id": session_id}
            else:
                LogUtil.log_warning(f"Failed to delete session {session_id}", "CONTROLLER")
                raise HTTPException(status_code=500, detail="Failed to delete session")
                
        except HTTPException:
            raise
        except Exception as e:
            LogUtil.log_error("Error deleting session", "CONTROLLER", e)
            raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

    def get_all_sessions_by_user_id(self, user_id: str, request: Request = None) -> dict:
        """
        Lấy tất cả sessions của một user
        
        Args:
            user_id: ID của user (fallback nếu không có JWT)
            request: FastAPI Request để extract JWT token
            
        Returns:
            dict: Danh sách các sessions
            
        Raises:
            HTTPException: Nếu có lỗi trong quá trình truy vấn
            
        Note:
            JWT token user_id được ưu tiên cao hơn parameter user_id
        """
        try:
            # DEBUG: Log để kiểm tra
            LogUtil.log_info(f"get_all_sessions_by_user_id called with user_id={user_id}, request={request}", "CONTROLLER")

            # Extract user_id from JWT token - HIGHEST priority
            effective_user_id = user_id
            if request:
                LogUtil.log_info(f"Request object provided, attempting to extract JWT", "CONTROLLER")
                jwt_user_id = jwt_service.get_user_id_from_header(request)
                LogUtil.log_info(f"JWT extraction result: {jwt_user_id}", "CONTROLLER")

                if jwt_user_id:
                    if user_id and user_id != jwt_user_id:
                        LogUtil.log_warning(
                            f"JWT user_id ({jwt_user_id}) differs from parameter user_id ({user_id}). Using JWT user_id.",
                            "CONTROLLER"
                        )
                    effective_user_id = jwt_user_id
                    LogUtil.log_info(f"Using user_id from JWT: {jwt_user_id}", "CONTROLLER")
                else:
                    LogUtil.log_warning(f"No JWT user_id found, using parameter user_id: {user_id}", "CONTROLLER")
            else:
                LogUtil.log_warning("No request object provided for JWT extraction", "CONTROLLER")

            # Validate user_id
            if not effective_user_id or len(effective_user_id.strip()) == 0:
                LogUtil.log_warning("Empty user_id provided (no JWT and no parameter)", "CONTROLLER")
                raise HTTPException(status_code=400, detail="user_id cannot be empty - provide via JWT token or query parameter")
            
            # Lấy danh sách sessions
            sessions = CoreService.get_all_sessions_by_user_id(effective_user_id)
            
            LogUtil.log_info(f"Retrieved {len(sessions)} sessions for user {effective_user_id}", "CONTROLLER")
            return {"sessions": sessions, "count": len(sessions)}
            
        except HTTPException:
            raise
        except Exception as e:
            LogUtil.log_error("Error retrieving sessions", "CONTROLLER", e)
            raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

    def get_all_messages_by_session_id(self, session_id: str) -> dict:
        """
        Lấy tất cả messages trong một session
        
        Args:
            session_id: ID của session
            
        Returns:
            dict: Danh sách các messages
            
        Raises:
            HTTPException: Nếu có lỗi trong quá trình truy vấn
        """
        try:
            # Validate session_id
            if not session_id or len(session_id.strip()) == 0:
                LogUtil.log_warning("Empty session_id provided", "CONTROLLER")
                raise HTTPException(status_code=400, detail="session_id cannot be empty")
            
            # Lấy danh sách messages
            messages = CoreService.get_all_messages_by_session_id(session_id)
            
            LogUtil.log_info(f"Retrieved {len(messages)} messages for session {session_id}", "CONTROLLER")
            return {"messages": messages, "count": len(messages)}
            
        except HTTPException:
            raise
        except Exception as e:
            LogUtil.log_error("Error retrieving messages", "CONTROLLER", e)
            raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

