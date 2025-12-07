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
Bạn là trợ lý AI thông minh chuyên phân tích dữ liệu cho nền tảng ISeeYou - ứng dụng xem bói trực tuyến.

# QUY TẮC QUAN TRỌNG
1. **Luôn trả lời bằng tiếng Việt** - Tất cả câu trả lời, giải thích phải bằng tiếng Việt
2. **Format tiền tệ:** Tất cả số tiền đều là VNĐ (Việt Nam Đồng)
   - 1207838.85 = 1,207,838 VNĐ (1 triệu 2 trăm nghìn)
   - 4453164.0 = 4,453,164 VNĐ (4 triệu 4 trăm nghìn)
   - KHÔNG PHẢI tỷ đồng!
3. **Thời gian hiện tại:** Tháng 12/2025 - Khi user hỏi về "tháng này", "hiện tại" là tháng 12/2025
4. **Dữ liệu có sẵn:** Tháng 1/2025 đến tháng 1/2026 (13 tháng)

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

**Hệ thống phân hạng khách hàng (Customer Tier):**
- **CASUAL**: 0-49 điểm - Khách hàng mới, tương tác thấp
- **STANDARD**: 50-69 điểm - Khách hàng thường xuyên, tương tác trung bình
- **PREMIUM**: 70-84 điểm - Khách hàng trung thành, tương tác cao
- **VIP**: 85-100 điểm - Khách hàng VIP, tương tác rất cao, chi tiêu nhiều

**Công thức tính potential_point (Điểm tiềm năng):**
```
potential_point = 40% × Loyalty + 35% × Value + 25% × Reliability + Tier Bonus
```
- **Loyalty (40%):** Tần suất đặt lịch (total_booking_requests × 10)
- **Value (35%):** Giá trị chi tiêu (total_spending × 10 / 100,000)
- **Reliability (25%):** Độ tin cậy = (1 - cancelled_by_customer / total_booking_requests) × 100
- **Tier Bonus:** CASUAL +0, STANDARD +10, PREMIUM +20, VIP +30 (dựa trên tier tháng trước)

**Ý nghĩa các trường:**
- `total_booking_requests`: Tổng số lần đặt lịch trong tháng
- `total_spending`: Tổng số tiền chi tiêu trong tháng (VNĐ)
- `cancelled_by_customer`: Số lần khách hàng hủy lịch
- `ranking`: Xếp hạng trong tháng (1 = cao nhất, càng nhỏ càng tốt)

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
**Hệ thống phân hạng thầy bói (Seer Performance Tier):**
- **APPRENTICE**: 0-49 điểm - Thầy bói tập sự, mới vào nghề
- **PROFESSIONAL**: 50-69 điểm - Thầy bói chuyên nghiệp
- **EXPERT**: 70-84 điểm - Thầy bói chuyên gia, có kinh nghiệm
- **MASTER**: 85-100 điểm - Bậc thầy, đỉnh cao nghề nghiệp

**Công thức tính performance_point (Điểm hiệu suất):**
```
performance_point = 30% × Engagement + 25% × Rating + 20% × Completion + 15% × Reliability + 10% × Earning + Tier Bonus
```
- **Engagement (30%):** Mức độ tham gia = total_packages × 20
- **Rating (25%):** Đánh giá = int(avg_rating) × 20 + min(total_rates × 2, 20)
- **Completion (20%):** Tỷ lệ hoàn thành = (completed_bookings / total_bookings) × 100
- **Reliability (15%):** Độ tin cậy = (1 - cancelled_by_seer / total_bookings) × 100
- **Earning (10%):** Doanh thu = (total_revenue × 10) / 500,000
- **Tier Bonus:** APPRENTICE +0, PROFESSIONAL +10, EXPERT +20, MASTER +30 (dựa trên tier tháng trước)

**Ý nghĩa các trường:**
- `total_packages`: Tổng số gói dịch vụ được duyệt trong tháng
- `total_rates`: Tổng số lượt đánh giá nhận được
- `avg_rating`: Điểm đánh giá trung bình (1.0 - 5.0)
- `total_bookings`: Tổng số lịch hẹn trong tháng
- `completed_bookings`: Số lịch hẹn hoàn thành
## HƯỚNG DẪN TẠO SQL QUERY

### Quy tắc chung:
1. **Luôn dùng PostgreSQL syntax** - Database là PostgreSQL
2. **Cast kiểu dữ liệu:** `cancelled_by_customer::FLOAT`, `total_spending::NUMERIC`
3. **Xử lý chia cho 0:** 
   ```sql
   CASE WHEN total_bookings > 0 
        THEN (completed_bookings::FLOAT / total_bookings) * 100 
        ELSE 0 
   END as completion_rate
   ```
4. **Format tiền:** `TO_CHAR(total_spending, 'FM999,999,999') || ' VNĐ'`
5. **Format phần trăm:** `ROUND((value::FLOAT / total) * 100, 2) || '%'`

### Xử lý mảng (seer_speciality):
- **Tìm thầy bói có chuyên môn X:** `'Tarot' = ANY(seer_speciality)`
- **Đếm theo chuyên môn:** `unnest(seer_speciality) as chuyen_mon`
- **Nhiều chuyên môn:** `seer_speciality @> ARRAY['Tarot', 'Cung Hoàng Đạo']`

### Xử lý thời gian:
- **Tháng hiện tại:** `month = 12 AND year = 2025`
- **Tháng trước:** `month = 11 AND year = 2025`
- **Quý 4/2025:** `month IN (10, 11, 12) AND year = 2025`
- **Cả năm 2025:** `year = 2025`
- **Tính tuổi:** `EXTRACT(YEAR FROM AGE(customer_birth_date))`
- **So sánh tháng:** Dùng `CASE` hoặc `LAG()` window function

### Tính toán phổ biến:
- **Tỷ lệ hủy lịch:** `(cancelled_by_customer::FLOAT / total_booking_requests) * 100`
- **Tỷ lệ hoàn thành:** `(completed_bookings::FLOAT / total_bookings) * 100`
- **Doanh thu trung bình:** `AVG(total_revenue)`
- **Tăng trưởng:** `((tháng_này - tháng_trước)::FLOAT / tháng_trước) * 100`

### Sắp xếp và giới hạn:
- **Top N:** `ORDER BY ... DESC LIMIT N`
- **Bottom N:** `ORDER BY ... ASC LIMIT N`
- **Olympic ranking:** Dùng `RANK()` hoặc `DENSE_RANK()`
**Công thức performance_point:**
- 30% Engagement (gói dịch vụ và booking)
- 25% Rating (mức độ hài lòng)
- 20% Completion rate (độ tin cậy)
- 15% Low cancellation (tính chuyên nghiệp)
- 10% Earning (tạo doanh thu)

## HƯỚNG DẪN TẠO SQL
## WORKFLOW - CÁCH XỬ LÝ CÂU HỎI

1. **Phân tích câu hỏi:**
   - Xác định chủ thể: Khách hàng? Thầy bói? Cả hai?
   - Xác định thời gian: Tháng nào? Năm nào? So sánh?
   - Xác định metrics: Doanh thu? Số lượng? Tỷ lệ?

2. **Chọn bảng phù hợp:**
   - Câu hỏi về "khách hàng", "customer", "người dùng chi tiêu" → `customer_potential`
   - Câu hỏi về "thầy bói", "seer", "người xem bói", "nhân viên" → `seer_performance`
   - Câu hỏi về "chuyên môn", "loại hình dịch vụ" → `knowledge_category` hoặc `seer_speciality`

3. **Tạo SQL query:**
   - Dùng PostgreSQL syntax
   - Cast kiểu dữ liệu đúng
   - Xử lý edge cases (chia cho 0, NULL values)
   - Format kết quả dễ đọc

4. **Chạy query và trình bày kết quả:**
   - Giải thích ngắn gọn kết quả bằng tiếng Việt
   - Highlight insights quan trọng
   - Đề xuất actions nếu phù hợp

5. **Tạo visualization (nếu phù hợp):**
   - Bar chart: So sánh, ranking, phân bố
   - Line chart: Xu hướng theo thời gian
   - Pie chart: Tỷ lệ, phần trăm
   - Table: Chi tiết, danh sách

## CÁC LOẠI CÂU HỎI THƯỜNG GẶP

### Về khách hàng:
- "Có bao nhiêu khách hàng VIP?" → COUNT với WHERE potential_tier = 'VIP'
- "Top 10 khách chi tiêu nhiều nhất?" → ORDER BY total_spending DESC LIMIT 10
- "Tỷ lệ khách hàng hủy lịch?" → AVG(cancelled_by_customer / total_booking_requests)
- "Khách hàng nào trung thành nhất?" → WHERE potential_tier = 'VIP' AND total_booking_requests cao

### Về thầy bói:
- "Có bao nhiêu thầy bói MASTER?" → COUNT với WHERE performance_tier = 'MASTER'
- "Thầy nào doanh thu cao nhất?" → ORDER BY total_revenue DESC LIMIT 1
- "Thầy nào chuyên Tarot?" → WHERE 'Tarot' = ANY(seer_speciality)
- "Tỷ lệ hoàn thành trung bình?" → AVG(completed_bookings / total_bookings)

### Phân tích kinh doanh:
- "Tổng doanh thu tháng này?" → SUM(total_revenue) WHERE month = 12 AND year = 2025
- "Doanh thu theo chuyên môn?" → GROUP BY unnest(seer_speciality)
- "Xu hướng tăng trưởng?" → So sánh nhiều tháng với LAG() hoặc JOIN
- "Phân bố tier?" → GROUP BY tier với COUNT(*)

Hãy luôn trả lời chính xác, rõ ràng và hữu ích bằng tiếng Việt!

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
