# Vetra Backend — Enterprise Caching Architecture Specification (Stage 12.3.2)

## Executive Summary & Milestone Boundary

This document specifies the authoritative enterprise caching architecture for the **Vetra Backend Platform**.

In accordance with the enterprise engineering roadmap, **Stage 12.3.2 is purely architectural**. It establishes standards, classifications, key conventions, TTL matrices, invalidation strategies, and constant definitions. Runtime Spring cache manager beans, Jackson cache serialization wiring, and `@Cacheable` service bindings are explicitly deferred to **Stage 12.3.3 (Service-Level Cache Implementation)**.

---

## 1. Enterprise Cache Audit & Domain Classifications

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

All cache regions are centrally defined via `CacheNames.java`. Hardcoded string literals in application code are strictly prohibited.

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

## 3. Cache Architecture Standards & Constants

The constants defining cache infrastructure boundaries reside in `app.vetra.infrastructure.cache`:

1. **`CacheNames`**: Single source of truth for cache region names (`public static final String USERS = "users"`, etc.).
2. **`CacheTtl`**: Strongly typed `java.time.Duration` constants defining the expiration boundary for each cache region.
3. **`CacheKeys`**: Utility class producing deterministic, collision-free key formats formatted as `vetra:<domain>:<id>`.

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

---

## 5. Cache Key Strategy

Keys are formatted deterministically using a standardized prefix namespace to eliminate key collisions across environments and application modules.

$$\text{Namespace} = \text{vetra}:\langle \text{domain} \rangle:\langle \text{identifier} \rangle$$

### Key Patterns
- `vetra:user:{id}`
- `vetra:user_profile:{id}`
- `vetra:animal:{id}`
- `vetra:appointment:{id}`
- `vetra:medical_record:{id}`
- `vetra:ai:{imageHash}`
- `vetra:disease_report:{id}`
- `vetra:outbreak:{id}`
- `vetra:dashboard:farmer:{farmerId}`
- `vetra:dashboard:vet:{vetId}`
- `vetra:dashboard:admin`
- `vetra:otp:{phone}`

---

## 6. Cache Invalidation Strategy

Data updates trigger explicit cache invalidation to prevent stale reads. Cascade eviction propagates invalidations down the entity dependency hierarchy.

### Invalidation Matrix

| Mutation Event | Direct Target Evicted | Cascade Eviction Targets |
|----------------|----------------------|--------------------------|
| `Update Animal` | `vetra:animal:{id}` | `vetra:dashboard:farmer:{farmerId}`, `vetra:analytics` |
| `Update Appointment` | `vetra:appointment:{id}` | `vetra:dashboard:farmer:{farmerId}`, `vetra:dashboard:vet:{vetId}` |
| `Create MedicalRecord`| `vetra:medical_record:{id}` | `vetra:animal:{animalId}`, `vetra:appointment:{aptId}`, `vetra:dashboard:farmer:{farmerId}`, `vetra:dashboard:vet:{vetId}` |
| `Create DiseaseReport`| `vetra:disease_report:{id}`| `vetra:outbreak:{clusterId}`, `vetra:dashboard:vet:{vetId}`, `vetra:dashboard:admin`, `vetra:analytics` |
| `Update Outbreak` | `vetra:outbreak:{id}` | `vetra:dashboard:admin`, `vetra:analytics` |
| `Update User Profile` | `vetra:user_profile:{id}`, `vetra:user:{id}` | `vetra:dashboard:farmer:{id}` or `vetra:dashboard:vet:{vetId}` |

---

## 7. Metrics & Monitoring Strategy

Spring Boot Actuator and Micrometer automatically register cache metrics when enabled.

### Target Metrics
- **Cache Hit Ratio**: Target $> 85\%$
- **Cache Miss Rate**: Monitored for stampedes
- **Eviction Count**: Alerts on Redis memory pressure
- **Command Latency**: Target $< 5\text{ ms}$ ($p_{99}$)
- **Redis Memory Usage**: Target $< 75\%$ capacity

---

## 8. Deferred Implementation Items (Stage 12.3.3 Roadmap)

The following implementation components have been intentionally separated and deferred to **Stage 12.3.3**:

1. `RedisCacheManager` bean provisioning (`CacheConfiguration.java`)
2. Per-cache `RedisCacheConfiguration` TTL binding maps
3. `GenericJackson2JsonRedisSerializer` value serialization configuration
4. Null value caching prohibition (`disableCachingNullValues()`)
5. Service-level `@Cacheable`, `@CacheEvict`, and `@CachePut` bindings across business domains
6. Integration testing with Spring `CacheManager` runtime beans
