# Architecture Decision Log
**Document ID:** DOMAIN-21  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** All Vetra repositories  
**References:** [Engineering Principles](../engineering/00-principles.md), [`docs/architecture/adr/`](../architecture/adr/)

---

## Overview

This log provides a **chronological, project-wide record** of every significant engineering decision made on Vetra. It complements the individual Architecture Decision Records (ADRs) in `docs/architecture/adr/` by providing a unified timeline across all technical domains — database, API, security, infrastructure, and process.

**ADR vs. Decision Log:**
- **ADRs** are self-contained documents exploring a single decision in depth with full context and alternatives.
- **This log** is a summary index — a timeline that lets any engineer reconstruct the thinking behind the system's current state.

> [!IMPORTANT]
> Every architectural decision — whether it introduces a new technology, changes an existing approach, or documents a deliberate tradeoff — must have an entry in this log before the implementing code is merged.

---

## Log Entries

---

### [ADL-001] — 2026-07-27 — Flutter Selected as Mobile Client

**Context:** Vetra requires a cross-platform mobile application for Android and iOS. The application must work in low-bandwidth rural environments, support hardware camera integration, and render complex medical and animal data dashboards smoothly on entry-level devices.

**Alternatives Considered:**
- React Native — strong ecosystem, but bridge overhead and hardware integration complexity
- Native Android (Kotlin) + iOS (Swift) — maximum performance, but 2× development cost for 1 developer
- Flutter — single codebase, Impeller rendering engine, strong plugin ecosystem

**Decision:** Flutter with Material 3, Riverpod state management, and GoRouter declarative routing.

**Reasoning:** Single codebase efficiency (95%+ code reuse), predictable 60 FPS rendering via Impeller, compile-safe state management via Riverpod, and declarative route guards matching our RBAC model.

**Reference:** [ADR-001](../architecture/adr/ADR-001.md)

**Future Implications:** Dart expertise required from all mobile contributors. Binary size is slightly larger than native (~25 MB baseline).

---

### [ADL-002] — 2026-07-27 — Spring Boot 3 + PostgreSQL Selected for Backend

**Context:** Vetra requires a production-quality REST API serving both Farmer and Veterinarian roles with JWT authentication, relational data, geospatial queries, and future extensibility.

**Alternatives Considered:**
- Node.js / Express — lighter, but weaker type safety and less mature enterprise ecosystem
- Django REST Framework — good defaults, but Python team expertise not available
- Spring Boot — mature, enterprise-grade, strong security ecosystem (Spring Security)

**Decision:** Spring Boot 3 (Java 17), PostgreSQL 15, Flyway for migrations, Spring Security + JWT.

**Reasoning:** Spring Boot's production maturity, Spring Security's RBAC integration, and PostgreSQL's geospatial extensions (PostGIS) make it the optimal choice for a healthcare platform expected to handle sensitive data at scale.

**Reference:** [ADR-002](../architecture/adr/ADR-002.md)

**Future Implications:** Java 17+ baseline. Spring Boot 3 requires Jakarta EE namespace (not `javax.*`).

---

### [ADL-003] — 2026-07-27 — Clean Architecture Adopted Across All Layers

**Context:** With a growing feature set (Auth, Animals, Appointments, Medical Records, Disease Reporting), the codebase needed a structural approach that prevents coupling between business logic and infrastructure.

**Decision:** Clean Architecture with strict layer dependency rules:
- Presentation → Application → Domain ← Infrastructure
- DTOs cross boundaries; entities do not leave the domain layer
- Repository interfaces defined in domain, implemented in infrastructure

**Reasoning:** Enables independent testing of business logic, facilitates future migration from monolith to microservices, and prevents "god service" anti-patterns.

**Reference:** [ADR-003](../architecture/adr/ADR-003.md)

**Future Implications:** All new feature modules must follow this structure. Reviewers must reject PRs that violate layer boundaries.

---

### [ADL-004] — 2026-07-27 — JWT with Refresh Token Strategy for Authentication

**Context:** Vetra serves field veterinarians who may be offline for hours and farmers who access the platform intermittently. Authentication must be persistent but also revocable.

**Decision:** Short-lived JWT access tokens (15-minute expiry) with long-lived refresh tokens (7-day expiry) stored in the `refresh_tokens` table. Token rotation on each refresh.

**Reasoning:** Short access token lifetime limits the blast radius of token compromise. Refresh tokens are database-backed, allowing revocation. Token rotation invalidates refresh tokens on reuse, preventing replay attacks.

**Reference:** [ADR-004](../architecture/adr/ADR-004.md)

**Future Implications:** Logout must invalidate the refresh token server-side. Refresh token table must be pruned regularly (expired tokens).

---

### [ADL-005] — 2026-07-27 — Docker + Docker Compose for Local Development

**Context:** New contributors needed a reproducible local environment without manual PostgreSQL installation, environment configuration, or version conflicts.

**Decision:** Docker Compose for local development with `docker-compose.dev.yml` providing PostgreSQL 15 + pgAdmin. Application runs on host JVM via `./mvnw spring-boot:run`.

**Reasoning:** Eliminates "works on my machine" issues for database setup. Application runs on host (not in container) during development for faster iteration with hot reload.

**Reference:** [ADR-005](../architecture/adr/ADR-005.md)

**Future Implications:** CI/CD pipeline should use the same Docker Compose for integration testing. Production deployment requires a separate Compose or Kubernetes configuration.

---

### [ADL-006] — 2026-07-27 — Dual-Role Architecture (Farmer / Veterinarian)

**Context:** Vetra serves two distinct user types with fundamentally different workflows, data access patterns, and UI requirements. Early prototypes had a single user type, which proved unworkable.

**Decision:** Hard separation of roles via `role` field on the `users` table (`FARMER`, `VET`). Separate profile tables (`farmer_profiles`, `vet_profiles`). Role-specific API responses from the `/api/v1/dashboard` endpoint. Role-specific Flutter navigation stacks guarded by GoRouter.

**Reasoning:** A veterinarian should never see a farmer's dashboard and vice versa. Mixing roles in a single UI creates confusion and security risks. Separate profile tables allow each role to carry role-specific attributes without nullable columns on a shared table.

**Future Implications:** Any new role (e.g., `ADMIN`, `REGULATOR`) requires a new profile table, a new role enum value, and new route guards on both frontend and backend.

---

### [ADL-007] — 2026-07-28 — Medical Records Are Immutable

**Context:** Electronic veterinary medical records are legal clinical documents. A record created for a completed appointment must be a permanent, unalterable statement of what was observed, diagnosed, and treated.

**Decision:** No `PUT` or `DELETE` endpoints for `MedicalRecord`. Once created, a record can only be read. The `version` column exists for optimistic locking during creation only. Future corrections create a new record linked to a new appointment (amended visit).

**Reasoning:** Medical immutability protects both farmers (cannot have records altered retroactively) and veterinarians (cannot be accused of record falsification). Aligns with standard electronic health record (EHR) regulatory requirements.

**Future Implications:** A correction workflow will need to be designed as a separate feature: an "amendment" record type linked to an original record, with an explicit amendment reason.

---

### [ADL-008] — 2026-07-28 — Appointment State Machine with Optimistic Locking

**Context:** Appointments transition through states: `PENDING → CONFIRMED → COMPLETED/CANCELLED`. Concurrent updates (farmer cancels while vet confirms) could result in data inconsistency.

**Decision:** Appointment state transitions are enforced by a state machine in `AppointmentService`. The `appointments` table has a `version BIGINT` column. All updates use Spring Data JPA's `@Version` for optimistic locking. Concurrent conflicting updates return `409 CONFLICT`.

**Reasoning:** Optimistic locking is appropriate for an appointment system where concurrent updates are uncommon but possible. It avoids pessimistic database locks that would reduce throughput.

**Future Implications:** If appointment load grows significantly, consider moving to an event-sourced appointment aggregate to handle concurrent access patterns more cleanly.

---

### [ADL-009] — 2026-07-28 — Single Record Per Appointment Constraint

**Context:** A medical record is created for a completed appointment. It should not be possible to create two records for the same appointment (e.g., due to double-submit on a slow network).

**Decision:** `appointment_id` in `medical_records` is `UNIQUE`. The backend returns `409 CONFLICT` when a record for an appointment already exists. The Flutter client shows an informational message and navigates to the existing record.

**Reasoning:** Database-level uniqueness constraint is the strongest guarantee — it cannot be bypassed by a race condition or a bug in the service layer.

**Future Implications:** No impact expected. If multi-record workflows are needed (e.g., follow-up records on the same appointment), a new `visit_records` table should be introduced.

---

### [ADL-010] — 2026-07-29 — Modular Monolith Architecture (Not Microservices)

**Context:** Vetra is currently a single-developer project in active development. Microservices introduce operational complexity (service discovery, distributed tracing, network latency, independent deployments) that is unjustifiable at the current team size and traffic volume.

**Decision:** Spring Boot monolith with strict module boundaries (`auth`, `animal`, `appointment`, `medicalrecord`, `dashboard`). Each module is independently testable and has no direct imports from other modules' internal classes. Shared infrastructure lives in `infrastructure/`.

**Reasoning:** A modular monolith delivers the architectural clarity of microservices (enforced boundaries, independent concerns) without the operational overhead. Modules can be extracted to separate services when team size and traffic justify it.

**Reference:** [docs/architecture/08-modular-monolith.md](../architecture/08-modular-monolith.md)

**Future Implications:** Module extraction is viable when a module's development velocity is constrained by other modules, or when a module needs independent scaling. See the modular monolith document for the target microservices architecture.

---

### [ADL-011] — 2026-07-29 — Repository Separation (Flutter + Backend as Independent Git Repos)

**Context:** Both projects were initially pushed to the same GitHub repository (`omrajput14/vetra`), creating a mixed repository that confused contributors and violated standard project organisation.

**Decision:** Create `omrajput14/vetra-backend` as a completely independent GitHub repository. Rewrite Flutter history using `git-filter-repo` to remove build artifacts (171 MB zip-cache blob, 77 MB APK) that were accidentally committed and blocked all future pushes.

**Reasoning:** Independent repositories allow independent versioning, independent CI/CD pipelines, independent access control, and clear ownership boundaries. The history rewrite was necessary because a 171 MB file exceeded GitHub's hard limit and blocked all pushes.

**Future Implications:** Each repository has its own version scheme. Cross-repo coordination (e.g., API contract changes) must be documented in both repositories. Breaking API changes must be communicated to the Flutter team via the API versioning document.

---

## Template for New Entries

When adding a new decision, copy and complete this template:

```markdown
### [ADL-XXX] — YYYY-MM-DD — <Short Decision Title>

**Context:** What situation or problem prompted this decision?

**Alternatives Considered:**
- Option A — description and why it was rejected
- Option B — description and why it was rejected

**Decision:** What was decided?

**Reasoning:** Why was this decision made over the alternatives?

**Reference:** Link to ADR document if one exists.

**Future Implications:** What does this decision constrain or enable in the future?
```
