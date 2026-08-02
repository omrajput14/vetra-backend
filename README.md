# Vetra Backend — Veterinary Operating System (VetOS) REST API

[![Build Status](https://github.com/omrajput14/vetra-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/omrajput14/vetra-backend/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.5-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17%2BPostGIS-blue.svg)](https://www.postgresql.org/)
[![Redis 7](https://img.shields.io/badge/Redis-7%20Alpine-red.svg)](https://redis.io/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-red.svg)](https://flywaydb.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Vetra Backend is the production-grade, modular monolith REST API powering **Vetra** — an enterprise Veterinary Operating System (VetOS) for livestock healthcare, practice management, and epidemiological disease surveillance. Built with Spring Boot 3, Java 21 LTS, PostgreSQL 17 + PostGIS, and Redis 7, it provides dual-role authentication (Farmer/Veterinarian), digital animal passports, clinical appointment state management, and immutable Electronic Veterinary Medical Records (EVMR).

---

## Key Capabilities

- **Dual-Role Identity & Access Control:** Role-Based Access Control (RBAC) separating Farmers and Veterinarians, secured via stateless JWT and database-backed refresh token rotation.
- **Digital Livestock Passport:** Unique QR-code registration (`qr_code_id`) providing tamper-proof animal health records and chronological medical history.
- **Clinical Appointment Engine:** Formal state machine (`PENDING` → `CONFIRMED` → `COMPLETED` / `CANCELLED`) with JPA optimistic locking (`version`).
- **Electronic Veterinary Medical Records (EVMR):** Immutable clinical medical records created by licensed veterinarians for completed appointments, fulfilling medical data compliance.
- **Role-Specific Dashboards:** Aggregated metrics tailored to farmer herd health and veterinarian clinical schedules.
- **Interactive OpenAPI / Swagger UI:** Built-in dynamic API documentation powered by SpringDoc OpenAPI.

---

## System Architecture Overview

Vetra Backend is designed as a **Modular Monolith** following **Clean Architecture** and **Domain-Driven Design (DDD)** principles:

```
app.vetra/
├── auth/           ← Identity, JWT, Refresh Tokens, Vet Directory
├── animal/         ← Animal registration & QR Animal Passports
├── appointment/    ← Scheduling & clinical state machine
├── medicalrecord/  ← Immutable EVMR clinical history
├── dashboard/      ← Role-based metrics aggregation
└── infrastructure/ ← Shared JPA entities, Spring Security, Flyway, exception handling
```

---

## Professional Documentation Index

Comprehensive engineering specifications and design records are maintained under `docs/`:

### 🏛 Architecture & Product
- [Engineering Principles](docs/engineering/00-principles.md) — *The Vetra Engineering Constitution*
- [Product Requirements Document (PRD v2.0.0)](docs/product/01-PRD.md)
- [Software Architecture Document (SAD v2.0.0)](docs/architecture/02-SAD.md)
- [Modular Monolith Architecture](docs/architecture/08-modular-monolith.md)
- [Deployment Architecture](docs/architecture/09-deployment.md)
- [Infrastructure Diagram](docs/architecture/10-infrastructure.md)
- [Architecture Decision Log](docs/domain/21-decision-log.md)

### 📊 Domain & Database
- [Domain Model & Ubiquitous Language](docs/domain/03-domain-model.md)
- [Database Design Document](docs/database/04-database-design.md)
- [Entity Relationship Diagram (ERD)](docs/database/05-ERD.md)

### 🔌 API & Security
- [API Specification (REST Endpoints)](docs/api/06-specification.md)
- [Authentication & Authorization Design](docs/api/07-auth-design.md)
- [API Versioning Strategy](docs/api/22-versioning.md)
- [Application Error Catalogue](docs/api/23-error-catalogue.md)
- [Security Design Document](docs/security/11-security-design.md)

### 🛠 Operations & Engineering Guides
- [Developer Onboarding Guide](docs/guides/20-developer-onboarding.md) — *Start here!*
- [Enterprise Caching Architecture](docs/architecture/25-caching-architecture.md)
- [Cache Performance Benchmark Report](docs/performance/cache-benchmark-report.md) — *Stage 12.3.4 empirical benchmark report*
- [Coding Standards & Conventions (Java 21/Spring)](docs/engineering/12-coding-standards.md)
- [Git Workflow & Branching Strategy](docs/engineering/13-git-workflow.md)
- [Testing Strategy](docs/guides/14-testing-strategy.md)
- [CI/CD Pipeline Specification](docs/operations/15-cicd.md)
- [Logging & Monitoring Strategy](docs/operations/16-logging-monitoring.md)
- [Disaster Recovery & Backup Plan](docs/operations/17-disaster-recovery.md)
- [Scalability & Performance Plan](docs/operations/18-scalability.md)
- [Environment Configuration](docs/operations/24-environment-config.md)

---

## Quick Start (Local Development)

### 1. Prerequisites
- **JDK 21 LTS** (`java -version` → 21.x)
- **Docker Desktop** (for PostgreSQL 15 + PostGIS)

### 2. Start Database
```bash
cp .env.example .env
docker compose -f docker-compose.dev.yml up -d
```

### 3. Run Backend Application
```bash
./mvnw spring-boot:run
```

### 4. Verify Health & OpenAPI Docs
- **Actuator Health Check:** `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
- **Swagger UI:** Open `http://localhost:8080/swagger-ui.html` in your browser

### 5. Format & Lint
```bash
# Code formatting check
./mvnw spotless:check

# Apply Google Java Format
./mvnw spotless:apply

# Run Checkstyle validation
./mvnw checkstyle:check
```

For step-by-step onboarding, read the [Developer Onboarding Guide](docs/guides/20-developer-onboarding.md).
