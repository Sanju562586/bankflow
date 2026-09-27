@echo off
echo ==========================================================
echo        Starting Bankflow Enterprise Banking System
echo ==========================================================
echo.

where docker >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Docker detected. Starting complete multi-container stack via Docker Compose...
    docker-compose up -d
    echo.
    echo Stack started!
    echo - Web Dashboard:       http://localhost:3000
    echo - API Gateway:          http://localhost:8080
    echo - Prometheus:           http://localhost:9090
    echo - Grafana:              http://localhost:3001  (admin/admin)
    echo - Jaeger Tracing:       http://localhost:16686
    echo.
) else (
    echo [INFO] Docker not found in PATH. Starting Bankflow Web Dashboard on port 3000...
    start http://localhost:3000
    node bankflow-dashboard\server.js
)
