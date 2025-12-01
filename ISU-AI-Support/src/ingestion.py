import os

import nest_asyncio
from lightrag.kg.shared_storage import initialize_share_data, initialize_pipeline_status

from dotenv import load_dotenv
load_dotenv()

from gateway.openai_gateway import o3_complete, openai_embed

from lightrag import LightRAG

# cho phép chạy vòng lặp lồng nhau (trong Jupyter hoặc môi trường đã có vòng lặp)
nest_asyncio.apply()

# Hàm khởi tạo LightRAG
# Tạm thời lưu mặc định bên trong rag_storage. TODO: Lưu trong Postgre
async def initialize_rag(working_dir: str = "./rag_storage") -> LightRAG:
    # Bước 1: Khởi tạo LightRAG với cấu hình cơ bản
    rag = LightRAG(
        working_dir=working_dir, # NonVector DB
        embedding_func=openai_embed,
        llm_model_func=o3_complete,
        graph_storage="Neo4JStorage",
        vector_storage="FaissVectorDBStorage",   # đổi từ Qdrant
        vector_db_storage_cls_kwargs={
            "cosine_better_than_threshold": 0.3
        },
        chunk_token_size=800,
        chunk_overlap_token_size=200,
        default_embedding_timeout=36000, # 10 hours, tránh timeout khi embedding file lớn 
        default_llm_timeout=600, # 10 minutes, tránh timeout khi trả lời câu hỏi phức tạp
    )

    await rag.initialize_storages()

    # ensure shared dicts exist
    initialize_share_data()
    await initialize_pipeline_status()

    return rag

#  Hàm đánh chỉ mục dữ liệu
async def index_file(rag: LightRAG, file_path: str) -> None:
    # Bước 1: Kiểm tra file có tồn tại không
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Data file not found: {file_path}")

    # Bước 2: Đọc nội dung file
    with open(file_path, 'r', encoding='utf-8') as f:
        text = f.read()

    # Bước 3: Truyền các đoạn văn bản vào kho vector và đồ thị của LightRAG
    await rag.ainsert(input=text, file_paths=[file_path])