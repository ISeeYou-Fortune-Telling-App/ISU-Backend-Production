# ISeeYou Fortune Telling Platform - Backend Services

A comprehensive microservices-based backend system for the **ISeeYou Fortune Telling Platform** - a mobile application connecting users with professional fortune tellers (seers) for personalized consultations including Tarot reading, Astrology, Palmistry, and more.

Our mission is to **build a nationwide fortune telling community**, connecting spiritual seekers with verified professional seers across the country. ISeeYou creates a trusted ecosystem where users can easily discover, connect, and consult with fortune tellers regardless of geographical boundaries - bringing ancient wisdom into the digital age.

## About This Project

This backend powers the ISeeYou web and Android app and with a robust, scalable architecture built on:

- **Spring Boot 3.x** - Core microservices framework for Java-based services
- **Spring Cloud Gateway** - API Gateway for routing, load balancing, and security
- **FastAPI (Python)** - AI-powered services for intelligent consulting and data analysis
- **Docker & Docker Compose** - Containerized deployment for consistency across environments
- **Message-Driven Architecture** - RabbitMQ for async communication between services

### Key Features

- **Real-time Chat and Video call** - Socket.IO powered live communication between users and seers
- **AI-Powered Consulting** - LightRAG-based knowledge system for fortune telling assistance
- **Smart Analytics** - Vanna AI for natural language data analysis and reporting
- **Push Notifications** - Firebase Cloud Messaging integration
- **Multi-Database Strategy** - PostgreSQL for relational data, MongoDB for documents, Neo4j for knowledge graphs, Redis for caching and jwt refresh tokens

## Architecture Overview

```
                                    +------------------+
                                    |   API Gateway    |
                                    |   (Port 8080)    |
                                    +--------+---------+
                                             |
              +------------------------------+------------------------------+
              |                              |                              |
    +---------v---------+         +----------v----------+        +----------v----------+
    |   Core Service    |         | PushNoti Service    |        |  Report Service     |
    |   (Port 8081)     |         |   (Port 8082)       |        |   (Port 8083)       |
    | + Socket.IO 9092  |         +----------+----------+        +----------+----------+
    +---------+---------+                    |                              |
              |                              |                              |
    +---------v---------+         +----------v----------+        +----------v----------+
    | PostgreSQL + Redis|         |      MongoDB        |        |      MongoDB        |
    +-------------------+         +---------------------+        +---------------------+

              +------------------------------+------------------------------+
              |                                                             |
    +---------v---------+                                        +----------v----------+
    | AI Support (RAG)  |                                        | AI Analysis (Vanna) |
    |   (Port 8001)     |                                        |   (Port 8000)       |
    +---------+---------+                                        +----------+----------+
              |                                                             |
    +---------v---------+                                        +----------v----------+
    | Neo4j + MongoDB   |                                        |     PostgreSQL      |
    +-------------------+                                        +---------------------+

                                    +------------------+
                                    |    RabbitMQ      |
                                    | (Port 5672/15672)|
                                    +------------------+
```

## Services

| Service | Port | Description | Technology |
|---------|------|-------------|------------|
| Gateway Service | 8080 | API Gateway, routing, load balancing | Spring Cloud Gateway |
| Core Service | 8081, 9092 | Main business logic, authentication, chat | Spring Boot, Socket.IO |
| PushNoti Service | 8082 | Push notification management | Spring Boot, Firebase |
| Report Service | 8083 | Reports and analytics | Spring Boot, MongoDB |
| AI Support | 8001 | AI-powered consulting (LightRAG) | FastAPI, Neo4j |
| AI Analysis | 8000 | AI data analysis (Vanna) | FastAPI, PostgreSQL |

## Infrastructure

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL (Core) | 5432 | Main database for Core Service |
| PostgreSQL (Vanna) | 5433 | Database for AI Analysis |
| Redis | 6379 | Cache and session store |
| MongoDB (PushNoti) | 27018 | Database for Push Notification |
| MongoDB (Report) | 27019 | Database for Report Service |
| MongoDB (AI) | 27022 | Database for AI Support |
| Neo4j | 7474, 7687 | Graph database for LightRAG |
| RabbitMQ | 5672, 15672 | Message broker |

## Prerequisites

- Docker & Docker Compose
- Make (If you don't have Make, you can do docker-compose commands manually or install it via https://gnuwin32.sourceforge.net/packages/make.htm)
- Git

## Installation

### 1. Clone repository

```bash
git clone <repository-url>
cd ISU-Backend-Production
```

**Note:** Of course ISU-Backend-Production is the latest, combined version. The origin Backend was built first with only Monolythic architecture, then we split and built more microservices due to the requirements of scalability and maintainability. If you want, I can add you to see the original microservices repo, they are currently private, separate repos for each service:
- ISU-Backend-CoreService
- ISU-Backend-GatewayService
- ISU-Backend-PushNoti
- ISU-Backend-ReportService
- ISU-AI-Support
- ISU-AI-Analysis
- common (this if for common libs shared between services)

### 2. Configure environment

Review and edit files in the `env/` directory:

```
env/
  ├── core-service.env      # Core Service config
  ├── gateway-service.env   # Gateway config
  ├── pushnoti-service.env  # Push Notification config
  ├── report-service.env    # Report Service config
  ├── ai-support.env        # AI Support (LightRAG) config
  ├── ai-analysis.env       # AI Analysis (Vanna) config
  └── common.env            # Shared variables
```

### 3. Start services

```bash
# Start all services
make quick-start

# Or start step by step
make network-create
make up
```

## Common Commands

### Manage all services

```bash
make up              # Start all services
make down            # Stop all services
make build           # Build all services
make rebuild         # Rebuild and restart all
make logs            # View logs
make status          # Check status
make clean           # Remove containers
make clean-all       # Remove everything including volumes
```

### Manage individual services

```bash
# Gateway
make gateway-up / gateway-down / gateway-logs / gateway-rebuild

# Core Service
make core-up / core-down / core-logs / core-rebuild
make core-run          # Run locally with Maven
make core-test         # Run tests

# Push Notification
make pushnoti-up / pushnoti-down / pushnoti-logs / pushnoti-rebuild

# Report Service
make report-up / report-down / report-logs / report-rebuild
make report-import-data    # Import sample data

# AI Support (LightRAG)
make ai-support-up / ai-support-down / ai-support-logs
make ai-support-health     # Check health

# AI Analysis (Vanna)
make ai-analysis-up / ai-analysis-down / ai-analysis-logs
```

### Infrastructure

```bash
make infra-up        # Start databases, Redis, RabbitMQ
make infra-down      # Stop infrastructure
make dev             # Start infra for local development
```

## Project Structure

```
ISU-Backend-All/
├── docker-compose.yaml          # Main Docker Compose file
├── Makefile                     # Management commands
├── env/                         # Environment files
│   ├── core-service.env
│   ├── gateway-service.env
│   ├── pushnoti-service.env
│   ├── report-service.env
│   ├── ai-support.env
│   ├── ai-analysis.env
│   └── common.env
├── ISU-Backend-CoreService/     # Core Service source
├── ISU-Backend-GatewayService/  # Gateway Service source
├── ISU-Backend-PushNoti/        # Push Notification source
├── ISU-Backend-ReportService/   # Report Service source
├── ISU-AI-Support/              # AI Support (LightRAG) source
├── ISU-AI-Analysis/             # AI Analysis (Vanna) source
└── var/                         # Logs (gitignored)
    └── logs/
```

## API Endpoints

### Gateway (Port 8080)

All requests go through the Gateway and are routed to services:

- `/api/v1/**` -> Core Service
- `/api/pushnoti/**` -> Push Notification Service
- `/api/report/**` -> Report Service
- `/api/ai/**` -> AI Support Service
- `/api/analysis/**` -> AI Analysis Service

### Core Service (Port 8081)

- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/register` - Register
- `GET /api/v1/users/**` - User management
- `GET /api/v1/seers/**` - Seer management
- `GET /api/v1/bookings/**` - Booking management
- Socket.IO: `ws://localhost:9092` - Real-time chat

### AI Support (Port 8001)

- `GET /health` - Health check
- `POST /api/query` - AI query
- API Docs: http://localhost:8001/docs

### AI Analysis (Port 8000)

- `GET /health` - Health check
- `POST /api/ask` - Data analysis
- API Docs: http://localhost:8000/docs

## Troubleshooting

### MongoDB fails to start

```bash
# Remove volumes and restart
make clean-all
make quick-start
```

### Service cannot connect to database

```bash
# Check if infrastructure is ready
make infra-up
make status

# Wait 30s for databases to be ready
# Then start services
make up
```

### View logs for debugging

```bash
# View logs of a specific service
make core-logs
make gateway-logs

# View infrastructure logs
make infra-logs
```

### Full reset

```bash
make clean-all
make quick-start
```

## Development

### Run locally (without Docker)

```bash
# Start infrastructure
make dev

# Run Core Service with Maven
make core-run

# Or run directly
cd ISU-Backend-CoreService
mvn spring-boot:run
```

### Run tests

```bash
make core-test
```

## License

Private - ISeeYou Fortune Telling Platform
