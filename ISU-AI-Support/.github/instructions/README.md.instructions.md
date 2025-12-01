# Cấu trúc thư mục 
## This is old version
- data/: nơi lưu giữ database reference cho lightRag 
    - data.txt: nơi lưu giữ database cho lý thuyết 
    - data.json: nơi lưu trữ database cho những lý thuyết có quan hệ 
- docker/
    - Dockerfile: file docker 
    - docker-compose.yml: file docker compose
- src/
    - controller/
    - dto/ 
    - service/
    - util/
    - ingestion.py
    - main.py
- .env
- Makefile: mọi lệnh chạy trên đây
- docker_log.log: nơi lưu giữ log của docker, tôi sẽ cập nhật mới nhất ở đây