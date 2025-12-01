FROM pytorch/pytorch:2.0.1-cuda11.7-cudnn8-runtime

# Thiết lập thư mục làm việc trong container
WORKDIR /app

# Cài đặt các dependencies hệ thống cần thiết
# build-essential: để compile các package Python có C extensions
# curl: để health check
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    gcc \
    g++ \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir --upgrade pip

COPY requirements-ai.txt .

RUN pip install --no-cache-dir -r requirements-ai.txt

COPY requirements-core.txt .

RUN pip install --no-cache-dir -r requirements-core.txt


COPY src/ ./src/
COPY data/ ./data/

# Tạo các thư mục cần thiết cho ứng dụng
# rag_storage: lưu trữ indexes và embeddings
# logs: lưu trữ log files (nếu cần)
RUN mkdir -p /app/rag_storage /app/logs && chmod -R 777 /app/data

EXPOSE 8000

# Thiết lập chỉ các biến môi trường cần thiết cho runtime Python
# (các biến khác sẽ được inject qua docker-compose hoặc runtime)
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/src

# Health check để kiểm tra container có hoạt động không
# Kiểm tra mỗi 30s, timeout 10s, thử lại 3 lần, chờ 40s ban đầu
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Chạy ứng dụng chính thông qua main.py (entry point mới)
CMD ["python", "src/main.py"]
