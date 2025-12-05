# ===========================================
# ISU Backend - Unified Makefile
# ===========================================
# All microservices management in one place
# Usage: make <service>-<command>
#
# Environment Selection:
#   make ENV=dev <command>   - Use dev environment (default)
#   make ENV=prod <command>  - Use prod environment
# ===========================================

# Default target
.DEFAULT_GOAL := help

# Environment selection (default: dev)
ENV ?= prod

# Docker compose settings based on environment
ifeq ($(ENV),prod)
    DOCKER_COMPOSE = docker-compose -f docker/prod/docker-compose.yaml
    ENV_DIR = ./env/prod
else
    DOCKER_COMPOSE = docker-compose -f docker/dev/docker-compose.yaml
    ENV_DIR = ./env/dev
endif

# ===========================================
# HELP
# ===========================================

help: ## Show this help message
	@echo ""
	@echo ============================================================
	@echo        ISU Backend - Unified Makefile
	@echo ============================================================
	@echo ""
	@echo Current Environment: $(ENV)
	@echo   - Use ENV=dev for development (databases + AI services in Docker, Spring Boot local)
	@echo   - Use ENV=prod for production (all services in Docker)
	@echo ""
	@echo [ALL SERVICES]
	@echo   make up                    - Start all services
	@echo   make down                  - Stop all services
	@echo   make build                 - Build all services
	@echo   make rebuild               - Rebuild and restart all services
	@echo   make logs                  - Show all logs
	@echo   make status                - Show status of all services
	@echo   make clean                 - Stop and remove containers
	@echo   make clean-all             - Remove everything including volumes
	@echo ""
	@echo [GATEWAY SERVICE] (prod only)
	@echo   make gateway-build         - Build gateway service
	@echo   make gateway-up            - Start gateway service
	@echo   make gateway-down          - Stop gateway service
	@echo   make gateway-logs          - Show gateway logs
	@echo   make gateway-rebuild       - Rebuild gateway service
	@echo ""
	@echo [CORE SERVICE] (prod only for Docker, local for dev)
	@echo   make core-build            - Build core service
	@echo   make core-up               - Start core service
	@echo   make core-down             - Stop core service
	@echo   make core-logs             - Show core service logs
	@echo   make core-run              - Run core service locally (Maven)
	@echo   make core-compile          - Compile core service
	@echo   make core-package          - Package core service JAR
	@echo   make core-clean            - Clean core service Maven
	@echo   make core-migration-up     - Apply Flyway migrations
	@echo   make core-migration-info   - Show Flyway migration info
	@echo   make core-rebuild          - Rebuild core service
	@echo ""
	@echo [PUSH NOTIFICATION SERVICE] (prod only for Docker, local for dev)
	@echo   make pushnoti-build        - Build pushnoti service
	@echo   make pushnoti-up           - Start pushnoti service
	@echo   make pushnoti-down         - Stop pushnoti service
	@echo   make pushnoti-logs         - Show pushnoti logs
	@echo   make pushnoti-rebuild      - Rebuild pushnoti service
	@echo   make pushnoti-clean        - Clean pushnoti data
	@echo ""
	@echo [REPORT SERVICE] (prod only for Docker, local for dev)
	@echo   make report-build          - Build report service
	@echo   make report-up             - Start report service
	@echo   make report-down           - Stop report service
	@echo   make report-logs           - Show report logs
	@echo   make report-logs-mongo     - Show MongoDB logs
	@echo   make report-import-data    - Import JSON data to MongoDB
	@echo   make report-list-collections - List MongoDB collections
	@echo   make report-rebuild        - Rebuild report service
	@echo   make report-clean          - Clean report data
	@echo ""
	@echo [AI SUPPORT SERVICE - LightRAG]
	@echo   make ai-support-build      - Build AI support service
	@echo   make ai-support-up         - Start AI support service
	@echo   make ai-support-down       - Stop AI support service
	@echo   make ai-support-logs       - Show AI support logs
	@echo   make ai-support-rebuild    - Rebuild AI support service
	@echo   make ai-support-health     - Check AI support health
	@echo   make ai-support-clean      - Clean AI support data
	@echo   make ai-support-clean-all  - Remove all AI support data
	@echo ""
	@echo [AI ANALYSIS SERVICE - Vanna]
	@echo   make ai-analysis-build     - Build AI analysis service
	@echo   make ai-analysis-up        - Start AI analysis service
	@echo   make ai-analysis-down      - Stop AI analysis service
	@echo   make ai-analysis-logs      - Show AI analysis logs
	@echo   make ai-analysis-rebuild   - Rebuild AI analysis service
	@echo   make ai-analysis-clean     - Clean AI analysis data
	@echo ""
	@echo [RABBITMQ]
	@echo   make rabbitmq-up           - Start RabbitMQ
	@echo   make rabbitmq-down         - Stop RabbitMQ
	@echo   make rabbitmq-logs         - Show RabbitMQ logs
	@echo ""
	@echo [INFRASTRUCTURE]
	@echo   make infra-up              - Start all infrastructure
	@echo   make infra-down            - Stop all infrastructure
	@echo   make infra-logs            - Show infrastructure logs (DB, Redis, RabbitMQ)
	@echo   make network-create        - Create Docker network (Remember to run this first)
	@echo ""
	@echo [QUICK COMMANDS]
	@echo   make dev                   - Start dev infrastructure + AI services
	@echo   make prod                  - Start prod (all services in Docker)
	@echo   make quick-start           - Quick start everything (network + all services)
	@echo ""

# ===========================================
# ALL SERVICES
# ===========================================

up: ## Start all services
	@echo Starting all services in $(ENV) environment...
	$(DOCKER_COMPOSE) up -d
	@echo All services started!
	@make status

down: ## Stop all services
	@echo Stopping all services in $(ENV) environment...
	$(DOCKER_COMPOSE) down
	@echo All services stopped!

build: ## Build all services
	@echo Building all services in $(ENV) environment...
	$(DOCKER_COMPOSE) build
	@echo Build complete!

rebuild: down build up ## Rebuild and restart all services

logs: ## Show all logs
	$(DOCKER_COMPOSE) logs -f

status: ## Show status of all services
	@echo Service Status ($(ENV) environment):
	$(DOCKER_COMPOSE) ps

clean: ## Stop and remove containers
	@echo Cleaning up containers in $(ENV) environment...
	$(DOCKER_COMPOSE) down --remove-orphans
	docker system prune -f
	@echo Cleanup complete!

clean-all: ## Remove everything including volumes
	@echo WARNING: This will remove ALL data including databases!
	$(DOCKER_COMPOSE) down -v --remove-orphans
	docker system prune -a -f --volumes
	@echo Everything cleaned!

network-create: ## Create Docker network
	@echo Creating Docker network...
	-docker network create isu-internal-network 2>nul || echo Network already exists
	@echo Network ready!

# ===========================================
# GATEWAY SERVICE
# ===========================================

gateway-build: ## Build gateway service
	@echo Building Gateway Service...
	$(DOCKER_COMPOSE) build gateway-service

gateway-up: ## Start gateway service
	@echo Starting Gateway Service...
	$(DOCKER_COMPOSE) up -d gateway-service
	@echo Gateway started at http://localhost:8080

gateway-down: ## Stop gateway service
	$(DOCKER_COMPOSE) stop gateway-service

gateway-logs: ## Show gateway logs
	$(DOCKER_COMPOSE) logs -f gateway-service

gateway-rebuild: gateway-down gateway-build gateway-up ## Rebuild gateway

# ===========================================
# CORE SERVICE
# ===========================================

core-build: ## Build core service
	@echo Building Core Service...
	$(DOCKER_COMPOSE) build core-backend-service

core-up: ## Start core service with dependencies
	@echo Starting Core Service...
	$(DOCKER_COMPOSE) up -d postgres-core redis rabbitmq core-backend-service
	@echo Core Service started at http://localhost:8081

core-down: ## Stop core service
	$(DOCKER_COMPOSE) stop core-backend-service

core-logs: ## Show core service logs
	$(DOCKER_COMPOSE) logs -f core-backend-service

core-rebuild: core-down core-build core-up ## Rebuild core service

# Core service local development (Maven)
core-run: ## Run core service locally (Maven)
	@echo Running Core Service locally...
	cd ISU-Backend-CoreService && mvn spring-boot:run

core-test: ## Run core service tests
	@echo Running Core Service tests...
	cd ISU-Backend-CoreService && mvn test

core-compile: ## Compile core service
	cd ISU-Backend-CoreService && mvn compile

core-package: ## Package core service JAR
	cd ISU-Backend-CoreService && mvn package -DskipTests

core-clean: ## Clean core service Maven
	cd ISU-Backend-CoreService && mvn clean

core-migration-up: ## Apply Flyway migrations
	cd ISU-Backend-CoreService && mvn flyway:migrate

core-migration-info: ## Show Flyway migration info
	cd ISU-Backend-CoreService && mvn flyway:info

# ===========================================
# PUSH NOTIFICATION SERVICE
# ===========================================

pushnoti-build: ## Build pushnoti service
	@echo Building PushNoti Service...
	$(DOCKER_COMPOSE) build pushnoti-service

pushnoti-up: ## Start pushnoti service with dependencies
	@echo Starting PushNoti Service...
	$(DOCKER_COMPOSE) up -d mongodb-pushnoti rabbitmq pushnoti-service
	@echo PushNoti Service started at http://localhost:8085

pushnoti-down: ## Stop pushnoti service
	$(DOCKER_COMPOSE) stop pushnoti-service

pushnoti-logs: ## Show pushnoti logs
	$(DOCKER_COMPOSE) logs -f pushnoti-service

pushnoti-rebuild: pushnoti-down pushnoti-build pushnoti-up ## Rebuild pushnoti

pushnoti-clean: ## Stop and remove pushnoti volumes
	$(DOCKER_COMPOSE) stop pushnoti-service mongodb-pushnoti
	$(DOCKER_COMPOSE) rm -f pushnoti-service mongodb-pushnoti
	docker volume rm -f isu-backend-$(ENV)_mongodb_pushnoti_data 2>nul || true

# ===========================================
# REPORT SERVICE
# ===========================================

report-build: ## Build report service
	@echo Building Report Service...
	$(DOCKER_COMPOSE) build report-service

report-up: ## Start report service with dependencies
	@echo Starting Report Service...
	$(DOCKER_COMPOSE) up -d mongodb-report rabbitmq report-service
	@echo Report Service started at http://localhost:8086

report-down: ## Stop report service
	$(DOCKER_COMPOSE) stop report-service

report-logs: ## Show report logs
	$(DOCKER_COMPOSE) logs -f report-service

report-logs-mongo: ## Show MongoDB logs
	$(DOCKER_COMPOSE) logs -f mongodb-report

report-rebuild: report-down report-build report-up ## Rebuild report service

report-import-data: ## Import JSON data to MongoDB
	@echo Importing data to MongoDB...
	@echo Waiting for MongoDB to be ready...
	@sleep 5 2>/dev/null || ping -n 6 127.0.0.1 > nul
	@echo ""
	@echo Importing customer_potentials...
	$(DOCKER_COMPOSE) exec -T mongodb-report mongoimport \
		--authenticationDatabase admin \
		--username admin \
		--password secret \
		--db isu_report_mongo \
		--collection customer_potentials \
		--type json \
		--file /data/customer_potential.json \
		--jsonArray \
		--drop
	@echo ""
	@echo Importing seer_performances...
	$(DOCKER_COMPOSE) exec -T mongodb-report mongoimport \
		--authenticationDatabase admin \
		--username admin \
		--password secret \
		--db isu_report_mongo \
		--collection seer_performances \
		--type json \
		--file /data/seer_performance.json \
		--jsonArray \
		--drop
	@echo ""
	@echo Data import complete!

report-list-collections: ## List MongoDB collections
	$(DOCKER_COMPOSE) exec -T mongodb-report mongosh \
		--authenticationDatabase admin \
		--username admin \
		--password secret \
		--eval "use isu_report_mongo; db.getCollectionNames()"

report-clean: ## Stop and remove report volumes
	$(DOCKER_COMPOSE) stop report-service mongodb-report
	$(DOCKER_COMPOSE) rm -f report-service mongodb-report
	docker volume rm -f isu-backend-$(ENV)_mongodb_report_data 2>nul || true

# ===========================================
# AI SUPPORT SERVICE (LightRAG)
# ===========================================

ai-support-build: ## Build AI support service
	@echo Building AI Support Service...
	$(DOCKER_COMPOSE) build lightrag-api

ai-support-up: _ai-support-create-volumes ## Start AI support service with dependencies
	@echo Starting AI Support Service...
	$(DOCKER_COMPOSE) up -d neo4j mongodb-ai rabbitmq lightrag-api
	@echo Waiting for services to be ready...
	@sleep 30 2>/dev/null || ping -n 31 127.0.0.1 > nul
	@echo AI Support Service started!
	@echo API: http://localhost:8001
	@echo Docs: http://localhost:8001/docs
	@echo Neo4j: http://localhost:7474

ai-support-down: ## Stop AI support service
	$(DOCKER_COMPOSE) stop lightrag-api neo4j mongodb-ai

ai-support-logs: ## Show AI support logs
	$(DOCKER_COMPOSE) logs -f lightrag-api

ai-support-rebuild: ai-support-down ai-support-build ai-support-up ## Rebuild AI support

ai-support-health: ## Check AI support health
	@echo Checking AI Support health...
	@curl -s http://localhost:8001/health || echo API not responding

ai-support-clean: ## Clean AI support containers
	$(DOCKER_COMPOSE) stop lightrag-api neo4j mongodb-ai
	$(DOCKER_COMPOSE) rm -f lightrag-api neo4j mongodb-ai

ai-support-clean-all: ## Remove all AI support data including volumes
	@echo WARNING: This will remove ALL AI Support data!
	$(DOCKER_COMPOSE) stop lightrag-api neo4j mongodb-ai
	$(DOCKER_COMPOSE) rm -f lightrag-api neo4j mongodb-ai
	docker volume rm -f isu-backend-$(ENV)_neo4j_data isu-backend-$(ENV)_neo4j_logs \
		isu-backend-$(ENV)_neo4j_import isu-backend-$(ENV)_neo4j_plugins \
		isu-backend-$(ENV)_mongodb_ai_data isu-backend-$(ENV)_lightrag_storage 2>nul || true

_ai-support-create-volumes:
	@echo Creating AI Support directories...
	@if not exist "ISU-AI-Support\docker\volumes" mkdir ISU-AI-Support\docker\volumes 2>nul || true
	@if not exist "ISU-AI-Support\docker\logs" mkdir ISU-AI-Support\docker\logs 2>nul || true
	@if not exist "var\logs\lightrag" mkdir var\logs\lightrag 2>nul || true

# ===========================================
# AI ANALYSIS SERVICE (Vanna)
# ===========================================

ai-analysis-build: ## Build AI analysis service
	@echo Building AI Analysis Service...
	$(DOCKER_COMPOSE) build vanna-server postgres-vanna

ai-analysis-up: ## Start AI analysis service with dependencies
	@echo Starting AI Analysis Service...
	$(DOCKER_COMPOSE) up -d postgres-vanna vanna-server
	@echo AI Analysis Service started at http://localhost:8000

ai-analysis-down: ## Stop AI analysis service
	$(DOCKER_COMPOSE) stop vanna-server postgres-vanna

ai-analysis-logs: ## Show AI analysis logs
	$(DOCKER_COMPOSE) logs -f vanna-server

ai-analysis-rebuild: ## Rebuild AI analysis service
	$(DOCKER_COMPOSE) stop vanna-server postgres-vanna
	$(DOCKER_COMPOSE) rm -f vanna-server postgres-vanna
	docker volume rm -f isu-backend-$(ENV)_postgres_vanna_data 2>nul || true
	$(DOCKER_COMPOSE) up -d --build postgres-vanna vanna-server

ai-analysis-clean: ## Clean AI analysis data
	$(DOCKER_COMPOSE) stop vanna-server postgres-vanna
	$(DOCKER_COMPOSE) rm -f vanna-server postgres-vanna
	docker volume rm -f isu-backend-$(ENV)_postgres_vanna_data 2>nul || true

# ===========================================
# RABBITMQ
# ===========================================

rabbitmq-up: ## Start RabbitMQ
	@echo Starting RabbitMQ...
	$(DOCKER_COMPOSE) up -d rabbitmq
	@echo RabbitMQ started!
	@echo Management UI: http://localhost:15672 (admin/secret)

rabbitmq-down: ## Stop RabbitMQ
	$(DOCKER_COMPOSE) stop rabbitmq

rabbitmq-logs: ## Show RabbitMQ logs
	$(DOCKER_COMPOSE) logs -f rabbitmq

# ===========================================
# INFRASTRUCTURE
# ===========================================

infra-up: ## Start all infrastructure (DB, Redis, RabbitMQ, Neo4j)
	@echo Starting infrastructure services in $(ENV) environment...
	$(DOCKER_COMPOSE) up -d postgres-core redis rabbitmq mongodb-pushnoti mongodb-report mongodb-ai neo4j postgres-vanna
	@echo Infrastructure started!
	@echo ""
	@echo Services:
	@echo   PostgreSQL Core: localhost:5432
	@echo   PostgreSQL Vanna: localhost:5433
	@echo   Redis: localhost:6379
	@echo   RabbitMQ: localhost:5672 (Management: 15672)
	@echo   MongoDB PushNoti: localhost:27018
	@echo   MongoDB Report: localhost:27019
	@echo   MongoDB AI: localhost:27022
	@echo   Neo4j: localhost:7687 (Browser: 7474)

infra-down: ## Stop all infrastructure
	@echo Stopping infrastructure services...
	$(DOCKER_COMPOSE) stop postgres-core redis rabbitmq mongodb-pushnoti mongodb-report mongodb-ai neo4j postgres-vanna

infra-logs: ## Show infrastructure logs
	$(DOCKER_COMPOSE) logs -f postgres-core redis rabbitmq

# ===========================================
# QUICK COMMANDS
# ===========================================

dev: network-create infra-up ai-support-up ai-analysis-up ## Start dev environment (infrastructure + AI services)
	@echo ""
	@echo ============================================================
	@echo DEV environment ready!
	@echo ============================================================
	@echo ""
	@echo Infrastructure services running in Docker:
	@echo   PostgreSQL Core: localhost:5432
	@echo   PostgreSQL Vanna: localhost:5433
	@echo   Redis: localhost:6379
	@echo   RabbitMQ: localhost:5672 (Management: http://localhost:15672)
	@echo   MongoDB PushNoti: localhost:27018
	@echo   MongoDB Report: localhost:27019
	@echo   MongoDB AI: localhost:27022
	@echo   Neo4j: localhost:7687 (Browser: http://localhost:7474)
	@echo ""
	@echo AI services running in Docker:
	@echo   AI Support (LightRAG): http://localhost:8001
	@echo   AI Analysis (Vanna): http://localhost:8000
	@echo ""
	@echo Now run Spring Boot services locally:
	@echo   cd ISU-Backend-CoreService and run: mvn spring-boot:run
	@echo   cd ISU-Backend-GatewayService and run: mvn spring-boot:run
	@echo   cd ISU-Backend-PushNoti and run: mvn spring-boot:run
	@echo   cd ISU-Backend-ReportService and run: mvn spring-boot:run
	@echo ""

prod: ## Start prod environment (all services in Docker)
	@make ENV=prod network-create up
	@echo ""
	@echo ============================================================
	@echo PROD environment ready!
	@echo ============================================================
	@echo ""
	@echo All services running in Docker.

quick-start: network-create up _wait-for-services report-import-data ## Quick start everything
	@echo ""
	@echo All services are running!

_wait-for-services:
	@echo Waiting for services to be ready...
	@sleep 15 2>/dev/null || ping -n 16 127.0.0.1 > nul

.PHONY: help up down build rebuild logs status clean clean-all network-create \
	gateway-build gateway-up gateway-down gateway-logs gateway-rebuild \
	core-build core-up core-down core-logs core-rebuild core-run core-test core-compile core-package core-clean core-migration-up core-migration-info \
	pushnoti-build pushnoti-up pushnoti-down pushnoti-logs pushnoti-rebuild pushnoti-clean \
	report-build report-up report-down report-logs report-logs-mongo report-rebuild report-import-data report-list-collections report-clean \
	ai-support-build ai-support-up ai-support-down ai-support-logs ai-support-rebuild ai-support-health ai-support-clean ai-support-clean-all \
	ai-analysis-build ai-analysis-up ai-analysis-down ai-analysis-logs ai-analysis-rebuild ai-analysis-clean \
	rabbitmq-up rabbitmq-down rabbitmq-logs \
	infra-up infra-down infra-logs dev prod quick-start
