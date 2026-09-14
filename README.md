# Enterprise Digital Bank

Flagship portfolio project: a digital banking platform built as independent, polyglot microservices behind a single API Gateway, communicating asynchronously over Kafka, backed by PostgreSQL/Redis, and consumed by an Angular web client — all containerized for Podman/Docker.

```
                      Angular Web (frontend/)
                              |
                              v
                    API Gateway (Spring Cloud Gateway, JWT auth) :8080
                              |
              +---------------+---------------+
              |                               |
       Customer Service                 Account Service
       (Spring Boot, JPA)                (Spring Boot, JPA)
             :8081                            :8082

       Transaction Service  ---Kafka--->  Payment Service
       (Spring Boot, JPA)   transaction-  (Spring WebFlux,
             :8083           events       R2DBC)  :8084
                                                |
                                          payment-events
                                                |
                                                v
                                     Notification Service
                                     (Python / FastAPI) :8085

                       PostgreSQL (one DB per service) / Redis / Kafka
                       Podman / Docker Compose
```

Creating a deposit or withdrawal on `transaction-service` publishes a `TransactionCreatedEvent` to Kafka; `payment-service` consumes it, auto-approves the payment, and publishes a `PaymentProcessedEvent`; `notification-service` consumes that event and records/"sends" a notification. This demonstrates an event-driven flow across three independently deployable services in two different languages.

## Technologies

**Backend** — Java 25, Spring Boot 4.0.8, Spring Cloud Gateway (WebFlux), Spring Security + JWT (jjwt), Spring Data JPA, Spring Data R2DBC, Spring Kafka, PostgreSQL, Maven, JUnit 5 + Mockito.

**Notification service** — Python 3.13, FastAPI, aiokafka, Pydantic, Pytest.

**Frontend** — Angular 20 (standalone components, signals), TypeScript strict, RxJS, light/dark theming, Jest.

**Infrastructure** — PostgreSQL (one database per microservice), Redis, Kafka (KRaft mode, no Zookeeper), Podman Compose / Docker Compose, GitHub Actions CI.

**Architecture & practices** — Domain-Driven Design–inspired layering (controller / service / repository / domain / dto) in every backend module, test-driven bug fixes, Clean Code, consistent JSON error contract across all services, trunk-based Git flow (feature branch → PR → squash/merge to `master`).

## Repository layout

```
enterprise-digital-bank/
├── backend/
│   ├── api-gateway/           # Spring Cloud Gateway + JWT auth + routing (port 8080)
│   ├── customer-service/      # Customer CRUD (port 8081)
│   ├── account-service/       # Bank account CRUD + balance adjustment (port 8082)
│   ├── transaction-service/   # Transaction CRUD + Kafka producer (port 8083)
│   ├── payment-service/       # Reactive payment processing + Kafka consumer/producer (port 8084)
│   └── notification-service/  # Kafka consumer + notification REST API (Python, port 8085)
├── frontend/                  # Angular 20 web app
├── infra/
│   └── init-databases.sql     # creates the 4 per-service databases on first Postgres boot
├── .github/workflows/ci.yml   # CI: build + test every service and the frontend
└── docker-compose.yml         # works with `docker compose` or `podman compose`
```

## Authentication

`POST /api/auth/login` on the gateway (demo users `admin`/`admin123` and `customer`/`customer123`) issues an HS256 JWT. Every other route behind the gateway requires `Authorization: Bearer <token>`.

## Running locally

### With Podman Compose (recommended)

```bash
podman compose up --build
# or, with Docker:
docker compose up --build
```

This brings up PostgreSQL, Redis, Kafka, all five backend services, and the Angular app (served on `:4200`). The API Gateway listens on `:8080`.

### Running each piece by hand

1. Start PostgreSQL, Redis and Kafka (see `docker-compose.yml` for exact images/config).
2. From each `backend/<java-service>` directory: `./mvnw spring-boot:run` (or `mvnw.cmd` on Windows).
3. From `backend/notification-service`: `pip install -r requirements.txt && uvicorn notification_service.main:app --port 8085`.
4. From `frontend`: `npm install && npm start`, then open `http://localhost:4200`.

## Tests

Every backend module ships with unit tests that need no live database or broker:

- Java services: `./mvnw test` inside any `backend/<service>` folder (JUnit 5 + Mockito, `@WebMvcTest`/reactive `WebTestClient` slices for controllers).
- Notification service: `pytest -q` inside `backend/notification-service`.
- Frontend: `npm test -- --watch=false` inside `frontend`.

CI (`.github/workflows/ci.yml`) runs all of the above on every push and pull request to `master`.

## Wider portfolio

This repository is the integration hub for the author's broader microservices portfolio. Related, independently deployable pieces demonstrating additional stacks live in sibling repositories:

- [`react-customer-portal`](https://github.com/marcosvperboni/react-customer-portal) — React-based admin console.
- [`flutter-banking-app`](https://github.com/marcosvperboni/flutter-banking-app) — Flutter mobile client.
- [`kotlin-android-banking`](https://github.com/marcosvperboni/kotlin-android-banking) — native Kotlin/Android client.
- [`quarkus-high-performance-api`](https://github.com/marcosvperboni/quarkus-high-performance-api) — Quarkus service patterns.
- [`rabbitmq-notification-service`](https://github.com/marcosvperboni/rabbitmq-notification-service) — RabbitMQ-based messaging patterns.
