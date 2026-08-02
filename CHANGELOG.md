# Changelog

All notable changes to the Vetra backend will be documented here.

## [0.9.4] — 2026-08-02 — Stage 12.3.4: Enterprise Cache Performance Benchmarking & Optimization
### Added
- **Empirical Benchmark Suite:** Version-controlled automated HTTP load driver (`scripts/benchmark/benchmark_suite.py`) testing concurrent read throughput ($C=20$, 1,500 total requests).
- **Performance Report:** [`docs/performance/cache-benchmark-report.md`](file:///Users/0mrajput/vetra-backend/docs/performance/cache-benchmark-report.md) with complete audit matrix, evidence sources, and step-by-step reproducibility guide.
- **Empirical Metrics:** 
  - **99.62% Cache Hit Ratio** across 3,136 total Redis keyspace operations.
  - **0 SQL statements executed** during warm-cache requests (100% query bypass).
  - **Peak Throughput:** 1,923.6 req/sec (Notifications), 1,851.2 req/sec (Profile), 1,409.4 req/sec (Farmer Dashboard).
  - **Redis Latency:** $2.93\ \mu\text{s}$ average GET command latency.
  - **Memory Footprint:** 1.33 MB peak memory usage with 0 evicted keys and 0 slowlog entries.
- **CI/CD Hardening:** Updated GitHub Actions `build-and-test` job to start Redis service container (`redis:7-alpine`) with health check and environment variable authentication.

## [0.9.3] — 2026-08-02 — Stage 12.3.3: Enterprise Cache Layer
### Added
- **Spring Cache Implementation:** Configured `RedisCacheManager` in `CacheConfiguration` with region-specific TTLs (5 min dashboards to 24 hours AI diagnosis/analytics).
- **Service Caching:** Applied `@Cacheable` and `@CacheEvict` annotations across `DashboardService`, `AuthService`, `AnimalService`, `AppointmentService`, `MedicalRecordService`, `NotificationService`, `DiseaseService`, and `DiseaseRegistryService`.
- **Jackson Polymorphic Serialization:** `GenericJackson2JsonRedisSerializer` with `JavaTimeModule` for high-performance DTO JSON serialization.
- **Resilient Degradation:** Implemented custom `CacheErrorHandler` in `CacheConfiguration` for graceful database fallback when Redis is unreachable.

## [0.9.1] — 2026-08-01 — Stage 12.3.1 & 12.3.2: Redis Infrastructure & Cache Architecture
### Added
- **Redis Infrastructure:** Added `spring-boot-starter-data-redis` and `spring-boot-starter-cache`. Created `RedisConfig` and `RedisProperties`.
- **Cache Architecture Specification:** Documented Enterprise Cache Architecture (`docs/architecture/25-caching-architecture.md`) with region TTL policies, key conventions, and invalidation rules.

## [0.9.0] — 2026-08-01 — Stage 12: Production Infrastructure & CI/CD Platform
### Added
- **Dockerization (Stage 12.1):** Multi-stage production `Dockerfile` (Temurin 21 JRE Alpine) and `docker-compose.yml` with PostGIS 17 database persistence.
- **CI/CD Platform (Stage 12.2):** GitHub Actions workflows (`ci.yml`, `codeql.yml`), Dependabot, Gitleaks secret scanner (`.gitleaks.toml`), CODEOWNERS.

## [0.7.0] — 2026-07-28 — Stage 7: Electronic Veterinary Medical Records (EVMR)
### Added
- `V6__create_medical_records.sql` Flyway migration for `medical_records` table.
- `MedicalRecord` JPA entity with optimistic locking (`@Version`).
- `MedicalRecordController` & `MedicalRecordService` implementation.

## [0.1.0–0.6.0] — Stages 1–6: Foundation, Auth, Profiles, Appointments, Animals
### Added
- Spring Boot 3 project foundation with Maven, Checkstyle, Flyway, JWT Auth, Animal QR passports, and Clinical Appointment state machine.
