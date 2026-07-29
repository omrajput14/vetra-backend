# Scalability & Performance Plan
**Document ID:** OPS-18  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Modular Monolith Architecture](../architecture/08-modular-monolith.md), [Database Design](../database/04-database-design.md), [Logging & Monitoring](./16-logging-monitoring.md)

---

## Overview

This document defines Vetra's performance targets, current architecture's capacity estimates, and the scaling approach as user growth progresses.

---

## Performance Targets

| Metric | Target | Measurement |
|---|---|---|
| API response time (p50) | < 100 ms | All authenticated endpoints |
| API response time (p99) | < 500 ms | All authenticated endpoints |
| API response time (p99.9) | < 2000 ms | Under peak load |
| Database query time (p99) | < 100 ms | All queries |
| Authentication (login/refresh) | < 200 ms | Including bcrypt verification |
| Throughput (concurrent users) | 500 CCU | Initial production target |
| Availability | 99.9% | 8.7 hours downtime/year |

---

## Current Architecture Capacity

### Spring Boot (Single Instance)

| Resource | Specification | Bottleneck |
|---|---|---|
| vCPU | 2 | CPU-bound under bcrypt load |
| RAM | 2 GB | JVM heap ~512 MB typical |
| Threads (Tomcat default) | 200 concurrent | Request queue if exceeded |
| Connections (HikariCP) | 10 pool | DB contention if exceeded |

**Estimated capacity:** ~100–200 concurrent users without database being the bottleneck.

### PostgreSQL (Single Instance — Local Docker)

| Resource | Specification | Bottleneck |
|---|---|---|
| vCPU | Shared (local) | CPU-bound on complex queries |
| RAM | Shared (local) | Shared buffer cache |
| Connections | 100 (PostgreSQL default) | HikariCP pool manages this |

**Estimated capacity:** Suitable for development. Production requires managed RDS.

---

## Scaling Tiers

### Tier 1: 0–500 Daily Active Users (Current)

**Architecture:** Single Spring Boot instance + single PostgreSQL  
**Action Required:** None — current architecture is sufficient  
**Monitoring Threshold:** Alert when p99 > 500 ms or CPU > 70% sustained

### Tier 2: 500–5,000 Daily Active Users (Planned)

**Architecture Changes:**
1. Upgrade to RDS `db.t3.medium` (2 vCPU, 4 GB RAM)
2. Add 1 additional Spring Boot instance behind ALB
3. Increase HikariCP pool size to 20
4. Enable PostgreSQL query logging for slow queries (> 100 ms)
5. Add read replica for analytics/dashboard queries

**Key Queries to Optimize:**
- `GET /api/v1/animals` — add composite index on `(farmer_id, created_at DESC)`
- `GET /api/v1/appointments` — add composite index on `(farmer_id, status, appointment_date)`
- `GET /api/v1/animals/{id}/medical-history` — already indexed on `animal_id`

### Tier 3: 5,000–50,000 Daily Active Users (Future)

**Architecture Changes:**
1. Begin module extraction (see [Modular Monolith](../architecture/08-modular-monolith.md))
2. Implement response caching (Redis) for:
   - Vet directory (`GET /api/v1/auth/vets`) — cache 60 seconds
   - Dashboard metrics — cache 5 minutes per user
3. Database connection pooling via PgBouncer
4. CDN for static Flutter app assets
5. Horizontal scaling: 3–5 application instances

### Tier 4: 50,000+ Daily Active Users (Future)

Full microservices extraction, event-driven architecture, read replicas, CQRS for medical history queries.

---

## Performance Optimisation Inventory

### Database Optimisations Implemented

| Optimisation | Tables | Benefit |
|---|---|---|
| Index on every FK | All tables | Eliminates FK join scans |
| Index on `status` | `appointments` | Fast filtering by appointment state |
| Index on `appointment_date` | `appointments` | Fast date range queries |
| Index on `is_available` | `vet_profiles` | Fast vet directory filtering |
| Index on `animal_id` | `medical_records` | Fast medical history retrieval |
| Optimistic locking | `appointments`, `medical_records` | Prevents concurrent update conflicts |

### Database Optimisations Planned

| Optimisation | When | Rationale |
|---|---|---|
| Composite index `(farmer_id, status)` on `appointments` | Tier 2 | Dashboard appointment list query |
| Partial index `WHERE is_available = TRUE` on `vet_profiles` | Tier 2 | Available vet directory query |
| VACUUM ANALYZE schedule | Production | Prevent table bloat |
| `pg_stat_statements` extension | Production | Query performance analysis |

### Application Optimisations

| Optimisation | Status | Description |
|---|---|---|
| HikariCP connection pool | Implemented | 10 connections, tunable via env |
| DTO projection | Implemented | Only selected columns fetched (not full entities) |
| Pagination | Planned | All list endpoints should support cursor-based pagination |
| Async logging | Planned | Logback AsyncAppender prevents logging from blocking request threads |
| Response compression | Planned | Gzip compression for responses > 1 KB |

---

## Load Testing Plan (Planned)

Before production launch, load tests must be performed using **Gatling** or **k6**:

| Test | Scenario | Pass Condition |
|---|---|---|
| Baseline | 100 CCU for 10 min | p99 < 500 ms, 0 errors |
| Ramp | 0 → 500 CCU over 5 min | p99 < 2000 ms, error rate < 1% |
| Spike | 0 → 1000 CCU in 30 sec | System recovers within 60 sec |
| Soak | 200 CCU for 2 hours | No memory leak, p99 stable |

Test results must be committed to `docs/operations/load-test-results/` with date, config, and summary.

---

## SLA Definition

| Tier | Monthly Uptime | Allowed Downtime |
|---|---|---|
| Development | Best effort | N/A |
| Staging | 95% | 36 hours/month |
| Production | 99.9% | 8.7 hours/year |

Downtime is defined as `GET /actuator/health` returning non-200 for more than 30 consecutive seconds from at least 2 monitoring locations.
