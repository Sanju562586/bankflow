#!/usr/bin/env bash
set -e

echo "=========================================================="
echo "       Starting Bankflow Enterprise Banking System        "
echo "=========================================================="

if command -v docker >/dev/null 2>&1; then
    echo "[INFO] Starting full microservices stack with Docker Compose..."
    docker-compose up -d
    echo ""
    echo "Services running:"
    echo "- Web Dashboard:       http://localhost:3000"
    echo "- API Gateway:          http://localhost:8080"
    echo "- Prometheus:           http://localhost:9090"
    echo "- Grafana:              http://localhost:3001"
    echo "- Jaeger Tracing:       http://localhost:16686"
else
    echo "[INFO] Starting Web Dashboard server on port 3000..."
    node bankflow-dashboard/server.js
fi
