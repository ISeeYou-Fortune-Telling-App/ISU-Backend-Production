# Vanna AI với PostgreSQL

Ứng dụng Vanna AI để query database PostgreSQL bằng ngôn ngữ tự nhiên.

## 📋 Yêu cầu

- Docker và Docker Compose
- OpenAI API Key

## 🚀 Cài đặt và chạy

### 1. Cấu hình môi trường

Copy file `.env.example` thành `.env`:

```bash
cp .env.example .env
```

Chỉnh sửa file `.env` và thêm OpenAI API Key của bạn:

```env
OPENAI_API_KEY=your_actual_api_key_here
```

### 2. Chạy với Docker Compose

```bash
# Build và khởi động tất cả services
docker-compose up --build

# Hoặc chạy ở background
docker-compose up -d --build
```

### 3. Truy cập ứng dụng

- Vanna Web UI: http://localhost:8000
- PostgreSQL: localhost:5433

## 📁 Cấu trúc project

```
.
├── .env                    # Environment variables (không commit lên git)
├── .env.example            # Template cho environment variables
├── docker-compose.yaml     # Docker compose configuration
├── Dockerfile              # Dockerfile cho Vanna server
├── Dockerfile.postgres     # Dockerfile cho PostgreSQL với data
├── server.py               # Vanna server application
├── postgres_runner.py      # PostgreSQL runner
├── requirements.txt        # Python dependencies
└── data_import.sql         # SQL data import
```

## 🛠 Các lệnh hữu ích

### Xem logs

```bash
# Tất cả services
docker-compose logs -f

# Chỉ Vanna server
docker-compose logs -f vanna

# Chỉ PostgreSQL
docker-compose logs -f postgres
```

### Dừng services

```bash
docker-compose down

# Xóa cả volumes (xóa data)
docker-compose down -v
```

### Restart services

```bash
docker-compose restart

# Restart một service cụ thể
docker-compose restart vanna
```

### Kiểm tra trạng thái

```bash
docker-compose ps
```

### Truy cập PostgreSQL

```bash
# Từ máy local
docker exec -it postgres-vanna psql -U postgres -d vanna

# Hoặc sử dụng bất kỳ PostgreSQL client nào
# Host: localhost
# Port: 5433
# Database: vanna
# User: postgres
# Password: secret
```

## 🔧 Development

### Chạy local (không dùng Docker)

1. Cài đặt dependencies:

```bash
pip install -r requirements.txt
```

2. Đảm bảo PostgreSQL đang chạy (có thể dùng Docker):

```bash
docker-compose up postgres -d
```

3. Chạy server:

```bash
python server.py
```

### Cập nhật code

Nếu thay đổi code, rebuild container:

```bash
docker-compose up --build
```

## 📝 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key | (required) |
| `POSTGRES_HOST` | PostgreSQL host | postgres |
| `POSTGRES_PORT` | PostgreSQL port | 5432 |
| `POSTGRES_DB` | Database name | vanna |
| `POSTGRES_USER` | Database user | postgres |
| `POSTGRES_PASSWORD` | Database password | secret |
| `VANNA_PORT` | Vanna server port | 8000 |
| `TZ` | Timezone | Asia/Ho_Chi_Minh |

## ⚠️ Lưu ý bảo mật

- **Không commit file `.env`** lên git repository
- File `.env` đã được thêm vào `.gitignore`
- Sử dụng `.env.example` để chia sẻ template
- Thay đổi password PostgreSQL trong production

## 🐛 Troubleshooting

### Port đã được sử dụng

Nếu port 5433 hoặc 8000 đã được sử dụng, thay đổi trong file `.env`:

```env
VANNA_PORT=8001  # Thay đổi port của Vanna
```

Hoặc thay đổi port mapping trong `docker-compose.yaml`.

### OpenAI API Error

Nếu gặp lỗi streaming, đảm bảo `stream=False` đã được set trong `server.py`.

### Database connection error

Đợi PostgreSQL khởi động hoàn toàn (health check sẽ tự động kiểm tra).

## 📚 Tài liệu tham khảo

- [Vanna AI Documentation](https://vanna.ai/docs/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
