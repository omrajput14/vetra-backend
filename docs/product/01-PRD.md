# Product Requirements Document (PRD) — Vetra Veterinary Operating System (VetOS)
**Document ID:** PRD-01  
**Version:** 2.0.0  
**Status:** Approved / Active  
**Last Updated:** 2026-07-29  
**Applies To:** Vetra Ecosystem (`omrajput14/vetra` & `omrajput14/vetra-backend`)  
**References:** [Engineering Principles](../engineering/00-principles.md), [Domain Model](../../vetra-backend/docs/domain/03-domain-model.md), [API Specification](../../vetra-backend/docs/api/06-specification.md)

---

## 1. Executive Summary

### 1.1 Product Overview
**Vetra** is an enterprise-grade **Veterinary Operating System (VetOS)** designed to serve as the digital infrastructure for livestock healthcare, veterinary practice management, and epidemiological disease surveillance in agricultural economies. 

Vetra provides a unified operating environment connecting smallholder farmers, field veterinarians, commercial livestock operations, and government veterinary authorities. It combines digital animal identity (Animal Passport), clinical workflow automation, Electronic Veterinary Medical Records (EVMR), practice management, and optional AI-assisted visual diagnostics within an offline-first, highly secure architecture.

### 1.2 Product Positioning
Vetra is **not** an "AI app." Vetra is an **Enterprise Operating System**. AI-assisted visual analysis is an optional diagnostic support tool integrated into the broader clinical workflow, operating strictly under the supervision of licensed veterinary professionals.

---

## 2. Product Vision & Mission

### 2.1 Product Vision
To build the world's most trusted, resilient, and scalable Veterinary Operating System, powering digital animal healthcare identity, clinical excellence, and disease eradication across global agricultural supply chains.

### 2.2 Product Mission
To digitize every livestock animal's identity and medical lineage, streamline veterinary clinical operations, eliminate preventable animal disease outbreaks through real-time surveillance, and maximize economic yields for livestock producers.

---

## 3. Product Philosophy & Core Principles

Every architectural, product, and design decision in Vetra must adhere to the following nine core principles:

1. **Mobile-First:** Tailored for field environments where smartphones are the primary computing device for both farmers and mobile veterinarians.
2. **Offline-First:** Critical clinical and passport capabilities must function seamlessly in zero-connectivity rural zones, synchronizing deterministically when connectivity restores.
3. **Security-First:** Strict Role-Based Access Control (RBAC), multi-tenant isolation, cryptographic token security, and complete data privacy by default.
4. **Documentation-First:** No feature is complete without synchronized, production-quality technical documentation, API specifications, and architectural records.
5. **Enterprise-Ready:** Designed from day one for multi-tenant scalability, auditability, regulatory compliance, and multi-region deployment.
6. **Scalable for Millions of Users:** High-throughput database schemas, stateless API tiers, and efficient payload serialization supporting millions of concurrent livestock records.
7. **Clean Architecture:** Strict isolation between presentation, application domain, and infrastructure layers across both client and backend codebases.
8. **Domain-Driven Design (DDD):** Universal domain language (`FarmerProfile`, `VetProfile`, `AnimalPassport`, `EVMR`, `VisitType`) reflected identically in code, database, and APIs.
9. **Long-Term Maintainability:** Minimal technical debt, modular boundaries, zero magic, explicit dependencies, and strict static analysis enforcement.

---

## 4. Problem Statement

### 4.1 Enterprise & Agricultural Challenges
1. **Absence of Verified Animal Health Lineage:** Over $12B in annual global livestock value is lost due to unverified animal health histories, leading to misdiagnoses, buyer fraud, and vaccine failure.
2. **Veterinary Practice Inefficiencies:** Field veterinarians spend up to 40% of their working hours on manual paper registers, phone coordination, and billing tracking rather than direct clinical care.
3. **Catastrophic Outbreak Propagation:** Infectious livestock diseases (e.g., Foot-and-Mouth, Lumpy Skin Disease) propagate undetected due to non-existent real-time disease surveillance systems.
4. **Unregulated Drug Administration:** Paper-based prescriptions result in unmonitored antibiotic usage, contributing to Antimicrobial Resistance (AMR) and international trade rejections.

---

## 5. Business Goals & Key Performance Indicators (KPIs)

### 5.1 Business Goals
- Establish Vetra as the standard digital Operating System for veterinary care in target agricultural markets.
- Achieve 99.9% clinical data integrity and zero record loss across offline synchronization cycles.
- Provide real-time disease outbreak warning capabilities to regional veterinary authorities.

### 5.2 Key Performance Indicators (KPIs)

| Metric | Target | Measurement Frequency |
|---|---|---|
| **System Uptime (SLA)** | 99.9% Availability | Continuous |
| **API Latency (p95)** | < 150 ms | Continuous monitoring |
| **Offline Sync Success Rate** | 99.99% without conflict | Weekly audit |
| **EVMR Adoption Rate** | 100% of completed visits generate an EVMR | Daily |
| **Mobile Crash-Free Sessions** | ≥ 99.5% on Android & iOS | Continuous |
| **Active Practice Retention** | ≥ 90% monthly active vet practices | Monthly |

---

## 6. Stakeholders

| Stakeholder Group | Role & Primary Interest |
|---|---|
| **Smallholder Farmers** | Animal identity, fast emergency vet dispatch, health tracking |
| **Commercial Dairy / Herd Owners** | Herd health analytics, bulk vaccinations, animal valuation |
| **Field Veterinarians & Surgeons** | Clinical workflow, EVMR creation, appointment scheduling, billing |
| **Veterinary Clinics & Hospitals** | Practice management, vet resource allocation, revenue tracking |
| **Government Epidemiologists** | Real-time disease outbreak mapping, biosecurity containment |
| **Vetra Platform Operations** | Security, system availability, multi-tenant compliance, platform health |

---

## 7. Target Audience & User Personas

### Persona A: Ramesh Kumar — Smallholder Dairy Farmer
- **Demographics:** 42 years old, Karnal District, Haryana | 8 Cattle & Buffaloes.
- **Environment:** Low-end Android smartphone, intermittent 3G/4G connectivity, low digital literacy.
- **Pain Points:** Cannot find qualified vets at night; paper health cards get damaged; buyers doubt animal health claims.
- **Vetra Goals:** One-click vet booking, tamper-proof QR Animal Passport, vaccine reminders.

### Persona B: Dr. Suresh Sharma — Licensed Field Veterinary Surgeon
- **Demographics:** 38 years old, B.V.Sc & A.H., M.V.Sc (Surgery) | 12 Years Experience.
- **Environment:** Mid-range Android smartphone, travels 60+ km daily for farm visits.
- **Pain Points:** Manually writing paper prescriptions in the field; no access to animal's past treatment history; missed follow-ups.
- **Vetra Goals:** Instant QR scan for past medical records, structured digital EVMR issuing, automated schedule management.

---

## 8. End-to-End User Journeys

### Journey 1: Clinical Appointment & EVMR Creation Flow
1. **Booking:** Farmer selects an animal, chooses visit type (`TREATMENT`), and requests an appointment with a nearby registered Vet via the mobile client.
2. **Confirmation:** Vet receives push alert/dashboard update, reviews reason, and marks appointment `CONFIRMED`.
3. **Field Visit & Scan:** Vet arrives at the farm, scans the animal's physical QR tag with the Vetra app, and views its complete historical EVMR timeline.
4. **Clinical Examination:** Vet conducts exam, completes the visit, and enters diagnosis, treatment, and prescription into the digital EVMR form.
5. **State Transition & Lock:** Vet submits record; appointment transitions to `COMPLETED`. The EVMR becomes immutable and is instantly visible on the Farmer's Animal Passport.

---

## 9. Functional Requirements

### FR-1: Identity, Multi-Tenancy & Access Management
- **FR-1.1:** System shall support strict role separation between `FARMER` and `VET` user accounts.
- **FR-1.2:** Authentication shall utilize JWT access tokens (15-min expiry) with database-backed refresh token rotation (7-day expiry).
- **FR-1.3:** System shall enforce resource-level ownership: Farmers can only view their own animals/records; Vets can only manage assigned appointments.

### FR-2: Livestock Management & Digital Animal Passport
- **FR-2.1:** Farmers shall register animals with ear tag numbers, species, breed, gender, birth date, and optional photo.
- **FR-2.2:** System shall generate a globally unique `qr_code_id` for every registered animal.
- **FR-2.3:** Scanning an animal's QR code shall open its digital Animal Passport, displaying current health status and full chronological medical history.

### FR-3: Veterinary Practice & Appointment Operations
- **FR-3.1:** Farmers shall search registered veterinarians by location, specialization, and availability status.
- **FR-3.2:** System shall implement a formal appointment state machine (`PENDING` → `CONFIRMED` → `COMPLETED` / `CANCELLED`).
- **FR-3.3:** System shall use database optimistic locking (`version` column) to prevent concurrent update race conditions.

### FR-4: Electronic Veterinary Medical Record (EVMR) System
- **FR-4.1:** System shall allow licensed veterinarians to generate an EVMR for any `COMPLETED` appointment.
- **FR-4.2:** EVMR shall capture diagnosis, symptoms, clinical treatment, prescription, weight, temperature, follow-up date, and notes.
- **FR-4.3:** Once created, an EVMR shall be **immutable** — no `PUT` or `DELETE` endpoints shall exist for medical records.
- **FR-4.4:** System shall enforce a maximum of 1 EVMR per appointment (`appointment_id` UNIQUE constraint).

### FR-5: Clinical Decision Support & Visual Diagnostics (Optional Capability)
- **FR-5.1:** Platform shall provide an optional camera-assisted visual diagnostic module for preliminary lesion and symptom analysis.
- **FR-5.2:** Visual diagnostic outputs shall be clearly designated as "Diagnostic Support Suggestions" and require veterinarian verification before clinical action.

### FR-6: Epidemiological Surveillance & Spatial Mapping (Planned)
- **FR-6.1:** System shall record geo-coordinates for farm locations and clinic practices using PostGIS.
- **FR-6.2:** System shall aggregate anonymized disease diagnoses to detect regional outbreak clusters and render risk zones on an interactive map.

---

## 10. Non-Functional Requirements

### NFR-1: Scalability & Performance
- **NFR-1.1:** API response latency shall be < 150 ms (p95) for all core read/write operations.
- **NFR-1.2:** System shall support 500+ concurrent active connections per database node without performance degradation.

### NFR-2: Offline Resilience & Network Optimization
- **NFR-2.1:** Mobile app shall cache Animal Passports and historical EVMRs locally for offline viewing.
- **NFR-2.2:** API payloads shall be optimized (< 50 KB per routine request) for operation over 2G/3G networks.

### NFR-3: Security, Privacy & Compliance
- **NFR-3.1:** Passwords must be hashed using bcrypt (cost factor 10). Passwords and tokens must never appear in log files.
- **NFR-3.2:** All API communication must mandate HTTPS / TLS 1.3 in staging and production environments.
- **NFR-3.3:** Database primary keys must use UUID v4 to prevent ID enumeration vulnerabilities.

### NFR-4: Immutability & Auditability
- **NFR-4.1:** Medical records cannot be updated or deleted once created, fulfilling electronic health record compliance.
- **NFR-4.2:** All schema modifications must be versioned and executed exclusively through Flyway migrations.

---

## 11. Feature Implementation Matrix

| Feature / Subsystem | Description | Target Role | Enterprise Status |
|---|---|---|---|
| **Role-Based Auth & JWT** | Dual-role registration, login, JWT refresh token rotation | Both | **Implemented** |
| **Farmer Profile & Vet Directory** | Location profiles, vet availability listing | Both | **Implemented** |
| **Livestock Passport & QR** | Animal registration, QR generation, herd list | Farmer | **Implemented** |
| **Appointment State Machine** | Booking, confirmation, completion, optimistic locking | Both | **Implemented** |
| **EVMR Clinical System** | Immutable record creation, clinical history timeline | Both | **Implemented** |
| **Repository Separation & Docs** | Independent Git repos, 25 engineering docs, `.git` optimization | System | **Implemented** |
| **AI Visual Diagnostic Support** | Camera-based lesion/symptom image inference | Farmer / Vet | **Planned** (Stage 9) |
| **PostGIS Spatial Outbreak Map** | Geospatial disease cluster analysis, outbreak alerts | All | **Planned** (Stage 10) |
| **Push Notifications (FCM)** | Automated appointment alerts & biosecurity warnings | Both | **Planned** (Stage 11) |
| **Cloud CI/CD & Monitoring** | GitHub Actions, AWS ECS/RDS, Prometheus/Grafana | System | **Planned** (Stage 12) |
| **Structured Prescription Table** | Normalized drug codes & dosage unit schema | Vet | **Deferred** (Post-V1) |

---

## 12. Scope & Out of Scope

### In-Scope (Vetra V1 Platform)
- Dual-role authentication (Farmer & Vet).
- Livestock herd management & QR Animal Passport.
- Appointment scheduling with state machine validation.
- Immutable Electronic Veterinary Medical Records (EVMR).
- Role-specific dashboards and clinical timelines.

### Out-of-Scope (Deferred to Future Major Versions)
- Direct e-commerce / pharmacy payment processing.
- Hardware RFID ear tag reader bluetooth pairing (QR scanning only for V1).
- In-app video calling (phone & field visit coordination only for V1).
- Automated AI prescription generation (prescriptions must be authored by licensed veterinarians).

---

## 13. Risks, Assumptions & Mitigations

| Risk | Impact | Likelihood | Mitigation Strategy |
|---|---|---|---|
| **Low rural network connectivity** | High | High | Offline-first local database caching (Hive/SQLite) & deterministic sync. |
| **Double record creation race condition** | Medium | Medium | Unique database constraint on `appointment_id` in `medical_records` table + optimistic locking. |
| **Veterinarian reluctance to enter digital data** | High | Medium | UI optimized for < 45-second EVMR creation time with auto-suggested common diagnoses. |
| **ID enumeration security attack** | High | Low | Global enforcement of UUID v4 primary keys across all domain entities. |

---

## 14. Multi-Phase Release Strategy

```
Phase 1: Core Foundation (Implemented)
└── Stages 1–7: Auth, Animals, Appointments, EVMR, Dual-Role Dashboards

Phase 2: Standardization & Documentation (Implemented — Current)
└── Stage 8: Repository separation, history rewrite, 25 engineering specs

Phase 3: Intelligence & Spatial Surveillance (Planned — Q3 2026)
└── Stages 9–10: AI Diagnostic Support, PostGIS Outbreak Mapping

Phase 4: Ecosystem & Production Scale (Planned — Q4 2026)
└── Stages 11–12: FCM Notifications, AWS Cloud Infrastructure, CI/CD, Audits
```

---

## 15. Future Vision (5–10 Year Horizon)

Over the next decade, Vetra will evolve into the national and international digital backbone for livestock health intelligence. Key long-term vectors include:
- **National Disease Eradication Intelligence:** Real-time integration with national agricultural departments for automated disease reporting and quarantine zoning.
- **Genomic & Breeding Health Lineage:** Incorporating genetic tracking into the Animal Passport to improve livestock breeding valuations.
- **Cross-Border Biosecurity Certification:** Export-grade digital health certificates verified via immutable clinical records for international livestock trade.
