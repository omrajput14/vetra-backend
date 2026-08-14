# Vetra Backend — Veterinary Operating System (VetOS) REST API

[![Build Status](https://github.com/omrajput14/vetra-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/omrajput14/vetra-backend/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.5.3-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%20%7C%2017%2BPostGIS-blue.svg)](https://www.postgresql.org/)
[![Redis 7](https://img.shields.io/badge/Redis-7%20TLS-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage-blue.svg)](https://www.docker.com/)
[![AWS ECS](https://img.shields.io/badge/AWS-ECS%20Fargate%20%2B%20ALB-orange.svg)](https://aws.amazon.com/ecs/)
[![Terraform](https://img.shields.io/badge/Terraform-1.5%2B-purple.svg)](https://www.terraform.io/)
[![Flyway](https://img.shields.io/badge/Flyway-14%20Migrations-red.svg)](https://flywaydb.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Vetra Backend is a production-grade, modular monolith REST API powering **Vetra** — an enterprise Veterinary Operating System (VetOS) for livestock healthcare management, clinical veterinary practice, and epidemiological disease surveillance.

Built with **Java 21 LTS** and **Spring Boot 3**, backed by **PostgreSQL with PostGIS** and **Redis 7**, the application is fully containerized with **Docker** and deployed to **AWS** via **Terraform** on **Amazon ECS Fargate** behind an **Application Load Balancer (ALB)**.

---

## Key Capabilities

* **Dual-Role Identity & Access Control:** Role-Based Access Control (RBAC) separating Farmers and Veterinarians, secured via stateless HMAC-SHA256 JWTs and database-backed refresh token rotation.
* **Digital Livestock Passport:** Unique QR-code registration (`qr_code_id`) providing tamper-proof animal identification, ownership tracking, and chronological medical history.
* **Clinical Appointment Engine:** Formal state machine (`PENDING` → `CONFIRMED` → `COMPLETED` / `CANCELLED`) with JPA optimistic locking (`version`) for conflict-free scheduling.
* **Electronic Veterinary Medical Records (EVMR):** Immutable clinical medical records created by licensed veterinarians for completed consultations, fulfilling veterinary regulatory compliance.
* **Epidemiological Disease Surveillance:** Geospatial outbreak tracking, disease reporting, and automated alert generation powered by PostGIS spatial queries.
* **Multi-Channel Notification Engine:** Notification dispatch architecture supporting in-app preferences, templates, and delivery tracking.
* **Role-Specific Dashboards:** Aggregated metrics tailored to farmer herd health metrics and veterinarian clinical schedules.
* **Interactive OpenAPI / Swagger UI:** Built-in interactive API documentation generated automatically by SpringDoc OpenAPI.

---

## Technology Stack

| Layer | Technologies |
|---|---|
| **Language & Runtime** | Java 21 LTS (Eclipse Temurin), JVM G1GC |
| **Framework** | Spring Boot 3.5.3, Spring Security, Spring Data JPA / Hibernate 6, SpringDoc OpenAPI 2.8 |
| **Relational Database** | PostgreSQL (RDS 15.13 in Staging / 17 in Local Dev) with PostGIS Spatial Extensions |
| **Database Migrations** | Flyway (14 verified, version-controlled SQL migrations) |
| **Cache & In-Memory Data** | Redis 7.1 (AWS ElastiCache with in-transit TLS encryption; Lettuce driver) |
| **Containerization** | Docker multi-stage build, Alpine Linux runtime, non-root system user (`vetra`) |
| **Cloud Infrastructure** | AWS (VPC, ECS Fargate, ALB, RDS, ElastiCache, ECR, Secrets Manager, CloudWatch Logs) |
| **Infrastructure as Code** | Terraform 1.5+ (Modular architecture, S3 remote state backend with DynamoDB locking) |
| **CI/CD & Security** | GitHub Actions, GitHub OIDC Keyless Authentication, Gitleaks, CodeQL |

---

## Application Architecture

Vetra Backend is structured as a **Modular Monolith** applying **Clean Architecture** and **Domain-Driven Design (DDD)** principles:

```
app.vetra/
├── auth/           ← Identity, JWT authentication, refresh token rotation, vet directory
├── animal/         ← Animal registry, species classification, QR digital passports
├── appointment/    ← Scheduling workflow & clinical state machine
├── medicalrecord/  ← Immutable Electronic Veterinary Medical Records (EVMR)
├── disease/        ← Epidemiological surveillance, outbreak detection, PostGIS spatial queries
├── notification/   ← Multi-channel notification templates, dispatch, and delivery logging
├── dashboard/      ← Role-based analytics and aggregated clinical metrics
└── infrastructure/ ← Shared JPA entities, security configuration, Redis TLS cache, Flyway, observability
```

### Architectural Rationale
The modular monolith architecture enforces strict domain package boundaries and cohesive domain models while operating as a single, easily deployable runtime unit. This avoids the operational complexity, distributed transaction overhead, and network latency of premature microservice decomposition while preserving clear decoupling for future extraction if required.

---

## AWS Deployment Architecture

The staging backend runs on a high-security, multi-AZ AWS infrastructure in `ap-south-1` provisioned entirely through Terraform:

```
                                 Internet
                                    │
                                    ▼ (HTTP :80)
                         ┌─────────────────────┐
                         │ Application Load    │
                         │ Balancer (ALB)      │
                         │ Public Subnets      │
                         └──────────┬──────────┘
                                    │
                                    ▼ (HTTP :8080, Security Group Chaining)
                         ┌─────────────────────┐
                         │ Amazon ECS Fargate  │
                         │ Vetra Backend Task  │
                         │ Private Subnets     │
                         │ NO Public IP        │
                         └──────┬───────┬──────┘
                                │       │
                       ┌────────┘       └────────┐
                       ▼                         ▼
               ┌───────────────┐         ┌───────────────┐
               │ Amazon RDS    │         │ ElastiCache   │
               │ PostgreSQL    │         │ Redis 7       │
               │ 15.13 + GIS   │         │ In-Transit TLS│
               │ (Isolated)    │         │ (Isolated)    │
               └───────────────┘         └───────────────┘

                          AWS Secrets Manager
                                 │
                                 ▼ (Injected at task startup)
                      DB / Redis / JWT secrets

        Developer Push ──► GitHub Actions ──► GitHub OIDC ──► AWS IAM ──► Amazon ECR
```

### Security Boundaries
* **Public Ingress:** The Application Load Balancer in public subnets is the sole internet entry point.
* **Compute Isolation:** ECS Fargate tasks reside strictly in private subnets with **no public IP addresses**.
* **Data Isolation:** RDS PostgreSQL and ElastiCache Redis are deployed in dedicated isolated subnets with no internet route.
* **Network Access Control:** Least-privilege security group rules restrict inbound traffic strictly along the chain (`Internet` → `ALB SG` → `ECS SG` → `RDS / Redis SGs`).
* **In-Transit Encryption:** Redis connections enforce TLS encryption (`REDIS_SSL=true`).

---

## Verified AWS Staging Infrastructure

The following infrastructure components are live and verified in AWS (`ap-south-1`):

| Component | Technology | Purpose | Status |
|---|---|---|---|
| **VPC & Subnets** | AWS VPC (`10.1.0.0/16`) | Network isolation across 2 Availability Zones | ✅ Live |
| **Public Subnets** | AWS Subnets (`10.1.1.0/24`, `10.1.2.0/24`) | Application Load Balancer placement | ✅ Live |
| **Private Subnets** | AWS Subnets (`10.1.10.0/24`, `10.1.11.0/24`) | ECS Fargate compute tasks | ✅ Live |
| **Isolated Subnets** | AWS Subnets (`10.1.20.0/24`, `10.1.21.0/24`) | RDS and ElastiCache data tier | ✅ Live |
| **NAT Gateway** | AWS NAT Gateway + Elastic IP | Controlled outbound access for private tasks | ✅ Live |
| **VPC Endpoints** | AWS PrivateLink / Gateway | Private access to S3, Secrets Manager, ECR, CloudWatch | ✅ Live |
| **Relational Database** | Amazon RDS PostgreSQL 15.13 | Persistent relational storage with PostGIS | ✅ Live |
| **In-Memory Cache** | Amazon ElastiCache Redis 7.1 | Session storage, caching, and rate limiting with TLS | ✅ Live |
| **Container Registry** | Amazon ECR (`vetra-backend-staging`) | Immutable, vulnerability-scanned Docker container images | ✅ Live |
| **Secret Storage** | AWS Secrets Manager | Encrypted runtime credential management | ✅ Live |
| **Container Runtime** | Amazon ECS Fargate | Serverless container compute engine | ✅ Live |
| **Load Balancer** | Application Load Balancer (ALB) | Public HTTP ingress and health-checked traffic routing | ✅ Live |
| **Target Group** | ALB Target Group (Port 8080) | Health-check management via `/actuator/health/liveness` | ✅ Live |
| **Observability** | Amazon CloudWatch Logs | Structured container log aggregation (`awslogs` driver) | ✅ Live |
| **Identity & Access** | AWS IAM Roles | Least-privilege ECS execution and task IAM roles | ✅ Live |
| **CI Authentication** | GitHub OIDC Identity Provider | Keyless authentication for GitHub Actions CI/CD | ✅ Live |

---

## CI/CD Pipeline

Continuous integration is managed through GitHub Actions with keyless AWS OIDC authentication:

```
Git Push to main / feature/**
   │
   ├── [Job 1: Gitleaks Secret Scanner] ──► Fails build if credentials/keys are exposed
   │
   ├── [Job 2: Build & Test Suite]      ──► Java 21 compile, Checkstyle validation, Maven tests
   │
   └── [Job 3: Docker Build & ECR Push] (main branch only)
           │
           ├── Validate AWS Account ID Variable
           ├── Authenticate to AWS via GitHub OIDC (AssumeRoleWithWebIdentity)
           ├── Log in to Amazon ECR
           ├── Build production multi-stage Docker image
           └── Push immutable commit-tagged image to Amazon ECR
```

> **Security Note:** GitHub Actions authenticates to AWS using GitHub OIDC and IAM role assumption rather than long-lived AWS access keys.

---

## Security Architecture

* **Stateless JWT Authentication:** HMAC-SHA256 tokens for authenticated API sessions.
* **Database-Backed Refresh Token Rotation:** Secure refresh token rotation with single-use revocation.
* **Runtime Secret Injection:** Application credentials (`DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`) are injected dynamically into ECS tasks at container startup through AWS Secrets Manager ARNs rather than being hardcoded into task definitions or source code.
* **Network Isolation:** ECS tasks have no public IP addresses; ingress is strictly mediated by the ALB.
* **Database & Cache Security:** RDS and ElastiCache instances are placed in isolated subnets with no internet ingress/egress.
* **Encrypted Cache Traffic:** ElastiCache Redis operates with in-transit encryption (TLS) enabled.
* **Non-Root Container Runtime:** Docker containers execute as an unprivileged system user (`vetra:vetra`).
* **Automated Security Scanning:** Gitleaks scans every commit for secret exposure, and CodeQL analyzes source code for security vulnerabilities.

---

## Database & Migrations

* **PostgreSQL Engine:** PostgreSQL 15.13 on AWS RDS staging; PostgreSQL 17 on local Docker Compose.
* **Geospatial Capabilities:** PostGIS enabled for livestock tracking and spatial outbreak queries.
* **Flyway Migration Strategy:** Schema migrations are version-controlled and executed automatically on startup.
* **Validated Schema Migrations:**
  * `V1__baseline.sql` — Extensions and baseline configuration
  * `V2__schema_entities.sql` — Users, roles, and core entity tables
  * `V3__refresh_tokens.sql` — Refresh token storage
  * `V4__add_animal_name.sql` — Animal name column addition
  * `V5__create_appointments.sql` — Appointment state machine and scheduling tables
  * `V6__create_medical_records.sql` — Electronic Veterinary Medical Records (EVMR)
  * `V7__optimize_vet_availability_index.sql` — Performance indexes for veterinarian availability
  * `V8__create_ai_scan_tables.sql` — AI diagnostic scan records
  * `V9__create_ai_scan_results.sql` — AI diagnostic result structured storage
  * `V10__make_medical_records_appointment_optional.sql` — Walk-in record support
  * `V11__create_disease_surveillance.sql` — Disease surveillance and PostGIS spatial tables
  * `V12__outbreak_intelligence_engine.sql` — Outbreak detection algorithms and triggers
  * `V13__disease_intelligence_automation.sql` — Automated alert generation
  * `V14__notification_engine.sql` — Notification templates, preferences, and delivery logs

---

## Health & Observability

### Health Endpoints (Spring Boot Actuator & Custom Probes)

| Endpoint | HTTP Method | Expected Status | Purpose | Verified Status |
|---|---|---|---|---|
| `/actuator/health/liveness` | `GET` | `200 OK` | Container liveness probe | ✅ `{"status":"UP"}` |
| `/actuator/health/readiness` | `GET` | `200 OK` | Database & disk space readiness probe | ✅ `{"status":"UP"}` |
| `/actuator/health` | `GET` | `200 OK` | Full application health (DB + Redis TLS) | ✅ `{"status":"UP"}` |
| `/liveness` | `GET` | `200 OK` | Unauthenticated application liveness | ✅ `{"status":"ALIVE"}` |
| `/readiness` | `GET` | `200 OK` | Unauthenticated application readiness | ✅ `{"status":"READY"}` |

### Container Logging
* Container logs stream to Amazon CloudWatch Logs under `/ecs/vetra-staging-backend` using the `awslogs` driver.
* Structured logging includes timestamp, thread, trace ID, span ID, and request correlation IDs.

---

## API Documentation

Vetra Backend provides dynamic OpenAPI 3.0 documentation generated by SpringDoc OpenAPI:

* **Swagger UI (Interactive):** `http://localhost:8080/swagger-ui.html`
* **OpenAPI JSON Specification:** `http://localhost:8080/v3/api-docs`

---

## Quick Start (Local Development)

### 1. Prerequisites
* **JDK 21 LTS** (`java -version` → 21.x)
* **Docker & Docker Compose** (for PostgreSQL 17 + PostGIS and Redis 7)

### 2. Configure Environment
```bash
cp .env.example .env
```

### 3. Start Local Infrastructure Containers
```bash
docker compose up -d postgres redis
```

### 4. Run the Backend Application
```bash
./mvnw spring-boot:run
```

### 5. Verify Health & OpenAPI Docs
* **Health Check:** `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
* **Swagger UI:** Open `http://localhost:8080/swagger-ui.html` in your browser

### 6. Run Code Formatting & Quality Checks
```bash
# Verify Google Java Format
./mvnw spotless:check

# Apply formatting
./mvnw spotless:apply

# Run Checkstyle validation
./mvnw checkstyle:check

# Run test suite
./mvnw test
```

---

## Documentation Index

Comprehensive engineering specifications and architecture design records are maintained under `docs/`:

### 🏛 Architecture & Product
* [Engineering Principles](docs/engineering/00-principles.md) — *The Vetra Engineering Constitution*
* [Product Requirements Document (PRD v2.0.0)](docs/product/01-PRD.md)
* [Software Architecture Document (SAD v2.0.0)](docs/architecture/02-SAD.md)
* [Modular Monolith Architecture](docs/architecture/08-modular-monolith.md)
* [Deployment Architecture](docs/architecture/09-deployment.md)
* [Infrastructure Diagram](docs/architecture/10-infrastructure.md)
* [Architecture Decision Log](docs/domain/21-decision-log.md)

### 📊 Domain & Database
* [Domain Model & Ubiquitous Language](docs/domain/03-domain-model.md)
* [Database Design Document](docs/database/04-database-design.md)
* [Entity Relationship Diagram (ERD)](docs/database/05-ERD.md)

### 🔌 API & Security
* [API Specification (REST Endpoints)](docs/api/06-specification.md)
* [Authentication & Authorization Design](docs/api/07-auth-design.md)
* [API Versioning Strategy](docs/api/22-versioning.md)
* [Application Error Catalogue](docs/api/23-error-catalogue.md)
* [Security Design Document](docs/security/11-security-design.md)
* [AWS Security Architecture](docs/operations/aws-security.md)

### 🛠 Operations & Engineering Guides
* [Developer Onboarding Guide](docs/guides/20-developer-onboarding.md)
* [Enterprise Caching Architecture](docs/architecture/25-caching-architecture.md)
* [Cache Performance Benchmark Report](docs/performance/cache-benchmark-report.md)
* [Coding Standards & Conventions](docs/engineering/12-coding-standards.md)
* [Git Workflow & Branching Strategy](docs/engineering/13-git-workflow.md)
* [Testing Strategy](docs/guides/14-testing-strategy.md)
* [CI/CD Pipeline Specification](docs/operations/15-cicd.md)
* [Logging & Monitoring Strategy](docs/operations/16-logging-monitoring.md)
* [Disaster Recovery & Backup Plan](docs/operations/17-disaster-recovery.md)
* [Scalability & Performance Plan](docs/operations/18-scalability.md)
* [Environment Configuration](docs/operations/24-environment-config.md)

---

## Deployment Status Summary

| Area | Component | Implementation / Cloud Provider | Status |
|---|---|---|---|
| **Core REST API** | Spring Boot 3.5.3 / Java 21 | Modular Monolith DDD Architecture | ✅ Verified |
| **Containerization** | Docker Multi-Stage | Alpine JRE 21, Non-root `vetra` User | ✅ Verified |
| **Relational Database** | Amazon RDS PostgreSQL 15.13 | Multi-AZ ready, Isolated Subnets | ✅ Verified |
| **Geospatial Engine** | PostGIS Extension | Spatial Indexing & Outbreak Queries | ✅ Verified |
| **In-Memory Cache** | Amazon ElastiCache Redis 7.1 | In-Transit TLS Encryption, Isolated Subnets | ✅ Verified |
| **Schema Migrations** | Flyway DB | 14 Validated Migrations | ✅ Verified |
| **Network Architecture** | AWS VPC (`10.1.0.0/16`) | Public, Private & Isolated Subnets, NAT GW | ✅ Verified |
| **Container Registry** | Amazon ECR | Staging Registry with Scan-on-Push & Lifecycle Rules | ✅ Verified |
| **CI Keyless Auth** | GitHub OIDC Identity Provider | AWS IAM Role Assumption (`AssumeRoleWithWebIdentity`) | ✅ Verified |
| **Compute Engine** | Amazon ECS Fargate | Serverless Tasks in Private Subnets (No Public IP) | ✅ Verified |
| **Ingress & Routing** | Application Load Balancer (ALB) | Public Ingress with Target Group Health Probes | ✅ Verified |
| **Secrets Management** | AWS Secrets Manager | Runtime ARN Injection (DB, Redis, JWT) | ✅ Verified |
| **Observability** | Amazon CloudWatch Logs | Container Logs via `awslogs` Log Driver | ✅ Verified |
| **Health Checks** | Spring Boot Actuator | Liveness & Readiness Probes (`HTTP 200 OK`) | ✅ Verified |

---

## Next Steps

### Next Infrastructure Stage (Stage 14.8)
* **Custom Domain & HTTPS Hardening:** Provision an AWS Certificate Manager (ACM) SSL/TLS certificate for the staging domain and configure an HTTPS listener on port 443 with an HTTP 80 → 443 redirect rule on the Application Load Balancer.
* **Automated CD Deployment Step:** Add an automated `aws ecs update-service` step in GitHub Actions to automatically trigger ECS rolling deployments upon newly published ECR container images.

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
