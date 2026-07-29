# Vetra Backend — Spring Boot 3 & PostgreSQL Service

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Vetra Backend is the production-quality, modular monolith REST API powering the **Vetra** livestock healthcare and disease surveillance ecosystem. Built with Spring Boot 3, Java 17, and PostgreSQL 15, it manages dual-role authentication (Farmer/Veterinarian), livestock records, clinical appointments, and immutable Electronic Veterinary Medical Records (EVMR).

---

## Key Features

- **Dual-Role Identity & Access Control:** Separate registration and profile management for Farmers and Veterinarians, secured with JWT and refresh token rotation.
- **Livestock Management:** Digital animal registration with unique QR-code Animal Passports.
- **Clinical Appointment Engine:** Complete state machine (`PENDING` → `CONFIRMED` → `COMPLETED`/`CANCELLED`) with JPA optimistic locking (`version`).
- **Electronic Veterinary Medical Records (EVMR):** Immutable clinical medical records created by veterinarians for completed appointments, providing a permanent medical history.
- **Role-Specific Dashboards:** Aggregated metrics tailored to farmer and veterinarian workflows.

---

## System Architecture Overview

Vetra Backend is designed as a **Modular Monolith** following **Clean Architecture** principles:

```
app.vetra/
├── auth/           ← Identity, JWT, Vet Directory
├── animal/         ← Animal registration & passports
├── appointment/    ← Booking & clinical state machine
├── medicalrecord/  ← Immutable EVMR clinical history
├── dashboard/      ← Role-based metric aggregation
└── infrastructure/ ← Shared entities, Spring Security, Flyway, exceptions
```

---

## Professional Documentation Index

Every aspect of this system is thoroughly documented. Please consult the engineering guides before contributing:

### 🏛 Architecture & Design
- [Engineering Principles](docs/engineering/00-principles.md) — *The Vetra Engineering Constitution*
- [Software Architecture Document (SAD)](docs/architecture/02-SAD.md)
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
- [Coding Standards & Conventions (Java/Spring)](docs/engineering/12-coding-standards.md)
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
- JDK 17+
- Docker Desktop

### 2. Start Database
```bash
cp .env.example .env
docker compose -f docker-compose.dev.yml up -d
```

### 3. Run Backend
```bash
./mvnw spring-boot:run
```

### 4. Health Check
```bash
curl http://localhost:8080/actuator/health
```

For full local environment setup details, read the [Developer Onboarding Guide](docs/guides/20-developer-onboarding.md).
