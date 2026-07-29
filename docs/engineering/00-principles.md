# Vetra Engineering Principles
**Document ID:** ENG-00  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** All Vetra repositories — `omrajput14/vetra`, `omrajput14/vetra-backend`

---

## Purpose

This document is the **engineering constitution of Vetra**. It defines the non-negotiable principles, philosophies, and standards that govern every technical decision made on this platform.

Every engineer — full-time, part-time, or contractor — is expected to read, understand, and adhere to these principles from day one.

When a technical decision conflicts with these principles, the decision must be revisited. When a principle requires updating, it must go through a formal architectural review and be documented in the Architecture Decision Log ([`docs/domain/21-decision-log.md`](../domain/21-decision-log.md)).

---

## 1. Engineering Philosophy

### 1.1 Build for the Farmer, Not for the Engineer

Vetra exists to solve real problems for rural farmers and field veterinarians, many of whom operate on low-bandwidth networks, entry-level Android devices, and with minimal digital literacy. Every technical decision must be evaluated against this constraint.

- Prefer offline-capable features over real-time synchronization.
- Prefer simple, fast interfaces over feature-rich, complex UIs.
- Prefer explicit error messages over cryptic codes.

### 1.2 Correctness Over Cleverness

Code that is correct and readable is always preferred over code that is clever or concise. If a reviewer cannot understand a block of code within 60 seconds, it must be refactored.

### 1.3 Explicit Over Implicit

Configuration, authorization, and business logic must be explicit. Magic is a maintenance liability.

- Prefer explicit dependency injection over service locators.
- Prefer explicit authorization annotations over global catch-alls.
- Prefer explicit error types over generic exceptions.

### 1.4 Fail Fast and Loudly

Bugs discovered in development cost 10× less than bugs discovered in production. Systems must validate assumptions at their boundaries — API inputs, database reads, external service responses — and fail with clear, actionable error messages rather than producing silent incorrect results.

### 1.5 Ownership

Every module, service, and document has a clear owner. Ownerless code is unmaintained code. When you create something, you own it until ownership is explicitly transferred.

---

## 2. Clean Architecture

All Vetra software — both the Flutter mobile app and the Spring Boot backend — is structured following **Clean Architecture** principles.

### 2.1 Layer Boundaries

```
Presentation Layer     ← UI, Controllers, HTTP handlers
      │
Application Layer      ← Use cases, Services, DTOs
      │
Domain Layer           ← Entities, Business Rules, Repository interfaces
      │
Infrastructure Layer   ← Database, External APIs, Frameworks
```

**The Dependency Rule:** Source code dependencies must point *inward only*. The domain layer must never import from the infrastructure layer. Frameworks and databases are implementation details — the domain does not know about them.

### 2.2 Rules

1. Entities contain business rules only. They have no knowledge of HTTP, databases, or UI.
2. Services orchestrate use cases. They do not contain persistence logic.
3. Repositories are interfaces defined in the domain layer and implemented in the infrastructure layer.
4. DTOs cross layer boundaries. Entities must not be serialized directly to API responses.
5. No `@Autowired` field injection — use constructor injection exclusively.

### 2.3 Module Boundaries in Flutter

Each feature in `lib/features/` contains exactly three layers:
```
features/<name>/
├── data/        ← API services, DTOs, repository implementations
├── domain/      ← Repository interfaces
└── presentation/ ← Pages, widgets, Riverpod providers
```

Cross-feature communication is mediated through shared providers, never through direct widget coupling.

---

## 3. SOLID Principles

These are not aspirational — they are requirements.

| Principle | Requirement |
|---|---|
| **Single Responsibility** | Every class, function, and widget has one reason to change. A `UserService` that also sends emails violates SRP. |
| **Open/Closed** | Extend through interfaces and composition, not modification. Changing existing code to add a feature is a design smell. |
| **Liskov Substitution** | Subtypes must be substitutable for their base types without altering program correctness. |
| **Interface Segregation** | Prefer narrow, focused interfaces over wide, general ones. A client must not depend on methods it does not use. |
| **Dependency Inversion** | High-level modules depend on abstractions. Low-level modules implement abstractions. Both depend on interfaces, never concrete classes. |

---

## 4. Domain-Driven Design (DDD)

Vetra's domain is veterinary livestock healthcare. The software must speak the language of that domain.

### 4.1 Ubiquitous Language

All code, documentation, variable names, and API endpoints must use the domain's language. Domain terms are defined in [`docs/domain/03-domain-model.md`](../domain/03-domain-model.md).

**Correct:** `AnimalPassport`, `MedicalRecord`, `VetProfile`, `AppointmentStatus`  
**Incorrect:** `UserData`, `RecordEntity`, `DoctorInfo`, `StatusCode`

### 4.2 Bounded Contexts

Each feature module represents a bounded context. Terms may have different meanings in different contexts, and that is acceptable — as long as each context's definition is explicit.

### 4.3 Aggregates and Invariants

Business invariants are enforced at the aggregate root level, not in services or controllers. For example:
- A `MedicalRecord` cannot exist without a `COMPLETED` `Appointment` — this is enforced by `MedicalRecordService`, not by a database trigger.
- An `Animal` always belongs to exactly one `FarmerProfile` — this is enforced at creation time, not assumed.

---

## 5. Documentation-First Development

> **Code without documentation is considered incomplete.**

### 5.1 The Documentation-First Sequence

Before writing production code for any new module:

1. **Define the business requirement** — What user problem does this solve?
2. **Design the architecture** — Which modules are affected? What are the dependencies?
3. **Design the database** — What tables, columns, and constraints are required?
4. **Design the APIs** — What endpoints, request/response shapes, and authorization rules?
5. **Update all affected documents** — PRD, SAD, domain model, ERD, API spec, decision log.
6. **Review for consistency** — Does this design conflict with existing decisions?
7. **Only then begin implementation.**

### 5.2 What Must Always Be Documented

| Artifact | Document |
|---|---|
| New API endpoint | `docs/api/06-specification.md` |
| New database table | `docs/database/04-database-design.md` and `05-ERD.md` |
| New domain entity | `docs/domain/03-domain-model.md` |
| Architectural decision | `docs/domain/21-decision-log.md` + ADR |
| Breaking change | `docs/api/22-versioning.md` |
| New error code | `docs/api/23-error-catalogue.md` |
| New environment variable | `docs/operations/24-environment-config.md` |
| New feature module | `docs/architecture/02-SAD.md` |

### 5.3 Documentation Must Stay In Sync

Stale documentation is worse than no documentation. When code changes, the corresponding document must change in the same commit. A PR that modifies business logic without updating documentation is incomplete and must not be merged.

---

## 6. API Standards

Full API standards are documented in [`docs/api/06-specification.md`](../api/06-specification.md) and [`docs/api/22-versioning.md`](../api/22-versioning.md). Summary:

- All APIs are versioned from the first release: `/api/v1/...`
- All requests and responses use `application/json`.
- HTTP verbs must semantically match the operation: `GET` reads, `POST` creates, `PUT` replaces, `PATCH` partially updates, `DELETE` removes.
- All responses use consistent envelope structure (see error catalogue).
- All timestamps are ISO 8601 UTC.
- All identifiers are UUIDs.
- Pagination is cursor-based for collections.
- Deprecation requires a minimum 3-month notice period.

---

## 7. Database Standards

Full standards in [`docs/database/04-database-design.md`](../database/04-database-design.md).

- All schema changes use Flyway migrations. Direct DDL changes to the database are prohibited.
- Every migration is idempotent where possible (`CREATE TABLE IF NOT EXISTS`).
- All primary keys are UUID v4 (`uuid_generate_v4()`).
- All timestamps include timezone (`TIMESTAMP WITH TIME ZONE`).
- Every table has `created_at` and `updated_at`.
- Tables using optimistic locking have a `version BIGINT NOT NULL DEFAULT 0` column.
- No `ON DELETE CASCADE` without explicit review and documentation.
- Indexes are created for every foreign key and every column used in `WHERE` clauses.

---

## 8. Git Standards

Full standards in [`docs/engineering/13-git-workflow.md`](./13-git-workflow.md).

- All commits follow **Conventional Commits** format: `type(scope): description`
- Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`
- All feature work happens on `feature/<name>` branches.
- Merges to `main` require a Pull Request with at minimum one reviewer.
- Commit messages must be in the imperative mood: "Add endpoint" not "Added endpoint".
- No force-push to `main` except for emergency security patches (requires documentation in decision log).
- Every new feature branch is created from the latest `main`.

---

## 9. Security Principles

Full security design in [`docs/security/11-security-design.md`](../security/11-security-design.md).

- **Authentication** is JWT-based with short-lived access tokens (15 minutes) and long-lived refresh tokens (7 days).
- **Authorization** is enforced server-side on every request. The client's claimed identity is never trusted.
- **Ownership** is always verified: a farmer cannot access another farmer's data, a veterinarian cannot create records for another veterinarian's appointments.
- **Secrets** are never hardcoded. All secrets live in environment variables documented in the environment configuration document.
- **Input validation** occurs at the API boundary — never assume incoming data is safe.
- **SQL injection** is prevented through parameterized queries only. String concatenation in SQL is prohibited.
- **Sensitive data** (passwords, tokens, personal information) is never logged.

---

## 10. Testing Principles

Full strategy in [`docs/guides/14-testing-strategy.md`](../guides/14-testing-strategy.md).

### 10.1 Testing Pyramid

```
         ╱─────────╲
        ╱  E2E Tests ╲        Few, slow, high confidence
       ╱───────────────╲
      ╱ Integration Tests╲    Moderate, test module boundaries
     ╱─────────────────────╲
    ╱     Unit Tests        ╲  Many, fast, test isolated logic
   ╱─────────────────────────╲
```

### 10.2 Rules

- Unit tests cover all service-layer business logic.
- Every public service method has at least one unit test.
- Integration tests cover every API endpoint for happy path and primary error paths.
- Tests must be deterministic — no random data, no time-dependent behavior without mocking.
- Test class names follow the pattern `<SubjectUnderTest>Test` or `<SubjectUnderTest>Spec`.
- Test method names must be descriptive: `shouldReturn409WhenDuplicateMedicalRecordCreated()`.

---

## 11. Code Review Rules

### 11.1 What Reviewers Check

- [ ] Does the code satisfy the documented requirement?
- [ ] Does it follow Clean Architecture layer boundaries?
- [ ] Are SOLID principles respected?
- [ ] Is the ubiquitous language used correctly?
- [ ] Is the error handling explicit and meaningful?
- [ ] Is sensitive data protected (not logged, not exposed in responses)?
- [ ] Are unit tests included and meaningful?
- [ ] Has documentation been updated?
- [ ] Is the `.gitignore` protecting generated files?

### 11.2 Author Responsibilities Before Review

- Self-review the diff before requesting review.
- Ensure all tests pass locally.
- Ensure `flutter analyze` / `./mvnw checkstyle:check` passes.
- Describe *why* the change was made, not just *what* it does.

### 11.3 Review Turnaround

- Reviews must be completed within **24 hours** on business days.
- Blocking comments must be resolved before merge.
- Non-blocking suggestions are marked with `nit:` prefix.

---

## 12. Naming Conventions

### Backend (Java/Spring)

| Artifact | Convention | Example |
|---|---|---|
| Package | lowercase, domain-driven | `app.vetra.medicalrecord` |
| Class | PascalCase | `MedicalRecordService` |
| Interface | PascalCase (no `I` prefix) | `MedicalRecordRepository` |
| Method | camelCase, verb-first | `createMedicalRecord()` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Variable | camelCase | `veterinarianId` |
| Database table | snake_case, plural | `medical_records` |
| Database column | snake_case | `appointment_id` |
| REST endpoint | kebab-case, plural resource nouns | `/api/v1/medical-records` |
| DTO suffix | `Request` for input, `Response` for output | `CreateMedicalRecordRequest` |

### Flutter (Dart)

| Artifact | Convention | Example |
|---|---|---|
| File | snake_case | `medical_record_page.dart` |
| Class | PascalCase | `MedicalRecordPage` |
| Variable/method | camelCase | `medicalRecordProvider` |
| Constant | camelCase with `k` prefix | `kPrimaryColor` |
| Riverpod provider | camelCase, `Provider` suffix | `medicalRecordProvider` |
| Widget file | matches class name | `vet_card.dart` → `VetCard` |

---

## 13. Migration Policy

Every database migration must satisfy:

1. **Versioned** — follows Flyway naming: `V{n}__{description}.sql`
2. **Idempotent** — uses `IF NOT EXISTS`, `IF EXISTS` guards
3. **Non-destructive by default** — dropping columns or tables requires explicit approval and a documented migration path
4. **Backwards-compatible** — a migration that breaks the currently deployed application version is a breaking change and must be deployed with the application update atomically
5. **Documented** — the migration's business rationale is described in [`docs/database/04-database-design.md`](../database/04-database-design.md)
6. **Tested** — the migration must be validated against a clean database before merging

---

## Document Maintenance

This document is reviewed:
- When a new engineering principle is proposed
- When an existing principle produces friction in practice
- At the start of each major development stage

All changes to this document require a corresponding entry in the Architecture Decision Log.

---

*This document is the single source of truth for engineering standards at Vetra. When in doubt, refer here first.*
