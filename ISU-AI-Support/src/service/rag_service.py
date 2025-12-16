"""
Service Layer - Xử lý logic nghiệp vụ chính của hệ thống RAG
Chứa tất cả logic xử lý RAG, khởi tạo và tìm kiếm
"""

import os
import json
from typing import Optional, List
from lightrag import LightRAG, QueryParam
from ingestion import initialize_rag, index_file
from util.text_search_util import TextSearchUtil, LogUtil
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
        LogUtil.log_info("[PALM ANALYSIS] Starting gpt-4o-2024-11-20 vision analysis for palm image", "SERVICE")
        base64_image = ImageUtil.encode_image_bytes(image_bytes)
        client = self._get_openai_client()
        
        LogUtil.log_info("[PALM ANALYSIS] Sending request to gpt-4o-2024-11-20 with image...", "SERVICE")
        response = client.chat.completions.create(
            model="gpt-4o-2024-11-20",
            max_completion_tokens=4000,
            messages=[{
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": """BẠN LÀ CHUYÊN GIA PHÂN TÍCH CẤU TRÚC LÒNG BÀN TAY VÀ CÁC ĐƯỜNG NẾP (Hand Structure and Palm Lines Analysis).

Nhiệm vụ: Mô tả chi tiết CẤU TRÚC, ĐƯỜNG NẾP và ĐẶC ĐIỂM quan sát được trên lòng bàn tay. Sử dụng THUẬT NGỮ CHUYÊN MÔN trong phân tích chỉ tay (palmistry terminology) khi mô tả:

**1. CÁC ĐƯỜNG CHỈ TAY CHÍNH (Major Palm Lines):**
- **Đường Đời / Life Line**: Đường cong bắt đầu giữa ngón cái và ngón trỏ, bao quanh gò Kim Tinh (Mount of Venus), chạy về cổ tay. Mô tả: độ dài (dài/trung bình/ngắn), độ sâu (sâu/nông), độ rõ nét, hình dạng (cong/thẳng), điểm bắt đầu và kết thúc
- **Đường Trí Tuệ / Head Line**: Đường ngang bắt đầu gần Life Line, chạy ngang qua lòng bàn tay. Mô tả: độ dài, độ sâu, hướng (thẳng/cong xuống), điểm kết thúc
- **Đường Tình Cảm / Heart Line**: Đường ngang trên cùng dưới các ngón tay. Mô tả: độ dài, độ sâu, hướng chạy, điểm bắt đầu (dưới ngón út) và kết thúc (ngón trỏ/giữa)
- **Đường Vận Mệnh / Fate Line**: Đường thẳng đứng (nếu có) từ cổ tay lên giữa lòng bàn tay. Mô tả: có hay không, độ rõ, độ dài
- **Các đường phụ**: Marriage Lines (dưới ngón út), Sun Line, Mercury Line (nếu có)

**2. HÌNH DẠNG VÀ TỶ LỆ BÀN TAY:**
- Hình dạng tổng thể của bàn tay: vuông, chữ nhật, hình thang, dài, ngắn
- Tỷ lệ chiều dài/rộng lòng bàn tay
- Chiều dài các ngón tay so với lòng bàn tay
- Hình dạng đầu các ngón tay: vuông, tròn, nhọn, hình spatula

**3. CÁC GÒ TRÊN BÀN TAY (Mounts):**
- **Gò Kim Tinh / Mount of Venus**: Gò ở gốc ngón cái (thenar). Mô tả: độ đầy (đầy/vừa/lép), độ nổi, kích thước
- **Gò Mộc Tinh / Mount of Jupiter**: Gò dưới ngón trỏ. Mô tả: nổi rõ/vừa/phẳng
- **Gò Thổ Tinh / Mount of Saturn**: Gò dưới ngón giữa. Mô tả: nổi rõ/vừa/phẳng
- **Gò Thái Dương / Mount of Apollo/Sun**: Gò dưới ngón áp út. Mô tả: nổi rõ/vừa/phẳng
- **Gò Thủy Tinh / Mount of Mercury**: Gò dưới ngón út. Mô tả: nổi rõ/vừa/phẳng
- **Gò Thái Âm / Mount of Luna/Moon**: Gò bên cạnh lòng bàn tay (hypothenar). Mô tả: độ đầy, kích thước
- **Gò Hỏa Tinh / Mount of Mars**: Nếu quan sát được (giữa Jupiter-Venus hoặc giữa Mercury-Moon)

**4. DẤU HIỆU ĐẶC BIỆT (Special Markings):**
- **Đoạn đứt quãng / Breaks**: Các đường chính có đoạn gián đoạn không? Vị trí cụ thể
- **Đường đôi / Sister Lines**: Có đường song song với đường chính không?
- **Nhánh / Branches**: Có nhánh rẽ từ Life Line, Head Line hay Heart Line không? Hướng lên/xuống
- **Dấu sao / Stars**: Hình sao tạo bởi giao điểm các nếp nhỏ. Vị trí
- **Hòn đảo / Islands**: Hình oval/đảo trên đường chính. Vị trí
- **Dấu chấm / Dots**: Các chấm đen/nâu trên đường hoặc gò
- **Màu sắc da**: hồng hào/trắng/vàng/ngăm
- **Kết cấu da**: mịn mượt/thô ráp/nhăn nheo
- **Nốt ruồi / Moles, Sẹo / Scars**: Vị trí cụ thể và kích thước

**5. THÔNG TIN BỔ SUNG:**
- Bàn tay trái hay phải
- Độ dày/mỏng của bàn tay (dựa vào quan sát)
- Các đặc điểm nổi bật đặc biệt khác

**YÊU CẦU QUAN TRỌNG:** 
- SỬ DỤNG thuật ngữ song ngữ (Việt/English) như: "Đường Đời/Life Line", "Gò Kim Tinh/Mount of Venus"
- CHỈ MÔ TẢ những gì QUAN SÁT ĐƯỢC - KHÔNG giải thích ý nghĩa hay suy luận
- Mô tả CHI TIẾT, CỤ THỂ từng đặc điểm (độ dài, độ sâu, hình dạng, vị trí)
- Trả lời HOÀN TOÀN bằng TIẾNG VIỆT"""
                    },
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}
                    },
                ],
            }],
        )
        
        palm_analysis = response.choices[0].message.content.strip()
        LogUtil.log_info("[PALM ANALYSIS] gpt-4o-2024-11-20 analysis completed", "SERVICE")
        LogUtil.log_info(f"[PALM ANALYSIS] Result:\n{palm_analysis}", "SERVICE")
        
        return palm_analysis

    def analyze_face_details(self, image_bytes: bytes) -> str:
        """
        Phân tích nhân tướng học từ ảnh khuôn mặt
        
        Args:
            image_bytes: Dữ liệu ảnh dưới dạng bytes
            
        Returns:
            str: Kết quả phân tích nhân tướng học chi tiết
        """
        LogUtil.log_info("[FACE ANALYSIS] Starting gpt-4o-2024-11-20 vision analysis for face image", "SERVICE")
        base64_image = ImageUtil.encode_image_bytes(image_bytes)
        client = self._get_openai_client()
        
        LogUtil.log_info("[FACE ANALYSIS] Sending request to gpt-4o-2024-11-20 with image...", "SERVICE")
        response = client.chat.completions.create(
            model="gpt-4o-2024-11-20",
            max_completion_tokens=4000,
            messages=[{
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": """PHÂN TÍCH CHI TIẾT NHÂN TƯỚNG HỌC:

Hãy quan sát kỹ và mô tả đầy đủ các đặc điểm khuôn mặt theo nhân tướng học:

1. HÌNH DẠNG KHUÔN MÁT:
- Hình dạng tổng thể (tròn, vuông, ô van, tam giác, kim cương, chữ nhật)
- Tỷ lệ dài/rộng
- Đường viền và độ cân đối

2. TRÁN (VÙNG TÀI LỘC VÀ SỰ NGHIỆP):
- Độ cao, độ rộng của trán
- Hình dạng (tròn, vuông, dốc)
- Các nếp nhăn và đường ngang trên trán
- Vị trí và hình dạng đường tóc (chữ M, tròn, vuông...)

3. MẮT (VÙNG HÔN NHÂN VÀ CÁC MỐI QUAN HỆ):
- Hình dạng mắt (to, nhỏ, tròn, dài, mí đơn, mí đôi)
- Khoảng cách giữa hai mắt
- Góc mắt (hếch lên, cụp xuống, thẳng)
- Màu mắt và ánh nhìn
- Lông mày (dày, mỏng, cong, thẳng, dài, ngắn, hướng lên/xuống)

4. MŨI (VÙNG TÀI CHÍNH VÀ TÀI SẢN):
- Hình dạng mũi (thẳng, cong, hếch, tẹt, cao)
- Kích thước cánh mũi
- Hình dạng đầu mũi (tròn, nhọn, vuông)
- Vị trí mũi trên khuôn mặt

5. MIỆNG VÀ MÔI (VÙNG CON CÁI VÀ DI SẢN):
- Kích thước miệng (lớn, nhỏ, vừa)
- Hình dạng môi (dày, mỏng, cong, thẳng)
- Góc miệng (hếch lên, cụp xuống, thẳng)
- Màu sắc môi
- Răng (nếu nhìn thấy): chỉnh tề, thưa, khấp khểnh

6. CẰM VÀ HÀM (VÙNG TÁ NHÂN VÀ HẬU VẬN):
- Hình dạng cằm (nhọn, tròn, vuông, chẻ)
- Độ nhô ra của cằm
- Đường viền xương hàm
- Tỷ lệ hàm dưới so với khuôn mặt

7. TAI (VÙNG TRƯỜNG THỌ):
- Kích thước tai
- Hình dạng và độ dày vành tai
- Vị trí tai so với mắt (cao, thấp, ngang)
- Màu sắc và độ sáng của tai
- Hình dạng dái tai (dày, mỏng, to, nhỏ)

8. GÒ MÁ VÀ THÁI DƯƠNG:
- Độ nổi của gò má
- Hình dạng vùng thái dương
- Độ đầy của má

9. CÁC ĐẶC ĐIỂM KHÁC:
- Nốt ruồi, sẹo, dấu (nếu có) - vị trí cụ thể
- Màu da (trắng, ngăm, vàng, hồng...)
- Các đường nét tổng thể trên khuôn mặt
- Biểu cảm khuôn mặt
- 12 cung vị trên khuôn mặt (nếu quan sát được)

YÊU CẦU: Mô tả chi tiết và khách quan tất cả các đặc điểm quan sát được, KHÔNG giải thích ý nghĩa hay đưa ra đánh giá về tính cách. Vui lòng trả lời HOÀN TOÀN BẰNG TIẾNG VIỆT."""
                    },
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}
                    },
                ],
            }],
        )

        face_analysis = response.choices[0].message.content.strip()
        LogUtil.log_info("[FACE ANALYSIS] gpt-4o-2024-11-20 analysis completed", "SERVICE")
        LogUtil.log_info(f"[FACE ANALYSIS] Result:\n{face_analysis}", "SERVICE")
        
        return face_analysis