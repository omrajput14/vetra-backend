# Modular Monolith Architecture
**Document ID:** ARCH-08  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [SAD](./02-SAD.md), [Engineering Principles](../engineering/00-principles.md), [Decision Log ADL-010](../domain/21-decision-log.md)

---

## Executive Summary

The Vetra backend is a **modular monolith** — a single deployable Spring Boot application composed of strictly isolated feature modules with enforced internal boundaries. It is *not* a microservices system.

This architecture was chosen deliberately for the current stage of Vetra's development. This document describes:
1. The current architecture (implemented)
2. Why a monolith was chosen
3. The module boundaries and isolation rules
4. The scalability limitations and thresholds
5. The future extraction strategy and target microservices architecture

> [!IMPORTANT]
> Never document a microservices architecture as if it were already implemented. The sections on future vision are clearly labeled **FUTURE VISION** and describe planned, not current, behavior.

---

## Part 1: Current State — Modular Monolith

### Package Structure

```
app.vetra/
├── VetraApplication.java          ← Application entry point
│
├── auth/                          ← Identity & Authentication module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── dto/
│
├── animal/                        ← Livestock Management module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── dto/
│
├── appointment/                   ← Appointment Management module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── dto/
│
├── medicalrecord/                 ← EVMR module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── dto/
│
├── dashboard/                     ← Dashboard Aggregation module
│   ├── controller/
│   └── service/
│
└── infrastructure/                ← Shared infrastructure (not a feature module)
    ├── persistence/
    │   ├── entity/                ← JPA entities (shared across modules via FK references)
    │   └── enums/
    ├── security/                  ← Spring Security config, JWT filter
    └── config/                    ← JPA config, global exception handler
```

### Module Boundary Rules

Each module (`auth`, `animal`, `appointment`, `medicalrecord`, `dashboard`) is treated as if it were a separate service with one exception: they share a database and a JVM process.

**Rules enforced by code review (not compile-time — future: ArchUnit):**

1. A module's `service/` classes must not import another module's `service/` classes.
2. A module's `controller/` classes must not import another module's `service/` or `repository/` classes directly.
3. Cross-module data access must go through the `repository/` layer only (via shared JPA entities).
4. The `infrastructure/` package is the *only* shared code. It provides:
   - JPA entities (referenced as FK targets across modules)
   - Security configuration
   - Global exception handling
   - Application configuration

**Example — Correct Cross-Module Access:**
```java
// In MedicalRecordService — reading an Appointment by ID
// ✅ Correct: uses AppointmentRepository (repository layer)
@Autowired
private AppointmentRepository appointmentRepository;
```

**Example — Incorrect Cross-Module Access:**
```java
// In MedicalRecordService — calling AppointmentService
// ❌ Incorrect: service-to-service coupling
@Autowired
private AppointmentService appointmentService;
```

### Communication Patterns

Within the monolith, modules communicate only via:
1. **Database reads** — one module reads another's tables via shared JPA entity references.
2. **Shared DTOs** — minimal response types shared in `infrastructure/` only when necessary.

Event-driven communication is planned but not yet implemented (see Future Vision).

### Why This Architecture Was Chosen

| Factor | Monolith Advantage |
|---|---|
| **Team size** | Single developer. Microservices require distributed tracing, service discovery, and cross-service testing — disproportionate overhead. |
| **Traffic volume** | Current traffic is zero (pre-launch). No scaling requirement today. |
| **Development velocity** | Refactoring across modules is fast in a monolith. Adding a column to `appointments` doesn't require an API contract change between services. |
| **Operational simplicity** | One process to deploy, monitor, and restart. One database. One set of logs. |
| **Testability** | Integration tests run against the whole application in-process. No mocking of network calls between services. |
| **Module clarity** | The boundary rules above give us the *design benefits* of microservices without the *operational costs*. |

---

## Part 2: Scalability Limitations

The modular monolith has known limitations that will become constraints as Vetra scales:

| Limitation | Threshold | Mitigation |
|---|---|---|
| **Vertical scaling only** | Single server can scale up but not out horizontally without shared session state | JWT is stateless — horizontal scaling is possible behind a load balancer |
| **Shared database** | All modules share one PostgreSQL instance | Connection pooling (HikariCP) manages current load; read replicas are the next step |
| **No independent deployment** | Deploying a fix to `medical-records` requires deploying the entire application | Acceptable at current scale; extraction resolves this |
| **No independent scaling** | If the `appointment` module receives high traffic, all modules must scale | Acceptable until load analysis identifies hotspots |
| **Shared failure domain** | An OOM error in one module can crash the entire application | Health checks and autorestart mitigate this |

**Extraction Trigger:** A module should be considered for extraction to an independent service when **any two** of the following are true:
- The module has an independent team (2+ developers working exclusively on it)
- The module requires scaling independently of the rest of the system
- The module's deployment frequency exceeds 2× the rest of the system
- The module introduces technology requirements incompatible with Spring Boot (e.g., AI/ML inference)

---

## Part 3: FUTURE VISION — Target Microservices Architecture

> [!NOTE]
> This section describes a planned future state. None of this is currently implemented.

When the extraction triggers above are met, Vetra will migrate toward the following service decomposition.

### Target Service Map

```
┌──────────────────────────────────────────────────────────────────┐
│                    Flutter Mobile Client                         │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTPS
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                       API Gateway                                │
│          (route, auth verification, rate limiting)               │
└──┬──────────┬──────────┬──────────┬──────────┬──────────────────┘
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────────────┐
│Auth  │  │Animal│  │Appt  │  │EVMR  │  │Disease       │
│Svc   │  │Svc   │  │Svc   │  │Svc   │  │Surveillance  │
│      │  │      │  │      │  │      │  │Svc           │
│:8081 │  │:8082 │  │:8083 │  │:8084 │  │:8085         │
└──┬───┘  └──┬───┘  └──┬───┘  └──┬───┘  └──────────────┘
   │         │         │         │
   └─────────┴─────────┴─────────┘
                   │
         ┌─────────┴──────────┐
         │    Message Bus     │
         │   (Apache Kafka    │
         │    or RabbitMQ)    │
         └────────────────────┘
```

### Planned Service Responsibilities

| Service | Owns | Database |
|---|---|---|
| `auth-service` | Users, profiles, JWT issuance, refresh tokens | `auth_db` |
| `animal-service` | Animals, QR codes, animal passport | `animal_db` |
| `appointment-service` | Appointments, state machine | `appointment_db` |
| `evmr-service` | Medical records, clinical history | `evmr_db` |
| `disease-service` | Disease reports, outbreaks, alerts | `disease_db` |
| `notification-service` | Push notifications, alert dispatch | `notification_db` |

### Migration Approach

1. **Strangler Fig Pattern** — Extract one module at a time, routing traffic to the new service while keeping the monolith for other modules.
2. **Database decomposition** — Each service gets its own database schema. Data that spans services is replicated via events (not shared tables).
3. **Event-driven integration** — Services communicate via domain events on a message bus (Kafka or RabbitMQ). No direct HTTP calls between services.
4. **API Gateway** — Kong, AWS API Gateway, or Spring Cloud Gateway handles routing, authentication verification, and rate limiting.

### Migration Sequence (Planned)

1. Extract `auth-service` first (self-contained, clear boundary)
2. Extract `animal-service` (depends only on auth for user identity)
3. Extract `evmr-service` (highest regulatory sensitivity — isolated first for compliance)
4. Extract `appointment-service` (requires events from auth and animal)
5. Add `disease-service` as a new greenfield service (not extracted — new capability)
