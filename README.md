# Bankflow - Enterprise Distributed Banking System

A cloud-native, event-driven banking microservices platform featuring Spring Boot 3, Apache Kafka, PostgreSQL, Redis, XGBoost ML scoring (SMOTE), Groq LLM natural-language reasoning, full observability (Prometheus, Grafana, Jaeger), and a modern interactive web dashboard.

---

## 🏛 Architecture Diagram

```
                                  +---------------------------------------+
                                  |              Client Tier              |
                                  |   Mobile App · Web Dashboard · APIs   |
                                  +-------------------+-------------------+
                                                      |
                                                      v
                                  +---------------------------------------+
                                  |              API Gateway              |
                                  |     Spring Cloud Gateway · JWT Auth   |
                                  |      Redis Token Bucket Rate Limiter  |
                                  +-------------------+-------------------+
                                                      |
                   +------------------+---------------+---------------+------------------+
                   |                  |                               |                  |
                   v                  v                               v                  v
            +--------------+   +---------------+               +--------------+   +--------------+
            |   Account    |   |    Payment    |               |    Fraud     |   | Notification |
            |   Service    |   |    Service    |               |  Detection   |   |   Service    |
            | (PostgreSQL) |   | (PostgreSQL)  |               | (ML + Groq)  |   | (SMS/Mail/UI)|
            +-------+------+   +-------+-------+               +-------+------+   +-------+------+
                    |                  |                               |                  |
                    +------------------+---------------+---------------+------------------+
                                                       |
                                                       v
            =============================================================================
                               Apache Kafka Distributed Event Mesh
                Topics: txn-events · fraud-alerts · audit-log · notification-queue
            =============================================================================
                                  |                    |                    |
                                  v                    v                    v
                        +------------------+ +-------------------+ +------------------+
                        | Stream Processor | |  AI Fraud Engine  | |   Audit Writer   |
                        | (Real-time Aggs) | | XGBoost - Groq LLM| |  Immutable Log  |
                        +------------------+ +-------------------+ +------------------+
                                                       |
                                                       v
            =============================================================================
                                              Data Tier
                     PostgreSQL (Accounts & Txns) · Redis (Rate Limits) · Logs/Audit
            =============================================================================
```

---

## 🚀 The 5 Microservices

### 1. `account-service` (`Port 8081`)
- **Technology:** Spring Boot 3 + PostgreSQL + Spring Data JPA + Kafka Producer
- **Features:** CRUD for accounts, balance inquiry, transaction history ledger with pessimistic write-locks
- **REST Endpoints:**
  - `POST /accounts`: Open new account
  - `GET /accounts/{id}`: Balance & profile lookup
  - `GET /accounts`: List accounts
  - `GET /accounts/{id}/balance`: Balance inquiry
  - `POST /accounts/{id}/balance`: Atomic debit/credit adjustment (called by payment saga)
  - `GET /accounts/{id}/transactions`: Transaction statement history
- **Kafka Events:** Publishes `AccountCreatedEvent` (topic `account-events`) & `BalanceUpdatedEvent` (topic `balance-updates`)

### 2. `payment-service` (`Port 8082`)
- **Technology:** Spring Boot 3 + PostgreSQL + Kafka Producer/Consumer + Saga Coordinator
- **Features:** Accepts payments (amount, sender/receiver, mode: NEFT/UPI/IMPS), idempotency key enforcement (`Idempotency-Key` header) to reject duplicate requests, orchestrates multi-step commit/rollback saga
- **REST Endpoints:**
  - `POST /payments`: Initiate payment (Idempotency Key protected)
  - `GET /payments/{id}`: Inspect payment status and saga state
  - `GET /payments`: List all payments
  - `GET /payments/account/{accountId}`: Payment history per account
- **Kafka Events:** Publishes `TransactionInitiatedEvent` to `txn-events`. Consumes `TransactionApprovedEvent`, `TransactionRejectedEvent`, and `FraudAlertEvent` for Saga resolution.

### 3. `fraud-detection-service` (`Port 8083`)
- **Technology:** Spring Boot 3 + XGBoost Tree Ensemble (SMOTE) + Groq LLM Client + Kafka Consumer/Producer
- **Features:** Real-time scoring of `TransactionInitiated` events. If score is in ambiguous band (`0.35 <= score <= 0.75`), sends features to **Groq LLM** (`llama-3.1-70b-versatile`) for natural-language contextual reasoning (*"This transaction is 3x the account's 30-day average..."*).
- **REST Endpoints:**
  - `POST /fraud/evaluate`: Interactive scoring endpoint
  - `GET /fraud/alerts`: List recent high-risk fraud alerts
  - `GET /fraud/evaluations`: List all recent evaluations
  - `GET /fraud/model-metrics`: Model AUC-ROC, precision, recall, and SMOTE metadata
- **Kafka Events:** Consumes `txn-events`. Publishes `FraudAlertEvent` to `fraud-alerts` and `TransactionApprovedEvent` to `txn-approved`.

### 4. `notification-service` (`Port 8084`)
- **Technology:** Spring Boot 3 + Kafka Consumer + Spring Retry (Exponential Backoff)
- **Features:** Multi-channel notification delivery:
  - **SMS:** Twilio Sandbox mock/live client
  - **Email:** JavaMailSender HTML/plain-text delivery
  - **In-App Push:** Real-time push feed for web/mobile clients
  - **Exponential Backoff Retry:** `@Retryable` with initial 1000ms delay, 2.0x multiplier, and recovery fallback
- **REST Endpoints:**
  - `GET /notifications`: Dispatched notification history
  - `GET /notifications/feed`: Active in-app notification feed
  - `POST /notifications/test`: Test manual dispatch with simulated carrier retry
- **Kafka Events:** Consumes `fraud-alerts` and `txn-approved`.

### 5. `api-gateway` (`Port 8080`)
- **Technology:** Spring Cloud Gateway (Reactive Netty) + Reactive Redis
- **Features:**
  - Single entry point for all client requests
  - JWT authentication filter (`JwtAuthGlobalFilter`) & role-based authorization (`ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_OPERATOR`)
  - Redis Token Bucket Rate Limiter per authenticated user (`RequestRateLimiter`)
  - Dynamic routing to downstream microservices with header mutation (`X-User-Id`, `X-User-Roles`)
- **Endpoints:**
  - `POST /auth/token`: Issues signed HMAC-SHA256 JWT tokens
  - `POST /auth/validate`: Validates JWT token
  - Downstream proxies: `/accounts/**`, `/payments/**`, `/fraud/**`, `/notifications/**`

---

## 💻 Client Tier: Interactive Web Dashboard (`Port 3000`)

The web dashboard is running live at:
👉 **`http://localhost:3000`**

### What you can test manually on the dashboard:
1. **System Topology:** Interactive visual architecture map displaying the 5 microservices, Kafka event mesh, and status indicators.
2. **Payment Terminal & Saga Visualizer:**
   - Execute payments with NEFT, UPI, or IMPS.
   - Watch the animated **6-step Saga Tracker** node-by-node in real time!
   - Test **Idempotency Key** generation and protection.
3. **AI Fraud Radar:**
   - Watch the real-time **XGBoost Probability Gauge**.
   - See the **Groq LLM Natural-Language Rationale Card** generate explanations for suspicious amounts and nocturnal transfers.
4. **Account Manager:**
   - View live balances and customer profiles.
   - Open new bank accounts with the modal form.
   - Inspect the transaction statement ledger.
5. **Notification Center:**
   - View simulated smartphone screen with real-time SMS alert bubbles.
   - Test failed SMS delivery to observe **Exponential Backoff Retry** in action.
6. **Live Kafka Event Bus:**
   - Real-time terminal streaming `txn-events`, `fraud-alerts`, `balance-updates`, `account-events`, and `notification-queue`.
7. **API Playground:**
   - Inspect active JWT tokens and copy pre-built cURL commands.

---

## 🛠 DevOps, Kubernetes & Observability

### Kubernetes (`k8s/`)
- Each service has its own:
  - `Deployment` (with readiness/liveness probes, environment bindings)
  - `ClusterIP` / `LoadBalancer` Service
  - `HorizontalPodAutoscaler` (HPA with CPU utilization thresholds)
- Bitnami Kafka: Production Helm chart values (`kafka-bitnami-helm-values.yaml`) with KRaft mode and topic pre-provisioning.
- Kubernetes Secrets (`secrets.yaml`): Non-hardcoded database passwords, JWT secrets, Groq API keys, and Twilio credentials.
- Ingress (`ingress.yaml`): NGINX routing for `api.bankflow.internal` and `app.bankflow.internal`.

### GitHub Actions CI/CD (`.github/workflows/ci-cd.yml`)
- **On Pull Request:** Matrix build across microservices, Maven test execution, Docker container build, push to GitHub Container Registry (`ghcr.io`).
- **On Merge to `main`:** Automated `kubectl apply` deployment to the Kubernetes cluster.

### Observability
- **Structured Logging:** SLF4J + Logback with `LogstashLogbackEncoder` producing structured JSON logs with trace IDs.
- **Prometheus:** Scrapes `/actuator/prometheus` across all microservices on `http://localhost:9090`.
- **Grafana:** Pre-provisioned dashboards on `http://localhost:3001` (login: `admin` / `admin`).
- **Jaeger:** Distributed tracing across services on `http://localhost:16686`.

---

## 🏃 Quick Start Guide

### 1. Run via Docker Compose (Recommended)
```bash
docker-compose up -d
```
All 12 containers (PostgreSQL, Redis, Kafka, Prometheus, Grafana, Jaeger, 5 Spring Boot microservices, and Web Dashboard) will boot automatically.

### 2. Run Web Dashboard Manually
```bash
node bankflow-dashboard/server.js
# Or on Windows: start.bat
# Or on Linux/macOS: ./start.sh
```
Open your browser to: **`http://localhost:3000`**

---

## 🌿 Git Branches & History

Every module was built on an isolated feature branch with multiple commits and merged directly into `main` and pushed to the remote repository (`origin`):

- `feature/common-dto`
- `feature/account-service`
- `feature/payment-service`
- `feature/fraud-detection-service`
- `feature/notification-service`
- `feature/api-gateway`
- `feature/client-dashboard`
- `feature/infra-observability`
- `main`
