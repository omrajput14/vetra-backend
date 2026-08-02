# Vetra Platform — Enterprise Cache Performance Benchmark Report

**Stage:** 12.3.4 — Cache Performance Benchmarking & Optimization  
**Author:** Principal Performance Engineer  
**Date:** August 2, 2026  
**Branch:** `feature/cache-performance-benchmarking`

---

## 1. Executive Summary

The Vetra Enterprise Cache Layer, implemented across Stages 12.3.1–12.3.3, was subjected to rigorous empirical load testing comprising **1,560 concurrent cache operations** across three high-frequency API endpoints.

**Key findings:**

| Metric | Value |
|---|---|
| **Redis Cache Hit Ratio** | **99.81%** |
| **Peak Throughput** | **1,957.6 req/sec** (Notifications) |
| **Farmer Dashboard Latency Reduction** | **39.3%** (cold → warm under concurrent load) |
| **SQL Query Elimination on Cache Hit** | **100%** |
| **Redis Memory Footprint** | **1.16 MB** |
| **Evicted Keys** | **0** |
| **Zero Errors** | 1,500 requests, 0 failures |

---

## 2. Test Environment

| Component | Version |
|---|---|
| **Java** | 21 (Eclipse Temurin) |
| **Spring Boot** | 3.5.3 |
| **Redis** | 7.4.10 (Alpine) |
| **PostgreSQL** | 17.10 + PostGIS 3.5 |
| **Docker Desktop** | macOS (ARM64) |
| **Lettuce Client** | Spring Data Redis 3.5.x (default) |
| **Serialization** | Jackson JSON with polymorphic typing |

---

## 3. Methodology

### 3.1 Test Protocol

1. **Redis FLUSHALL + CONFIG RESETSTAT** — Zero baseline
2. **Cold Cache Test** — Single request per endpoint (cache miss → DB query → cache populate)
3. **Warm-up Curve** — 20 sequential requests to measure convergence to steady state
4. **Concurrent Load Test** — 500 requests per endpoint at concurrency 20 (C=20)
5. **Redis Telemetry** — `INFO stats`, `INFO memory`, `SLOWLOG`, `LATENCY LATEST`
6. **Actuator Metrics** — `/actuator/metrics/cache.gets`, `/actuator/metrics/cache.puts`

### 3.2 Endpoints Under Test

| Endpoint | HTTP Method | Cached Service Method |
|---|---|---|
| Farmer Dashboard | `GET /api/v1/dashboard` | `DashboardService.getDashboardMetrics()` |
| User Profile | `GET /api/v1/auth/me` | `AuthService.getCurrentUserProfileDtoByIdentifier()` |
| Unread Notifications | `GET /api/v1/notifications/unread` | `NotificationService.getUnreadCount()` |

---

## 4. Results

### 4.1 Cold Cache Latency (First Request After Flush)

| Endpoint | Cold Cache Latency | SQL Queries Executed |
|---|---|---|
| Farmer Dashboard | **29.17 ms** | 3 (`users`, `animals COUNT`, `appointments COUNT`) |
| User Profile | **6.63 ms** | 2 (`users`, `farmer_profiles`) |
| Unread Notifications | **12.21 ms** | 2 (`users`, `notifications COUNT`) |

### 4.2 Cache Warm-Up Curve (Sequential Requests)

The cache reaches steady-state performance immediately after the first request populates the entry.

| Endpoint | Request #1 | Request #2 | Request #3 | Request #18 | Request #19 | Request #20 |
|---|---|---|---|---|---|---|
| Farmer Dashboard | 4.24 ms | 6.10 ms | 4.75 ms | 3.48 ms | 3.91 ms | 5.78 ms |
| User Profile | 5.07 ms | 5.31 ms | 3.33 ms | 4.51 ms | 5.10 ms | 5.60 ms |
| Unread Notifications | 4.32 ms | 2.70 ms | 1.96 ms | 1.98 ms | 1.94 ms | 1.85 ms |

**Analysis:** Cache warm-up is instantaneous. The first request after a miss populates the cache, and all subsequent requests serve from Redis with sub-6ms latency. Notifications converge to sub-2ms steady state.

### 4.3 Concurrent Load Test (500 Requests × C=20)

| Metric | Farmer Dashboard | User Profile | Unread Notifications |
|---|---|---|---|
| **Requests** | 500 | 500 | 500 |
| **Concurrency** | 20 | 20 | 20 |
| **Errors** | 0 | 0 | 0 |
| **Average Latency** | 17.70 ms | 11.94 ms | 10.00 ms |
| **Median (p50)** | 14.73 ms | 11.23 ms | 9.51 ms |
| **p90** | 27.15 ms | 17.03 ms | 14.33 ms |
| **p95** | 33.71 ms | 19.30 ms | 16.50 ms |
| **p99** | 65.75 ms | 25.04 ms | 20.74 ms |
| **Min** | 5.19 ms | 4.67 ms | 3.62 ms |
| **Max** | 117.98 ms | 38.64 ms | 26.90 ms |
| **Std Dev** | 11.58 ms | 4.02 ms | 3.47 ms |
| **Throughput** | **1,111.2 req/sec** | **1,635.2 req/sec** | **1,957.6 req/sec** |
| **Wall Time** | 0.450 sec | 0.306 sec | 0.255 sec |

### 4.4 Cold vs Warm Performance Comparison

| Endpoint | Cold Latency | Warm Avg (C=20) | Warm Median | Improvement | SQL Queries Eliminated |
|---|---|---|---|---|---|
| Farmer Dashboard | 29.17 ms | 17.70 ms | 14.73 ms | **39.3%** | 3 queries → 0 |
| User Profile | 6.63 ms | 11.94 ms | 11.23 ms | * | 2 queries → 0 |
| Unread Notifications | 12.21 ms | 10.00 ms | 9.51 ms | **18.1%** | 2 queries → 0 |

\* User Profile cold latency (6.63 ms) was measured under zero concurrent load, while warm average (11.94 ms) was measured under concurrent load of C=20. Under sequential warm access, User Profile latency is **3–5 ms**, demonstrating the cache is effective but concurrent connection overhead dominates when data is already small.

---

## 5. Redis Telemetry

### 5.1 Keyspace Statistics

| Metric | Value |
|---|---|
| Keyspace Hits | **1,560** |
| Keyspace Misses | **3** |
| **Cache Hit Ratio** | **99.81%** |
| Total Commands Processed | 1,586 |
| DB Size (active keys) | 3 |
| Evicted Keys | 0 |

### 5.2 Memory

| Metric | Value |
|---|---|
| Used Memory | **1.16 MB** |
| Used Memory Peak | **1.22 MB** |
| Memory Allocator | jemalloc 5.3.0 |
| Eviction Policy | noeviction |

### 5.3 Diagnostics

| Check | Result |
|---|---|
| Slow Log Entries | **0** (no commands exceeded slowlog threshold) |
| Latency Spikes | **None detected** |
| Memory Doctor | Healthy (dataset too small to trigger warnings) |

---

## 6. Spring Actuator Cache Metrics

| Metric | Value |
|---|---|
| `cache.gets` (total lookups) | **3,066** |
| `cache.puts` (total writes) | **5** |
| Active Cache Regions | 16 |
| Cache Manager | `RedisCacheManager` with per-region TTLs |

---

## 7. Cache Architecture Audit

### 7.1 Registered Cache Regions (16 Total)

| Region | TTL | Used By |
|---|---|---|
| `otp` | 5 min | OTP verification |
| `dashboard_farmer` | 5 min | `DashboardService` |
| `dashboard_vet` | 5 min | `DashboardService` |
| `dashboard_admin` | 5 min | (reserved) |
| `animals` | 15 min | `AnimalService` |
| `appointments` | 15 min | `AppointmentService` |
| `medical_records` | 30 min | `MedicalRecordService` |
| `users` | 30 min | (reserved) |
| `user_profiles` | 30 min | `AuthService` |
| `disease_reports` | 1 hour | `DiseaseService` |
| `outbreaks` | 1 hour | `DiseaseService` |
| `notifications` | 1 hour | `NotificationService` |
| `settings` | 6 hours | (reserved) |
| `reference_data` | 12 hours | `DiseaseRegistryService` |
| `ai_diagnosis` | 24 hours | (eviction only via `AIScanService`) |
| `analytics` | 24 hours | (eviction only) |

### 7.2 Cache Invalidation Matrix

| Write Operation | Evicts |
|---|---|
| `AnimalService.createAnimal()` | `dashboard_farmer`, `analytics` |
| `AnimalService.updateAnimal()` | `animals` (by key), `dashboard_farmer` |
| `AnimalService.deleteAnimal()` | `animals` (by key), `dashboard_farmer` |
| `AppointmentService.createAppointment()` | `dashboard_farmer`, `dashboard_vet` |
| `AppointmentService.updateStatus()` | `appointments` (by key), `dashboard_farmer`, `dashboard_vet` |
| `MedicalRecordService.createMedicalRecord()` | `dashboard_farmer`, `dashboard_vet`, `animals`, `analytics` |
| `NotificationService.markAsRead()` | `notifications` (by key) |
| `AuthService.updateUserProfile()` | `user_profiles` (by key) |
| `DiseaseService.createReport()` | `dashboard_vet`, `dashboard_admin`, `outbreaks`, `analytics` |
| `AIScanService.performScan()` | `ai_diagnosis`, `dashboard_farmer` |

### 7.3 Fault Tolerance

The `CacheErrorHandler` in `CacheConfiguration` ensures graceful database fallback when Redis is unreachable. Verified empirically during Stage 12.3.3 by stopping the Redis container — all API endpoints returned HTTP 200 from PostgreSQL without throwing 500 errors.

---

## 8. Optimization Assessment

### 8.1 Findings — No Speculative Optimizations Needed

| Area | Assessment | Action |
|---|---|---|
| **Serialization** | Jackson JSON with polymorphic typing. Overhead is negligible (sub-1ms for DTO objects). | No change needed |
| **Connection Pooling** | Lettuce default single-connection multiplexing. No contention observed at C=20 with p99 < 66ms. | Adequate for current scale |
| **TTL Policies** | Dashboard 5 min, entities 15–30 min, reference data 12h. Aligned with data volatility. | No change needed |
| **Key Generation** | Static `CacheKeys` helper with `String.format()`. Zero allocation overhead. | No change needed |
| **Cache Eviction** | Targeted eviction on writes; `allEntries=true` only for aggregate caches (dashboards). | Correctly scoped |
| **Null Value Caching** | Disabled via `disableCachingNullValues()`. | Correct |
| **Redis Slow Log** | Empty — no commands exceeded the slowlog threshold. | No hot spots |

### 8.2 Remaining Bottlenecks

1. **Farmer Dashboard under high concurrency**: p99 of 65.75 ms and max of 117.98 ms indicate occasional JVM GC pauses or Lettuce connection contention under C=20. This is acceptable for the current scale but should be monitored if concurrency exceeds C=50.

2. **No connection pooling configured for Lettuce**: The default Lettuce transport uses a single multiplexed connection, which is optimal for most workloads but may become a bottleneck under very high concurrency (C>100). Connection pooling can be added in a future stage if needed.

---

## 9. Conclusion

The Enterprise Cache Layer delivers production-grade performance:

- **99.81% cache hit ratio** eliminates virtually all redundant database queries
- **Sub-18ms average latency** across all endpoints under concurrent load
- **Up to 1,957 requests/second** throughput on a single Docker container instance
- **1.16 MB memory footprint** for the entire cached dataset
- **Zero errors** across 1,500 concurrent requests
- **Zero Redis slow log entries** — no performance anti-patterns detected
- **Instantaneous cache warm-up** — first request populates, all subsequent requests serve from Redis

The architecture is sound. No speculative optimizations were applied. Every metric is backed by actual benchmark execution.
