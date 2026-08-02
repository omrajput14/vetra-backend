# Changelog

All notable changes to the Vetra backend will be documented here.

## [Unreleased]
### Added — Stage 12.3.1: Redis Infrastructure Foundation
- **Package Architecture:** Created `app.vetra.infrastructure.redis` (`config/`, `properties/`).
- **Spring Boot Starters:** Added `spring-boot-starter-data-redis` and `spring-boot-starter-cache`.
- **Redis Configuration:** `RedisConfig` with `@EnableCaching`, `LettuceConnectionFactory`, `RedisTemplate<String, Object>` (with `GenericJackson2JsonRedisSerializer`), `StringRedisTemplate`, and transaction support.
- **Typed Configuration:** `RedisProperties` (`@ConfigurationProperties("vetra.redis")`) binding `host`, `port`, `password`, `database`, `timeout`.
- **Docker Compose Redis Container:** Integrated `redis:7.4-alpine` service with password authentication, `redis-cli ping` healthcheck, `redis_data` volume persistence, and service dependency ordering.
- **Actuator & Test Integration:** Exposed Redis health check via Actuator and created `RedisInfrastructureTest` integration test suite.

## [0.9.0] — 2026-08-01 — Stage 12: Production Infrastructure & CI/CD Platform
### Added
- **Dockerization (Stage 12.1):** Multi-stage production `Dockerfile` (Temurin 21 JRE Alpine, non-root system user `vetra`, OCI labels) and `docker-compose.yml` with PostGIS 17 database persistence and 12-Factor `STDOUT` logging.
- **CI/CD Platform (Stage 12.2):** GitHub Actions workflows (`ci.yml`, `codeql.yml`), Dependabot (`dependabot.yml`), Gitleaks secret scanner (`.gitleaks.toml`), CODEOWNERS, Issue templates, and PR template.
- **SpringDoc Upgrade:** Upgraded `springdoc-openapi-starter-webmvc-ui` from `2.8.9` to `2.8.15` (LTS 2.x production line for Spring Boot 3.x).
- **Operations Documentation:** `docs/operations/docker.md` and `docs/operations/ci-cd.md`.
- **Infrastructure Freeze:** Baseline frozen on Java 21 LTS, Spring Boot 3.5.3, SpringDoc 2.8.15, and PostGIS 17.

## [0.7.0] — 2026-07-28 — Stage 7: Electronic Veterinary Medical Records (EVMR)
### Added
- `V6__create_medical_records.sql` Flyway migration for `medical_records` table
- `MedicalRecord` JPA entity with optimistic locking (`@Version`)
- `MedicalRecordController` — `POST /api/v1/medical-records`, `GET /api/v1/medical-records/{id}`, `GET /api/v1/animals/{id}/medical-history`, `GET /api/v1/appointments/{id}/medical-record`
- Immutability enforcement — no PUT or DELETE endpoints for medical records
- Authorization: veterinarians can only create records for their own assigned appointments
- Single-record constraint per appointment (`409 CONFLICT` on duplicate)
- `medicalRecordsCreatedCount` metric added to Veterinarian dashboard
- `MedicalRecordServiceTest` — 5 unit tests, all passing

## [0.6.0] — 2026-07-27 — Stage 6: Appointment Management
### Added
- `V5__create_appointments.sql` Flyway migration
- `AppointmentController` with full state machine workflow
- Appointment states: `PENDING → CONFIRMED → COMPLETED/CANCELLED`
- Optimistic locking on appointment updates
- `AppointmentServiceTest` — integration tests

## [0.5.0] — 2026-07-27 — Stage 5: Animal Management
### Added
- `AnimalController` — full CRUD for farmer-owned animals
- Species enum, animal name, gender, and age fields
- `AnimalServiceTest`

## [0.1.0–0.4.0] — Stages 1–4: Foundation, Auth, Profiles, Dashboard
### Added
- Spring Boot 3 project foundation with Maven, Checkstyle, Flyway
- JWT authentication with refresh token support
- Farmer and Veterinarian registration and login
- Role-based security via Spring Security
- Dashboard metrics endpoint
- Docker and docker-compose configuration
