# Build stage
FROM pytorch/pytorch:2.0.1-cuda11.7-cudnn8-runtime AS builder
WORKDIR /app

# Install build dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    gcc \
    g++ \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Upgrade pip
RUN pip install --no-cache-dir --upgrade pip

# Copy and install AI dependencies first (larger, changes less frequently)
COPY requirements-ai.txt .
RUN pip install --no-cache-dir -r requirements-ai.txt

# Copy and install core dependencies
COPY requirements-core.txt .
RUN pip install --no-cache-dir -r requirements-core.txt

# Runtime stage
FROM pytorch/pytorch:2.0.1-cuda11.7-cudnn8-runtime
WORKDIR /app

# Install only runtime dependencies
RUN apt-get update && apt-get install -y \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy Python packages from builder
COPY --from=builder /opt/conda /opt/conda

# Copy application code
COPY src/ ./src/
COPY data/ ./data/

# Create necessary directories with proper permissions
RUN mkdir -p /app/rag_storage /app/logs && \
    chmod -R 777 /app/data /app/rag_storage /app/logs

# Expose port
EXPOSE 8000

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app/src

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Run the application
CMD ["python", "src/main.py"]
