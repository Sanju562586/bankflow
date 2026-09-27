# Bankflow - Enterprise Distributed Banking System

Bankflow is a cloud-native, event-driven banking microservices architecture built with Spring Boot 3, Apache Kafka, PostgreSQL, Redis, XGBoost ML scoring, Groq LLM reasoning, and full observability (Prometheus, Grafana, Jaeger, Structured SLF4J JSON logging).

## Architecture Overview

```
                      +---------------------------------------+
                      |              Client Tier              |
                      |   Mobile App - Web Dashboard - APIs   |
                      +-------------------+-------------------+
                                          |
                                          v
                      +---------------------------------------+
                      |              API Gateway              |
                      |   Spring Cloud Gateway - JWT - Rate   |
                      +-------------------+-------------------+
                                          |
           +-----------------+------------+------------+-----------------+
           |                 |                         |                 |
           v                 v                         v                 v
    +--------------+  +---------------+         +--------------+  +--------------+
    |   Account    |  |    Payment    |         |    Fraud     |  | Notification |
    |   Service    |  |    Service    |         |   Detection  |  |   Service    |
    | (PostgreSQL) |  |  (PostgreSQL) |         | (ML + Groq)  |  | (SMS/Mail/UI)|
    +-------+------+  +-------+-------+         +-------+------+  +-------+------+
            |                 |                         |                 |
            +-----------------+------------+------------+-----------------+
                                           |
                                           v
    =============================================================================
                       Apache Kafka Distributed Event Bus
        Topics: txn-events  |  fraud-alerts  |  audit-log  |  notification-queue
    =============================================================================
```

## Microservices Breakdown

1. **`account-service`** (`:8081`): CRUD for accounts, balance inquiry, transaction history, publishes `AccountCreated` & `BalanceUpdated` events.
2. **`payment-service`** (`:8082`): NEFT / UPI / IMPS payments, idempotency key enforcement, publishes `TransactionInitiated`, saga pattern for commit/rollback.
3. **`fraud-detection-service`** (`:8083`): Real-time Kafka consumer, XGBoost feature scoring engine, Groq LLM natural-language reasoning for borderline cases, publishes `FraudAlert` or `TransactionApproved`.
4. **`notification-service`** (`:8084`): Kafka consumer for alerts & approvals, multi-channel dispatch (Twilio SMS sandbox, JavaMail, in-app push), retry with exponential backoff.
5. **`api-gateway`** (`:8080`): Spring Cloud Gateway, JWT authentication, Redis token bucket rate limiting per user, downstream routing.
6. **`bankflow-dashboard`** (`:3000`): Full interactive real-time Web dashboard & visualizer.

## DevOps & Infrastructure

- **Docker & Docker Compose**: Full local cluster with Kafka KRaft, PostgreSQL, Redis, Prometheus, Grafana, Jaeger.
- **Kubernetes**: Production manifests with Deployments, Services, ConfigMaps, Secrets, and HorizontalPodAutoscalers (HPA).
- **GitHub Actions**: Automated CI/CD pipeline on PR & merge to `main`.
