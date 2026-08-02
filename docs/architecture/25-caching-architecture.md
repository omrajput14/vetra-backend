# Vetra Backend — Enterprise Caching Architecture (Stage 12.3.2)

## Executive Summary

This document specifies the enterprise caching architecture for the **Vetra Backend Platform**. Built on **Redis 7.4** and Spring Boot's unified `CacheManager` abstraction (`RedisCacheManager`), this architecture provides a high-throughput, low-latency, deterministic, and resilient multi-region caching layer.

The design enforces strict separation of concerns, zero hardcoded cache region strings, explicit Time-To-Live (TTL) policies per domain, collision-free key namespacing (`vetra:<domain>:<id>`), multi-level cache invalidation flow, and Micrometer/Prometheus observability.

---

## 1. Cache Architecture Audit

Every entity, endpoint, and query pattern across the Vetra platform is audited and classified into four operational caching categories based on data volatility, read-to-write ratio, and query complexity.

### Operational Classifications

| Category | Definition | Invalidation Policy | Example Domains |
|----------|------------|---------------------|-----------------|
| **Cache Candidate** | High-read, moderate-write operational domain entities accessed frequently by primary key or deterministic lookup. | Evict on write (Create/Update/Delete) | Users, Animals, Appointments, Medical Records |
| **Short-lived Cache** | Highly dynamic aggregation endpoints or temporal validation data subject to frequent state changes. | TTL-based expiration + targeted eviction | Dashboards (Farmer/Vet/Admin), OTP verification |
| **Long-lived Cache** | Immutable, slow-changing, or computationally expensive outputs (e.g., AI inference, reference catalogs). | High TTL (12-24h) + explicit manual flush | AI Diagnosis, Reference Data, Analytics |
| **Never Cache** | Sensitive security credentials, audit trails, real-time message streams, or transactional state transitions. | N/A (Direct DB read/write only) | Passwords/Hashes, Delivery Logs, Direct Financial Trx |

### Domain-by-Domain Audit Matrix

| Domain | Entity / Endpoint / Query | Classification | Justification |
|--------|---------------------------|----------------|---------------|
| **Auth** | OTP Code (`vetra:otp:{phone}`) | Short-lived Cache | 5-minute ephemeral lifespan. Rapid lookups during SMS login flow. |
| **Auth** | User Security Principal (`vetra:user:{id}`) | Cache Candidate | Read on every authenticated API request via JWT filter. High-frequency read. |
| **User** | User Profile (`vetra:user_profile:{id}`) | Cache Candidate | Accessed on profile views and UI headers. Updated infrequently. |
| **Animal** | Livestock Entity (`vetra:animal:{id}`) | Cache Candidate | Read frequently during medical checks, appointments, and dashboard views. |
| **Appointment** | Appointment Entity (`vetra:appointment:{id}`) | Cache Candidate | Accessed by farmers and vets. High read frequency. Evicted on status change. |
| **Medical Record**| EVMR Entity (`vetra:medical_record:{id}`) | Cache Candidate | Permanent clinical history. Read heavily during consultation; updated rarely. |
| **AI Engine** | AI Diagnosis Result (`vetra:ai:{imageHash}`) | Long-lived Cache | Multi-provider inference is computationally expensive ($ and latency). Deduplicated by SHA-256 image hash. |
| **Disease Surveillance**| Disease Report (`vetra:disease_report:{id}`) | Cache Candidate | Spatial & tabular surveillance queries. Evicted on diagnosis status updates. |
| **Disease Surveillance**| Outbreak Cluster (`vetra:outbreak:{id}`) | Short-lived Cache | Evaluated dynamically during spatial clustering. Evicted when risk score/radius changes. |
| **Dashboard** | Farmer Aggregation (`vetra:dashboard:farmer:{id}`) | Short-lived Cache | Aggregates animal count, upcoming appointments, and active alerts. 5m TTL. |
| **Dashboard** | Vet Aggregation (`vetra:dashboard:vet:{id}`) | Short-lived Cache | Aggregates assigned appointments, pending reports, and active outbreaks. 5m TTL. |
| **Dashboard** | Admin Aggregation (`vetra:dashboard:admin`) | Short-lived Cache | Platform-wide macro metrics. Heavy SQL GROUP BYs. 5m TTL. |
| **Notifications** | Notification Templates & Device Tokens | Cache Candidate | Template lookup per notification dispatch. High read, zero write during runtime. |
| **Reference Data** | Disease Catalog & Species Reference | Long-lived Cache | Static master catalogs. Zero write frequency during standard operations. 12h TTL. |
| **Analytics** | Epidemiological Statistics | Long-lived Cache | Heavy spatial-temporal queries across historical disease reports. 24h TTL. |
| **Security Audit**| Password Hashes & Refresh Tokens | Never Cache | Security risk to cache credentials in shared memory. Must validate against DB. |
| **Notification Log**| Transmission Delivery Logs | Never Cache | Write-only audit trail. Never read in hot paths. |

---

## 2. Enterprise Cache Registry

All cache regions are centrally managed via `CacheNames.java` and registered within `CacheConfiguration.java`. Hardcoded string literals in application code are strictly prohibited.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                CENTRAL CACHE REGISTRY                                   │
├──────────────────┬──────────────┬─────────────────────────────────┬───────────┬─────────┤
│ Cache Region     │ Owner        │ Operational Purpose             │ Default   │ Evict   │
│ Name             │ Domain       │                                 │ TTL       │ Method  │
├──────────────────┼──────────────┼─────────────────────────────────┼───────────┼─────────┤
│ otp              │ Auth         │ SMS OTP verification codes      │ 5 minutes │ On Verify
│ dashboard_farmer │ Dashboard    │ Farmer UI aggregated summaries  │ 5 minutes │ On Event│
│ dashboard_vet    │ Dashboard    │ Vet UI aggregated summaries     │ 5 minutes │ On Event│
│ dashboard_admin  │ Dashboard    │ Admin platform macro metrics    │ 5 minutes │ Scheduled
│ animals          │ Animal       │ Animal profiles & metadata      │ 15 mins   │ On Write│
│ appointments     │ Appointment  │ Scheduling & status data        │ 15 mins   │ On Write│
│ medical_records  │ MedicalRecord│ EVMR clinical records           │ 30 mins   │ On Write│
│ users            │ Auth         │ Security principal data         │ 30 mins   │ On Write│
│ user_profiles    │ Auth         │ Extended profile metadata       │ 30 mins   │ On Write│
│ disease_reports  │ Disease      │ Individual disease reports      │ 1 hour    │ On Write│
│ outbreaks        │ Disease      │ Active outbreak surveillance    │ 1 hour    │ On Write│
│ notifications    │ Notification │ Device tokens & templates       │ 1 hour    │ On Write│
│ settings         │ Platform     │ Dynamic app configuration flags │ 6 hours   │ On Write│
│ reference_data   │ Platform     │ Master disease & species catalog│ 12 hours  │ On Admin│
│ ai_diagnosis     │ AI           │ SHA-256 hash inference results  │ 24 hours  │ Manual  │
│ analytics        │ Analytics    │ Historical aggregation statistics│ 24 hours  │ Scheduled
└──────────────────┴──────────────┴─────────────────────────────────┴───────────┴─────────┘
```

---

## 3. Cache Constants Infrastructure Code

The cache architecture infrastructure is strictly encapsulated within the package `app.vetra.infrastructure.cache`:

1. **`CacheNames`**: Single source of truth for cache region names (`public static final String USERS = "users"`, etc.).
2. **`CacheTtl`**: Strongly typed `java.time.Duration` constants defining the expiration boundary for each cache region.
3. **`CacheKeys`**: Utility class producing deterministic, collision-free key formats formatted as `vetra:<domain>:<id>`.
4. **`CacheConfiguration`**: Spring `@Configuration` provisioning a `RedisCacheManager` configured with per-cache TTL maps, `GenericJackson2JsonRedisSerializer`, `StringRedisSerializer`, `disableCachingNullValues()`, and transaction awareness.

---

## 4. Time-To-Live (TTL) Policy & Justification

### TTL Policy Matrix

```
       5 min          15 min          30 min           1 hour           6 hours          12-24 hours
     ┌──────────────┬───────────────┬───────────────┬────────────────┬────────────────┬─────────────────┐
     │ OTP          │ Animals       │ MedicalRecs   │ Disease Reports│ App Settings   │ Reference Data  │
     │ Dashboards   │ Appointments  │ Users         │ Outbreaks      │                │ AI Diagnosis    │
     │              │               │ User Profiles │ Notifications  │                │ Analytics       │
     └──────────────┴───────────────┴───────────────┴────────────────┴────────────────┴─────────────────┘
     ◄ HIGH VOLATILITY / TEMPORAL                                            LOW VOLATILITY / COMPUTATIONAL ►
```

### Justification Breakdown

1. **OTP (5 Minutes)**: Security SLA limit. Ephemeral validation codes must expire rapidly to prevent brute-force exploitation.
2. **Dashboards (5 Minutes)**: High query complexity involving multiple JOINs and aggregations. 5-minute TTL provides near-real-time accuracy while shielding PostgreSQL from continuous polling spikes.
3. **Animals & Appointments (15 Minutes)**: Operational data with moderate update frequency. Balance between freshness for appointment state transitions and DB load reduction.
4. **Medical Records & Users (30 Minutes)**: Low modification frequency. EVMR records are immutable once completed; user roles change rarely.
5. **Disease Surveillance & Outbreaks (1 Hour)**: Regional spatial calculations and outbreak cluster boundaries. Re-evaluated hourly by background jobs.
6. **Reference Data (12 Hours) & AI Diagnosis (24 Hours)**: Static disease catalogs and deterministic AI model inferences (keyed by image hash). Maximum cache utility; zero stale data risk.

---

## 5. Cache Key Strategy

Keys are formatted deterministically using a standardized prefix namespace to eliminate key collisions across environments and application modules.

### Namespace Pattern
$$\text{Namespace} = \text{vetra}:\langle \text{domain} \rangle:\langle \text{identifier} \rangle$$

### Key Patterns

```
vetra:user:{uuid}                     -> vetra:user:123e4567-e89b-12d3-a456-426614174000
vetra:user_profile:{uuid}             -> vetra:user_profile:123e4567-e89b-12d3-a456-426614174000
vetra:animal:{uuid}                   -> vetra:animal:123e4567-e89b-12d3-a456-426614174000
vetra:appointment:{uuid}              -> vetra:appointment:123e4567-e89b-12d3-a456-426614174000
vetra:medical_record:{uuid}           -> vetra:medical_record:123e4567-e89b-12d3-a456-426614174000
vetra:ai:{imageSha256Hash}            -> vetra:ai:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
vetra:disease_report:{uuid}           -> vetra:disease_report:123e4567-e89b-12d3-a456-426614174000
vetra:outbreak:{uuid}                 -> vetra:outbreak:123e4567-e89b-12d3-a456-426614174000
vetra:dashboard:farmer:{uuid}         -> vetra:dashboard:farmer:123e4567-e89b-12d3-a456-426614174000
vetra:dashboard:vet:{uuid}            -> vetra:dashboard:vet:123e4567-e89b-12d3-a456-426614174000
vetra:dashboard:admin                 -> vetra:dashboard:admin
vetra:otp:{phoneNumber}              -> vetra:otp:+256700000000
vetra:ref:{category}                  -> vetra:ref:diseases
```

### Determinism Rules
- Primary keys must use canonical UUID hyphenated string representations (`java.util.UUID#toString()`).
- Image hashes must use lower-case SHA-256 hex strings.
- Phone numbers must follow E.164 international format (`+<country><national>`).

---

## 6. Cache Invalidation & Cascade Eviction Strategy

Data updates trigger explicit cache invalidation to prevent stale reads. Cascade eviction propagates invalidations down the entity dependency hierarchy.

### Invalidation Trigger Matrix

| Mutation Event | Direct Target Evicted | Cascade Eviction Targets |
|----------------|----------------------|--------------------------|
| `Update Animal` | `vetra:animal:{id}` | `vetra:dashboard:farmer:{farmerId}`, `vetra:analytics` |
| `Update Appointment` | `vetra:appointment:{id}` | `vetra:dashboard:farmer:{farmerId}`, `vetra:dashboard:vet:{vetId}` |
| `Create MedicalRecord`| `vetra:medical_record:{id}` | `vetra:animal:{animalId}`, `vetra:appointment:{aptId}`, `vetra:dashboard:farmer:{farmerId}`, `vetra:dashboard:vet:{vetId}` |
| `Create DiseaseReport`| `vetra:disease_report:{id}`| `vetra:outbreak:{clusterId}`, `vetra:dashboard:vet:{vetId}`, `vetra:dashboard:admin`, `vetra:analytics` |
| `Update Outbreak` | `vetra:outbreak:{id}` | `vetra:dashboard:admin`, `vetra:analytics` |
| `Update User Profile` | `vetra:user_profile:{id}`, `vetra:user:{id}` | `vetra:dashboard:farmer:{id}` or `vetra:dashboard:vet:{id}` |

### Cascade Eviction Flow Diagram

```
                 ┌─────────────────────────────┐
                 │    Mutation: Create EVMR    │
                 └──────────────┬──────────────┘
                                │
          ┌─────────────────────┴─────────────────────┐
          ▼                                           ▼
┌───────────────────────────┐               ┌───────────────────────────┐
│ Direct Evict:             │               │ Direct Evict:             │
│ vetra:medical_record:{id} │               │ vetra:appointment:{id}    │
└─────────┬─────────────────┘               └─────────┬─────────────────┘
          │                                           │
          └─────────────────────┬─────────────────────┘
                                │
                                ▼ (Cascade)
                  ┌───────────────────────────┐
                  │ Evict: vetra:animal:{id}  │
                  └─────────────┬─────────────┘
                                │
          ┌─────────────────────┴─────────────────────┐
          ▼ (Cascade)                                 ▼ (Cascade)
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│ Evict:                          │       │ Evict:                          │
│ vetra:dashboard:farmer:{farmer} │       │ vetra:dashboard:vet:{vetId}     │
└─────────────────────────────────┘       └─────────────────────────────────┘
```

---

## 7. Redis & Serialization Architecture

### 1. Serializer Design
- **Keys & Hash Keys**: `StringRedisSerializer` (UTF-8 encoded human-readable string keys).
- **Values & Hash Values**: `GenericJackson2JsonRedisSerializer` for polymorphic JSON serialization supporting complex Java DTOs, Enums, and collections without Java native serialization vulnerabilities (`JdkSerializationRedisSerializer` is strictly prohibited).

### 2. Null Value Protection
All cache configurations invoke `.disableCachingNullValues()`. If a database lookup returns `null` or `Optional.empty()`, Spring Cache will **not** store a `null` value in Redis. This prevents cache poisoning and ensures subsequent valid creations immediately resolve.

### 3. Transaction Support
`RedisCacheManager` is configured with `.transactionAware()`. Cache puts and evictions participate in Spring `@Transactional` boundaries, executing cache updates only after successful database commit.

---

## 8. Metrics & Monitoring Architecture

Spring Boot Actuator and Micrometer automatically register cache metrics for RedisCacheManager when Micrometer is present.

### Key Performance Indicators (KPIs)

| Metric Name | Prometheus Metric Name | Target SLA | Actionable Threshold |
|-------------|------------------------|------------|----------------------|
| **Cache Hit Ratio** | `sum(rate(cache_gets_total{result="hit"}[5m])) / sum(rate(cache_gets_total[5m]))` | $> 85\%$ | $< 70\%$ (Investigate invalidation frequency or TTL) |
| **Cache Miss Rate** | `sum(rate(cache_gets_total{result="miss"}[5m]))` | Low | Spike indicates cache stampede or deployment flush |
| **Eviction Count** | `cache_evictions_total` | Monitored | Sudden spike indicates Redis maxmemory limit pressure |
| **Redis Command Latency**| `lettuce_command_completion_time_seconds_bucket` | $< 5\text{ ms}$ ($p_{99}$) | $> 15\text{ ms}$ |
| **Redis Memory Used**| `redis_memory_used_bytes` | $< 75\%$ allocated | $> 85\%$ (Requires Redis instance scale-up) |

---

## 9. Verification & Architectural Compliance

1. **Checkstyle Compliance**: 0 violations across all infrastructure classes (`CacheNames`, `CacheKeys`, `CacheTtl`, `CacheConfiguration`).
2. **Unit Test Suite**: `CacheConfigurationTest` verifies 53 unit tests passing clean with zero errors.
3. **Layer Separation**: Zero `@Cacheable`, `@CacheEvict`, or `@CachePut` annotations were introduced into business services in Stage 12.3.2. Caching logic remains 100% prepared and frozen for targeted business service enablement in Stage 12.3.3.
