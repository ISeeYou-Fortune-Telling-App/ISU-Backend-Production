"""
Service Layer - Xử lý logic nghiệp vụ chính của hệ thống RAG
Chứa tất cả logic xử lý RAG, khởi tạo và tìm kiếm
"""

import os
import json
from typing import Optional, List
from lightrag import LightRAG, QueryParam
from ingestion import initialize_rag, index_file
from util.text_search_util import TextSearchUtil
from util.image_util import ImageUtil
from service.core_service import CoreService
from openai import OpenAI
from dto.QueryRequest import QueryRequest


class RAGService:
    """
    Lớp dịch vụ để quản lý các hoạt động của hệ thống RAG
    Đây là "bộ não" chính xử lý tất cả logic nghiệp vụ
    """
    
    def __init__(self, data_path: str = "../../data/"):
        self.data_path = data_path           # Đường dẫn đến directory chứa data
        self.data_files = []                # Danh sách tất cả files cần index
        self.rag = None                      # Đối tượng RAG (ban đầu chưa có)
        self.raw_text: Optional[str] = None  # Văn bản dự phòng nếu RAG lỗi
        self.indexing_complete: bool = False # Trạng thái đánh chỉ mục
        self.openai_client = None           # OpenAI client for image analysis

    def _has_existing_data(self, working_dir: str = "./rag_storage") -> bool:
        """
        Kiểm tra xem dữ liệu đã được đánh chỉ mục trước đó hay chưa
        Kiểm tra sự tồn tại của các files KV store và có dữ liệu thực sự
        
        Returns:
            bool: True nếu có dữ liệu đã tồn tại, False nếu không
        """
        # Các file KV store cần kiểm tra
        required_kv_files = [
            "kv_store_full_docs.json",
            "kv_store_text_chunks.json", 
            "kv_store_full_entities.json",
            "kv_store_full_relations.json"
        ]
        
        try:
            # Kiểm tra thư mục working_dir có tồn tại không
            if not os.path.exists(working_dir):
                print(f"Working directory {working_dir} does not exist")
                return False
            
            # Kiểm tra từng file KV store
            for filename in required_kv_files:
                file_path = os.path.join(working_dir, filename)
                
                # File phải tồn tại
                if not os.path.exists(file_path):
                    print(f"KV store file {filename} does not exist")
                    return False
                
                # File không được rỗng
                if os.path.getsize(file_path) == 0:
                    print(f"KV store file {filename} is empty")
                    return False
                
                # Kiểm tra nội dung JSON hợp lệ và có dữ liệu
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                        
                    # Nếu là dict và rỗng, hoặc là list và rỗng
                    if (isinstance(data, dict) and len(data) == 0) or \
                       (isinstance(data, list) and len(data) == 0):
                        print(f"KV store file {filename} has no data")
                        return False
                        
                except json.JSONDecodeError as e:
                    print(f"KV store file {filename} has invalid JSON: {e}")
                    return False
            
            print("All KV store files exist and contain data")
            return True
            
        except Exception as e:
            print(f"Error checking existing data: {e}")
            return False

    async def _check_rag_data_status(self, rag: LightRAG) -> bool:
        """
        Kiểm tra trạng thái dữ liệu trong RAG system (Neo4j và Qdrant)
        
        Args:
            rag: LightRAG instance đã được khởi tạo
            
        Returns:
            bool: True nếu có dữ liệu, False nếu không
        """
        try:
            # Kiểm tra đơn giản bằng cách thử query
            # Nếu có dữ liệu, query sẽ trả về kết quả
            # Nếu không có dữ liệu, sẽ không có kết quả hoặc lỗi
            test_query = "test"
            try:
                # Thử query đơn giản để kiểm tra xem có dữ liệu không
                from lightrag import QueryParam
                test_param = QueryParam(mode="naive", top_k=1)
                result = await rag.aquery(test_query, param=test_param)
                
                # Nếu có kết quả và không phải error message
                if result and len(result.strip()) > 0 and "I don't know" not in result and "không biết" not in result.lower():
                    print("RAG storages appear to have data (query returned meaningful result)")
                    return True
                else:
                    print("RAG storages may be empty (query returned empty or default response)")
                    return False
                    
            except Exception as e:
                print(f"Could not query RAG system to check data: {e}")
                return False
                    
        except Exception as e:
            print(f"Error checking RAG data status: {e}")
            return False

    async def initialize(self, force_reindex: bool = False):
        """
        Khởi tạo và đánh chỉ mục dữ liệu từ nhiều files không đồng bộ. 
        Sử dụng hàm index_file với thử lại nhiều lần,
        và dự phòng lưu văn bản thô nếu đánh chỉ mục thất bại.
        """
        
        # Bước 1: Lấy tất cả files cần đánh index
        self.data_files = []
        for entry in os.listdir(self.data_path):
            full_path = os.path.join(self.data_path, entry)
            if os.path.isfile(full_path) and (entry.endswith('.txt')):
                self.data_files.append(full_path)
        
        # Bước 2: Khởi tạo RAG nếu chưa có
        if self.rag is None:
            print("Initializing RAG system...")
            try:
                self.rag = await initialize_rag()
            except Exception as e:
                print(f"initialize_rag failed: {e}")
                self.rag = None

        # Bước 4: Kiểm tra xem có cần đánh chỉ mục không
        should_skip_indexing = False
        
        if not force_reindex and self.rag is not None:
            print("Checking if data already exists...")
            
            # Kiểm tra KV store files
            has_kv_data = self._has_existing_data("./rag_storage")
            
            # Kiểm tra RAG storage (Neo4j + Qdrant) 
            has_rag_data = False
            if has_kv_data:
                try:
                    has_rag_data = await self._check_rag_data_status(self.rag)
                except Exception as e:
                    print(f"Could not check RAG data status: {e}")
                    has_rag_data = False
            
            # Chỉ skip nếu cả KV store và RAG storage đều có dữ liệu
            if has_kv_data and has_rag_data:
                should_skip_indexing = True
                print("✅ Data already exists in both KV storage and RAG storages, skipping reindexing")
                self.indexing_complete = True
            elif has_kv_data:
                print("⚠️  KV storage has data but RAG storages may be incomplete, will reindex")
            else:
                print("📄 No existing data found, will proceed with indexing")

        # Bước 5: Đánh chỉ mục nếu cần thiết
        if not should_skip_indexing and self.rag is not None:
            if force_reindex:
                print("🔄 Force reindexing enabled, proceeding with indexing...")
            else:
                print("📊 Starting fresh indexing...")
                
            # Đánh chỉ mục tất cả files
            print(f"Indexing data from {len(self.data_files)} files...")
            indexed_files = []
            failed_files = []
            
            for file_path in self.data_files:
                print(f"Indexing {file_path}...")
                last_exc = None
                
                # Thử 3 lần cho mỗi file
                for attempt in range(1, 4):
                    try:
                        print(f"  Attempt {attempt}/3 for {os.path.basename(file_path)}...")
                        
                        await index_file(self.rag, file_path)
                        
                        print(f"  ✅ Successfully indexed {os.path.basename(file_path)}")
                        indexed_files.append(file_path)
                        last_exc = None
                        break
                    except Exception as e:
                        print(f"  ❌ Attempt {attempt} failed for {os.path.basename(file_path)}: {e}")
                        last_exc = e

                # Nếu file thất bại sau tất cả attempts
                if last_exc:
                    print(f"Failed to index {file_path} after retries: {last_exc}")
                    failed_files.append(file_path)

            # Báo cáo kết quả indexing
            print(f"Indexing summary:")
            print(f"  ✅ Successfully indexed: {len(indexed_files)} files")
            print(f"  ❌ Failed to index: {len(failed_files)} files")
            
            if indexed_files:
                print("Successfully indexed files:")
                for file_path in indexed_files:
                    print(f"  - {os.path.basename(file_path)}")
            
            if failed_files:
                print("Failed files:")
                for file_path in failed_files:
                    print(f"  - {os.path.basename(file_path)}")
                
                # Chuẩn bị văn bản dự phòng từ tất cả files có thể đọc được
                self._prepare_fallback_text()

                # Thử chèn trực tiếp văn bản dự phòng
                try:
                    if self.rag is not None and self.raw_text:
                        await self.rag.ainsert(self.raw_text)
                        print("Fallback raw indexing complete!")
                        self.indexing_complete = True
                except Exception as e2:
                    print(f"Fallback ainsert also failed: {e2}. Will use local text search fallback.")
                    self.indexing_complete = False
            else:
                # Tất cả files đều indexed thành công
                self.indexing_complete = True
                print("🎉 All files indexed successfully!")
                
        elif self.rag is None:
            # Không thể khởi tạo RAG; tải văn bản thô để tìm kiếm cục bộ
            self._prepare_fallback_text()
            print("Loaded raw text from all files for local fallback search.")
            self.indexing_complete = False

        # Đánh dấu hoàn thành đánh chỉ mục nếu rag tồn tại và không có lỗi
        if self.rag is not None and not self.indexing_complete:
            # Nếu rag tồn tại và chưa đặt indexing_complete, đặt True
            self.indexing_complete = True
            
        return self.rag

    def _prepare_fallback_text(self):
        self.raw_text = "Rag khởi tạo bị lỗi, check lại quá trình đánh index, các file vector db, các cấu hình khác hoặc api key"

    def _get_query_params_by_option(self, selected_option: int) -> dict:
        """
        Lấy các tham số query dựa trên option được chọn
        
        Args:
            selected_option: 1 (nhanh), 2 (trung bình), 3 (chất lượng cao)
            
        Returns:
            dict: Dictionary chứa các tham số cho QueryParam
        """
        if selected_option == 1:
            # OPTION 1: NHANH NHẤT
            return {
                "mode": "naive",           # Tìm kiếm đơn giản nhất
                "top_k": 3,                # Chỉ lấy 3 kết quả top
                "chunk_top_k": 2,          # Chỉ lấy 2 chunks
                "enable_rerank": False,    # Tắt reranking để tiết kiệm thời gian
                "max_entity_tokens": 500,  # Giới hạn tokens thấp
                "max_relation_tokens": 500,
                "max_total_tokens": 2000,  # Tổng tokens thấp
            }
        elif selected_option == 2:
            # OPTION 2: TRUNG BÌNH - CÂN BẰNG
            return {
                "mode": "mix",             # Mix mode cân bằng
                "top_k": 5,                # 5 kết quả
                "chunk_top_k": 4,          # 4 chunks
                "enable_rerank": True,     # Bật reranking
                "max_entity_tokens": 1500, # Tokens trung bình
                "max_relation_tokens": 1500,
                "max_total_tokens": 4000,
            }
        else:  # selected_option == 3
            # OPTION 3: CHẤT LƯỢNG CAO NHẤT
            return {
                "mode": "hybrid",          # Hybrid mode kết hợp local + global
                "top_k": 10,               # Lấy nhiều kết quả nhất
                "chunk_top_k": 8,          # Lấy nhiều chunks
                "enable_rerank": True,     # Bật reranking
                "max_entity_tokens": 3000, # Tokens cao
                "max_relation_tokens": 3000,
                "max_total_tokens": 8000,  # Tổng tokens cao
            }

    async def get_answer(
        self, 
        request: 'QueryRequest'
    ) -> str:
        """
        Xử lý câu hỏi và trả về câu trả lời
        
        Args:
            request: QueryRequest chứa câu hỏi và tham số
        """
        # Bước 1: Thử khởi tạo RAG
        try:
            await self.initialize(force_reindex=request.force_reindex)
        except Exception as e:
            print(f"RAG initialization failed in get_answer: {e}")

        # Bước 2: Nếu RAG có sẵn, thử sử dụng nó
        if self.rag is not None:
            # Lấy parameters dựa trên option
            query_param_dict = self._get_query_params_by_option(request.selected_option)
            query_param = QueryParam(**query_param_dict)
            
            try:
                # TODO: Call to push notification service and core backend
                answer = await self.rag.aquery(request.question, param=query_param)
                
                # Save user message and AI response to database
                if request.user_id and request.session_id:
                    # Determine analysis type based on question content
                    analysis_type = "query"
                    if "lòng bàn tay" in request.question.lower():
                        analysis_type = "palm"
                    elif "nhân tướng học" in request.question.lower():
                        analysis_type = "face"
                    
                    # Get image_url from request if available
                    image_url = getattr(request, 'image_url', None) or ""
                    
                    # Save user message
                    CoreService.create_new_message(
                        session_id=request.session_id,
                        sent_by_user=True,
                        text_content=request.question,
                        analysis_type=analysis_type,
                        image_url=image_url
                    )
                    # Save AI response
                    CoreService.create_new_message(
                        session_id=request.session_id,
                        sent_by_user=False,
                        text_content=answer,
                        analysis_type="response"
                    )
                
                return answer
            except Exception as e:
                print(f"RAG query failed: {e}")

        # Bước 3: Dự phòng: tìm kiếm cục bộ trên văn bản thô
        print("Using local fallback search...")
        if not self.raw_text:
            self._prepare_fallback_text()
            if not self.raw_text:
                return "Sorry, I'm not able to provide an answer to that question.[no-data]"

        # Sử dụng utility class để tìm kiếm
        text_search = TextSearchUtil()
        # Lấy top_k từ params dựa trên option
        params = self._get_query_params_by_option(request.selected_option)
        answer = text_search.local_search(self.raw_text, request.question, params["top_k"])
        
        # Save to database even for fallback
        if request.user_id and request.session_id:
            # Determine analysis type based on question content
            analysis_type = "query"
            if "lòng bàn tay" in request.question.lower():
                analysis_type = "palm"
            elif "nhân tướng học" in request.question.lower():
                analysis_type = "face"
            
            # Get image_url from request if available
            image_url = getattr(request, 'image_url', None) or ""
            
            CoreService.create_new_message(
                session_id=request.session_id,
                sent_by_user=True,
                text_content=request.question,
                analysis_type=analysis_type,
                image_url=image_url
            )
            CoreService.create_new_message(
                session_id=request.session_id,
                sent_by_user=False,
                text_content=answer,
                analysis_type="response"
            )
        
        return answer


    def get_status(self) -> dict:
        """
        Lấy trạng thái hiện tại của hệ thống RAG
        """
        return {
            "rag_initialized": self.rag is not None,
            "indexing_complete": self.indexing_complete,
            "data_files": self.data_files,
            "data_files_count": len(self.data_files),
            "data_path": self.data_path,  # Backward compatibility
            "has_fallback_text": self.raw_text is not None,
            "has_existing_kv_data": self._has_existing_data("./rag_storage")
        }
    
    def _get_openai_client(self) -> OpenAI:
        if self.openai_client is None:
            self.openai_client = OpenAI()
        return self.openai_client
    
    def analyze_palm_details(self, image_bytes: bytes) -> str:
        base64_image = ImageUtil.encode_image_bytes(image_bytes)
        client = self._get_openai_client()
        response = client.chat.completions.create(
            model="gpt-5",
            max_completion_tokens=4000,
            messages=[{
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": """DETAILED PALM ANALYSIS:

Please observe carefully and fully describe the following characteristics:
1. MAJOR PALM LINES:
- Life Line: length, depth, clarity, start/end points, curvature/straightness
- Head Line: similar characteristics as above
- Heart Line: similar characteristics as above
- Other minor lines (if any)
- Other features (e.g., fingerprints, wrinkles, scars)

2. SHAPE AND SIZE:
- Hand shape (square, rectangular, triangular)
- Finger length compared to the palm
- Fingertip shapes

3. PALM MOUNTS:
- Mount of Venus, Jupiter, Saturn, etc.
- Prominence of the mounts

4. SPECIAL SIGNS:
- Stars, dots, islands, intersections
- Skin color and texture

REQUIREMENT: Describe in as much detail as possible, only list observable features, do NOT interpret meanings. Please respond in Vietnamese."""
                    },
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}
                    },
                ],
            }],
        )
        return response.choices[0].message.content.strip()

    def analyze_face_details(self, image_bytes: bytes) -> str:
        """
        Phân tích nhân tướng học từ ảnh khuôn mặt
        
        Args:
            image_bytes: Dữ liệu ảnh dưới dạng bytes
            
        Returns:
            str: Kết quả phân tích nhân tướng học chi tiết
        """
        base64_image = ImageUtil.encode_image_bytes(image_bytes)
        client = self._get_openai_client()
        response = client.chat.completions.create(
            model="gpt-5",
            max_completion_tokens=4000,
            messages=[{
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": """DETAILED PHYSIOGNOMY ANALYSIS:

Please carefully observe and fully describe the following facial characteristics according to physiognomy:

1. FACE SHAPE:
- General shape (round, square, oval, triangular, diamond, rectangular)
- Length/width ratio
- Contour and symmetry

2. FOREHEAD (WEALTH AND CAREER AREA):
- Height, width of forehead
- Shape (round, square, sloping)
- Wrinkles and horizontal lines on forehead
- Hairline position and shape

3. EYES (MARRIAGE AND RELATIONSHIP AREA):
- Eye shape (large, small, round, long, single eyelid, double eyelid)
- Distance between eyes
- Eye corners (upturned, downturned, straight)
- Eye color and expression
- Eyebrows (thick, thin, curved, straight, long, short)

4. NOSE (WEALTH AND FINANCE AREA):
- Nose shape (straight, curved, upturned, flat, high)
- Size of nose wings
- Shape of nose tip
- Nose position on face

5. MOUTH AND LIPS (CHILDREN AND LEGACY AREA):
- Mouth size (large, small, medium)
- Lip shape (thick, thin, curved, straight)
- Mouth corners (upturned, downturned, straight)
- Lip color

6. CHIN AND JAW (SERVANTS AND SUPPORT AREA):
- Chin shape (pointed, round, square, cleft)
- Chin prominence
- Jawline contour
- Lower jaw ratio compared to face

7. EARS (LONGEVITY AREA):
- Ear size
- Ear rim shape and thickness
- Ear position relative to eyes
- Ear color and brightness

8. CHEEKBONES AND TEMPLES:
- Cheekbone prominence
- Temple area shape
- Fullness of cheeks

9. OTHER CHARACTERISTICS:
- Moles, scars, marks (if any)
- Skin color
- Overall facial lines
- Facial expression

REQUIREMENT: Describe in detail and objectively all observable features, do NOT interpret meanings or make personality judgments. Please respond in Vietnamese."""
                    },
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}
                    },
                ],
            }],
        )

        result = response.choices[0].message.content.strip()
        print("Face analysis result:", result)
        return result