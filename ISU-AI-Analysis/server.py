# ============================================================================
# VANNA 2.0+ Pure - Không dùng Legacy Adapter
# Agent Memory tự động học từ successful queries
# ============================================================================

import os
from dotenv import load_dotenv
from vanna import Agent, AgentConfig
from vanna.core.registry import ToolRegistry
from vanna.core.user import User, UserResolver
from vanna.servers.fastapi import VannaFastAPIServer
from vanna.integrations.openai import OpenAILlmService
from vanna.tools import RunSqlTool, VisualizeDataTool
from vanna.tools.agent_memory import SaveQuestionToolArgsTool, SearchSavedCorrectToolUsesTool
from vanna.integrations.local.agent_memory import DemoAgentMemory
from vanna.integrations.postgres import PostgresRunner

# Load environment variables
load_dotenv()

# ============================================================================
# Simple Anonymous User Resolver
# ============================================================================
class AnonymousUserResolver(UserResolver):
    """Simple user resolver that returns an anonymous user for all requests"""
    async def resolve_user(self, request_context):
        return User(id="anonymous", email="anonymous@localhost", group_memberships=[])

# ============================================================================
# Configuration
# ============================================================================

# LLM Service
llm = OpenAILlmService(
    model="o3",
    api_key=os.getenv("OPENAI_API_KEY"),
)

# Database Runner
db_runner = PostgresRunner(
    host=os.getenv("POSTGRES_HOST", "localhost"),
    port=int(os.getenv("POSTGRES_PORT", "5433")),
    database=os.getenv("POSTGRES_DB", "vanna"),
    user=os.getenv("POSTGRES_USER", "postgres"),
    password=os.getenv("POSTGRES_PASSWORD", "secret")
)

# Agent Memory
agent_memory = DemoAgentMemory(max_items=1000)

# User Resolver
user_resolver = AnonymousUserResolver()

# ============================================================================
# Tool Registry
# ============================================================================
tools = ToolRegistry()

# Database query tool - cho phép tất cả users
tools.register_local_tool(
    RunSqlTool(sql_runner=db_runner), 
    access_groups=[]  # Empty = all users can access
)

# Visualization tool
tools.register_local_tool(
    VisualizeDataTool(), 
    access_groups=[]
)

# Memory tools - Agent tự động học từ successful queries
tools.register_local_tool(
    SaveQuestionToolArgsTool(), 
    access_groups=[]
)

tools.register_local_tool(
    SearchSavedCorrectToolUsesTool(), 
    access_groups=[]
)

# ============================================================================
# Create Agent với Custom System Prompt
# ============================================================================

# Custom system prompt để agent hiểu database schema
CUSTOM_SYSTEM_PROMPT = """
Bạn là trợ lý AI thông minh chuyên phân tích dữ liệu cho nền tảng xem bói.

# QUAN TRỌNG
Luôn luôn trả lời bằng tiếng việt
Dữ liệu tiền luôn theo VNĐ, ví dụ như 1207838.85 nghĩa là hơn 1 triệu 2, chứ không phải 1 tỷ 2

## DATABASE SCHEMA

### 1. Bảng knowledge_category
Lưu các loại hình dịch vụ xem bói.
```sql
CREATE TABLE knowledge_category (
    category_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);
```
Ví dụ: Tarot, Astrology (Cung Hoàng Đạo), Face Reading (Nhân Tướng Học), Five Elements (Ngũ Hành)

### 2. Bảng customer_potential
Thống kê tiềm năng khách hàng theo tháng.
```sql
CREATE TABLE customer_potential (
    customer_full_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL UNIQUE,
    customer_profile_description TEXT,
    customer_birth_date TIMESTAMP,
    customer_gender VARCHAR(10),
    month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INT NOT NULL,
    potential_point INT NOT NULL,
    potential_tier VARCHAR(20) NOT NULL,  -- CASUAL, STANDARD, PREMIUM, VIP
    ranking INT NOT NULL,
    total_booking_requests INT NOT NULL DEFAULT 0,
    total_spending NUMERIC(15, 2) NOT NULL DEFAULT 0,
    cancelled_by_customer INT NOT NULL DEFAULT 0,
    PRIMARY KEY (customer_email, month, year)
);
```

**Hệ thống phân hạng khách hàng:**
- CASUAL: 0-49 điểm (tương tác thấp)
- STANDARD: 50-69 điểm (tương tác trung bình)
- PREMIUM: 70-84 điểm (tương tác cao)
- VIP: 85-100 điểm (tương tác rất cao)

**Công thức potential_point:**
- 40% Loyalty (tần suất đặt lịch)
- 35% Value (số tiền chi tiêu)
- 25% Reliability (tỷ lệ hủy lịch thấp)

### 3. Bảng seer_performance
Thống kê hiệu suất thầy bói theo tháng.
```sql
CREATE TABLE seer_performance (
    seer_full_name VARCHAR(255) NOT NULL,
    seer_email VARCHAR(255) NOT NULL UNIQUE,
    seer_profile_description TEXT,
    seer_birth_date TIMESTAMP,
    seer_gender VARCHAR(10),
    seer_speciality TEXT[],  -- Mảng chuyên môn
    month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INT NOT NULL,
    performance_tier VARCHAR(20) NOT NULL,  -- APPRENTICE, PROFESSIONAL, EXPERT, MASTER
    performance_point INT NOT NULL,
    ranking INT NOT NULL,
    total_packages INT NOT NULL DEFAULT 0,
    total_rates INT NOT NULL DEFAULT 0,
    avg_rating NUMERIC(3, 2) NOT NULL DEFAULT 0,
    total_bookings INT NOT NULL DEFAULT 0,
    completed_bookings INT NOT NULL DEFAULT 0,
    cancelled_by_seer INT NOT NULL DEFAULT 0,
    total_revenue NUMERIC(15, 2) NOT NULL DEFAULT 0,
    bonus NUMERIC(15, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (seer_email, month, year)
);
```

**Hệ thống phân hạng thầy bói:**
- APPRENTICE: 0-49 điểm (mới vào nghề)
- PROFESSIONAL: 50-69 điểm (chuyên nghiệp)
- EXPERT: 70-84 điểm (chuyên gia)
- MASTER: 85-100 điểm (bậc thầy)

**Công thức performance_point:**
- 30% Engagement (gói dịch vụ và booking)
- 25% Rating (mức độ hài lòng)
- 20% Completion rate (độ tin cậy)
- 15% Low cancellation (tính chuyên nghiệp)
- 10% Earning (tạo doanh thu)

## HƯỚNG DẪN TẠO SQL

1. **Luôn dùng PostgreSQL syntax**
2. **Cast kiểu dữ liệu đúng:** `cancelled_by_customer::FLOAT`, `::NUMERIC`
3. **Với mảng:** Dùng `ANY(seer_speciality)` hoặc `unnest(seer_speciality)`
4. **Tính tuổi:** `EXTRACT(YEAR FROM AGE(customer_birth_date))`
5. **Xử lý chia cho 0:** `CASE WHEN total > 0 THEN ... ELSE 0 END`
6. **Tháng hiện tại:** month = 11 AND year = 2025 (tháng 11/2025)

## EXAMPLE QUERIES

### Khách hàng VIP tháng 11/2025
```sql
SELECT customer_full_name, customer_email, potential_point, total_spending, ranking 
FROM customer_potential 
WHERE potential_tier = 'VIP' AND month = 11 AND year = 2025 
ORDER BY ranking;
```

### Thầy bói MASTER tháng 11/2025
```sql
SELECT seer_full_name, seer_email, performance_point, total_revenue, avg_rating, ranking 
FROM seer_performance 
WHERE performance_tier = 'MASTER' AND month = 11 AND year = 2025 
ORDER BY ranking;
```

### Tìm thầy bói chuyên Tarot
```sql
SELECT seer_full_name, seer_email, seer_speciality, avg_rating, total_revenue 
FROM seer_performance 
WHERE month = 11 AND year = 2025 AND 'Tarot' = ANY(seer_speciality) 
ORDER BY avg_rating DESC;
```

### Doanh thu theo chuyên môn
```sql
SELECT unnest(seer_speciality) as chuyen_mon, 
       AVG(total_revenue) as doanh_thu_tb, 
       COUNT(*) as so_thay 
FROM seer_performance 
WHERE month = 11 AND year = 2025 
GROUP BY chuyen_mon 
ORDER BY doanh_thu_tb DESC;
```

Nhiệm vụ của bạn:
1. Hiểu câu hỏi người dùng
2. Tạo SQL query chính xác dựa trên schema trên
3. Chạy query và trả về kết quả
4. Nếu cần, tạo visualization (biểu đồ)
5. Nếu có câu hỏi liên quan đến "Khách hàng", hãy tìm trong bảng customer_potentials
6. Nếu có câu hỏi liên quan đến "Thầy bói/Nhân viên", hãy tìm trong bảng seer_performances
"""

# Create agent with custom system prompt
agent = Agent(
    llm_service=llm,
    tool_registry=tools,
    user_resolver=user_resolver,
    agent_memory=agent_memory,
    config=AgentConfig(
        system_prompt=CUSTOM_SYSTEM_PROMPT,
        max_tool_iterations=100,  # Tăng từ 10 lên 100 (hoặc 999 nếu bạn thực sự giàu 😄)
        temperature=0.1  # Giảm temperature để responses chính xác hơn
    )
)

# ============================================================================
# Pre-populate Agent Memory với training data
# ============================================================================

# Helper function to add training data
async def populate_memory():
    """Pre-populate agent memory with common query patterns"""
    from vanna.core.tool import ToolContext
    from vanna.core.user import User
    
    # Create a mock context for training
    mock_user = User(id="system", email="system@vanna.ai", group_memberships=[])
    mock_context = ToolContext(
        user=mock_user,
        agent_memory=agent_memory,
        conversation_id="training",
        message_id="training",
        request_id="training-request"
    )
    
    # Training data - Common question-SQL pairs
    training_data = [
        {
            "question": "Có bao nhiêu khách hàng trong database?",
            "sql": "SELECT COUNT(DISTINCT customer_email) FROM customer_potential;"
        },
        {
            "question": "Có bao nhiêu thầy bói trong database?",
            "sql": "SELECT COUNT(DISTINCT seer_email) FROM seer_performance;"
        },
        {
            "question": "Có bao nhiêu khách hàng VIP trong tháng 11/2025?",
            "sql": "SELECT COUNT(*) FROM customer_potential WHERE potential_tier = 'VIP' AND month = 11 AND year = 2025;"
        },
        {
            "question": "Hiển thị tất cả khách hàng VIP trong tháng 11/2025",
            "sql": "SELECT customer_full_name, customer_email, potential_point, total_spending, ranking FROM customer_potential WHERE potential_tier = 'VIP' AND month = 11 AND year = 2025 ORDER BY ranking;"
        },
        {
            "question": "Top 10 khách hàng chi tiêu nhiều nhất tháng 11/2025?",
            "sql": "SELECT customer_full_name, customer_email, total_spending, potential_tier, ranking FROM customer_potential WHERE month = 11 AND year = 2025 ORDER BY total_spending DESC LIMIT 10;"
        },
        {
            "question": "Có bao nhiêu thầy bói hạng MASTER trong tháng 11/2025?",
            "sql": "SELECT COUNT(*) FROM seer_performance WHERE performance_tier = 'MASTER' AND month = 11 AND year = 2025;"
        },
        {
            "question": "Top 10 thầy bói có doanh thu cao nhất tháng 11/2025?",
            "sql": "SELECT seer_full_name, seer_email, total_revenue, performance_tier, ranking FROM seer_performance WHERE month = 11 AND year = 2025 ORDER BY total_revenue DESC LIMIT 10;"
        },
        {
            "question": "Thầy bói nào chuyên về Tarot trong tháng 11/2025?",
            "sql": "SELECT seer_full_name, seer_email, seer_speciality, avg_rating, total_revenue FROM seer_performance WHERE month = 11 AND year = 2025 AND 'Tarot' = ANY(seer_speciality) ORDER BY avg_rating DESC;"
        },
        {
            "question": "Phân bố các hạng khách hàng trong tháng 11/2025?",
            "sql": "SELECT potential_tier, COUNT(*) as so_luong FROM customer_potential WHERE month = 11 AND year = 2025 GROUP BY potential_tier ORDER BY potential_tier;"
        },
        {
            "question": "Tổng doanh thu toàn nền tảng tháng 11/2025?",
            "sql": "SELECT SUM(total_revenue) as tong_doanh_thu FROM seer_performance WHERE month = 11 AND year = 2025;"
        }
    ]
    
    print("📚 Đang pre-populate agent memory...")
    for item in training_data:
        await agent_memory.save_tool_usage(
            question=item["question"],
            tool_name="run_sql",
            args={"sql": item["sql"]},
            context=mock_context,
            success=True,
            metadata={"source": "pre_training"}
        )
    print(f"✅ Đã thêm {len(training_data)} patterns vào memory!")

# ============================================================================
# Server Setup
# ============================================================================
server = VannaFastAPIServer(agent)

if __name__ == "__main__":
    print("🚀 Starting Vanna 2.0+ Pure Server...")
    print("📍 Access at: http://localhost:8000")
    print("\n" + "="*60)
    print("💡 CÁCH SỬ DỤNG:")
    print("="*60)
    print("1. Mở browser tại http://localhost:8000")
    print("2. Đặt câu hỏi tiếng Việt về database")
    print("3. Agent sẽ tự động:")
    print("   - Tạo SQL query")
    print("   - Chạy query")
    print("   - Hiển thị kết quả")
    print("   - Lưu vào memory để học")
    print("\n📊 Database Schema đã được nhúng vào system prompt!")
    print("🧠 Agent Memory sẽ tự động học từ các query thành công!\n")
    
    # Pre-populate memory before starting server
    import asyncio
    asyncio.run(populate_memory())
    
    server.run()
