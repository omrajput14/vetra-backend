# Software Architecture Document — Backend
**Document ID:** ARCH-02-BACKEND  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Engineering Principles](../engineering/00-principles.md), [Modular Monolith](./08-modular-monolith.md), [Domain Model](../domain/03-domain-model.md), [Database Design](../database/04-database-design.md)

---

## 1. System Overview

The Vetra backend is a **Spring Boot 3 REST API** serving the Vetra Flutter mobile application. It provides authentication, animal management, appointment scheduling, and electronic veterinary medical records (EVMR) for two user roles: Farmers and Veterinarians.

**Technology Stack:**

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 + JWT (JJWT) |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 15 |
| Schema Migrations | Flyway |
| Build Tool | Maven (via `./mvnw` wrapper) |
| Containerisation | Docker + Docker Compose |
| API Style | REST, JSON, `application/json` |
| ID Format | UUID v4 |

---

## 2. Architectural Style

### Modular Monolith with Clean Architecture

The backend is a **modular monolith** — a single deployable application with strict internal module boundaries. See [docs/architecture/08-modular-monolith.md](./08-modular-monolith.md) for the full rationale and future extraction strategy.

Within each module, **Clean Architecture** layer boundaries are enforced:

```
┌─────────────────────────────────────────────────────┐
│                  HTTP Layer                          │
│  Controllers receive requests, return responses      │
│  No business logic. @Valid on all @RequestBody.     │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                Application Layer                     │
│  Services own business logic + authorization         │
│  Enforce ownership. Return DTOs, never entities.     │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│               Infrastructure Layer                   │
│  Repositories. Flyway migrations. JPA entities.      │
│  External framework concerns.                        │
└─────────────────────────────────────────────────────┘
```

**Dependency rule:** Controllers know Services; Services know Repositories; Repositories know Entities. No upward dependencies.

---

## 3. Module Map

```
app.vetra/
├── auth/           Identity, registration, login, JWT issuance
├── animal/         Animal CRUD, QR code management
├── appointment/    Appointment booking, state machine, scheduling
├── medicalrecord/  EVMR creation and retrieval
├── dashboard/      Role-specific aggregation (read-only)
└── infrastructure/ Shared: entities, enums, security, config, exceptions
```

### Cross-Cutting Concerns (infrastructure/)

| Concern | Location | Description |
|---|---|---|
| JPA Entities | `infrastructure/persistence/entity/` | Shared JPA entities referenced across modules |
| Enumerations | `infrastructure/persistence/enums/` | `AppointmentStatus`, `VisitType`, `Species`, etc. |
| Spring Security | `infrastructure/security/` | `SecurityConfig`, `JwtAuthFilter`, `JwtService` |
| Exception Handling | `infrastructure/config/` | `GlobalExceptionHandler` — centralized error mapping |
| JPA Configuration | `infrastructure/config/` | Transaction management, JPA settings |

---

## 4. Security Architecture

Authentication is JWT-based with refresh token rotation. Full details in [`docs/api/07-auth-design.md`](../api/07-auth-design.md).

**Request pipeline:**

```
HTTP Request
    │
    ▼ JwtAuthFilter (Spring Security Filter)
Extract Bearer token → Validate signature and expiry → Load user from DB
    │
    ▼ SecurityContextHolder
Authenticated user available via @AuthenticationPrincipal
    │
    ▼ Controller → Service
Service verifies ownership (userId from JWT, not from request body)
```

---

## 5. API Design

- **Versioned:** All endpoints are prefixed `/api/v1/`
- **RESTful:** Resource-oriented URLs, semantically correct HTTP verbs
- **JSON-only:** `Content-Type: application/json` required on all write operations
- **UUID identifiers:** All resource identifiers are UUIDs
- **Error envelope:** All errors return `{ "error": { "code": "...", "message": "..." } }`

Full endpoint reference: [`docs/api/06-specification.md`](../api/06-specification.md)

---

## 6. Database Architecture

**PostgreSQL 15** with the following characteristics:
- All migrations managed by **Flyway** (V1 through V6 as of Stage 7)
- UUID primary keys on all tables
- Timezone-aware timestamps (`TIMESTAMP WITH TIME ZONE`) everywhere
- Optimistic locking (`version BIGINT`) on `appointments` and `medical_records`
- Comprehensive index coverage on all FK columns and filter columns

Full schema: [`docs/database/04-database-design.md`](../database/04-database-design.md)  
ERD: [`docs/database/05-ERD.md`](../database/05-ERD.md)

---

## 7. Key Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Monolith vs. Microservices | Modular monolith | Single developer, pre-launch, operational simplicity |
| ID type | UUID v4 | Prevents ID enumeration, future-proof for distribution |
| Password hashing | bcrypt (cost 10) | Industry standard, resistant to GPU brute force |
| Token storage | Refresh token in DB | Enables revocation without Redis |
| Entity exposure | DTOs only (never entities in responses) | Decouples API contract from DB schema |
| Medical records | Immutable (no PUT/DELETE) | Legal/clinical document integrity |

All decisions documented in full: [`docs/domain/21-decision-log.md`](../domain/21-decision-log.md)
