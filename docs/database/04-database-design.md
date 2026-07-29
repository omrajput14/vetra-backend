# Database Design Document
**Document ID:** DATA-04  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Domain Model](../domain/03-domain-model.md), [ERD](./05-ERD.md), [Engineering Principles](../engineering/00-principles.md)

---

## Overview

This document describes the complete relational database design for Vetra. Every table, column, constraint, index, and design decision is documented here. The database is **PostgreSQL 15** managed exclusively through **Flyway** versioned migrations.

---

## Design Principles

1. **Flyway-only DDL** — No direct DDL changes to the database. Every schema change must be a new Flyway migration.
2. **UUID primary keys** — All primary keys use `uuid_generate_v4()`. This enables future distributed insertion without key conflicts and prevents ID enumeration attacks.
3. **Timezone-aware timestamps** — All timestamps are `TIMESTAMP WITH TIME ZONE`. This ensures correctness for users in different time zones and future multi-region deployment.
4. **Audit columns everywhere** — Every table has `created_at` and `updated_at`.
5. **Optimistic locking** — Tables with concurrent update risk have a `version BIGINT NOT NULL DEFAULT 0` column.
6. **No silent deletes** — `ON DELETE CASCADE` is used only where documented. Soft delete via `is_active` flag is preferred for user data.

---

## PostgreSQL Extensions

Enabled in `V2__schema_entities.sql`:

| Extension | Purpose |
|---|---|
| `uuid-ossp` | `uuid_generate_v4()` for primary key generation |
| `postgis` | Geospatial data types and queries for location-based features |
| `pg_trgm` | Trigram-based text search (future search features) |

---

## Migration History

| Version | File | Description | Date |
|---|---|---|---|
| V1 | `V1__initial_setup.sql` | Initial schema (pre-V2 users table) | Stage 1 |
| V2 | `V2__schema_entities.sql` | Core entities: users, farmer_profiles, vet_profiles, animals | Stage 2 |
| V3 | `V3__refresh_tokens.sql` | Refresh token table for JWT rotation | Stage 3 |
| V4 | `V4__add_animal_name.sql` | Added `name` column to animals | Stage 4 |
| V5 | `V5__create_appointments.sql` | Appointment management + state machine | Stage 6 |
| V6 | `V6__create_medical_records.sql` | EVMR module: medical_records table | Stage 7 |

---

## Table Specifications

---

### `users`

The core identity table. Every actor in the system is a user.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | UUID | NOT NULL | `uuid_generate_v4()` | Primary key |
| `email` | VARCHAR(255) | NULL | — | Unique login email. NULL allowed if phone is set. |
| `phone` | VARCHAR(50) | NULL | — | Unique login phone. NULL allowed if email is set. |
| `password_hash` | VARCHAR(255) | NOT NULL | — | bcrypt-hashed password |
| `role` | VARCHAR(30) | NOT NULL | — | `FARMER` or `VET`. Immutable after registration. |
| `is_active` | BOOLEAN | NOT NULL | TRUE | Soft delete / account suspension flag |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | Record creation time |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | Last modification time |

**Constraints:** `users_email_key` (UNIQUE on email), `users_phone_key` (UNIQUE on phone)  
**Indexes:** `idx_users_email`, `idx_users_phone`, `idx_users_role`  
**Business Rules:**
- At least one of `email` or `phone` must be non-null (enforced at service layer, not DB).
- `role` is set at registration and never changed.
- Passwords are never stored in plaintext.

---

### `farmer_profiles`

Extended profile for Farmer users. Contains farm and location data.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | UUID | NOT NULL | `uuid_generate_v4()` | Primary key |
| `user_id` | UUID | NOT NULL | — | FK → `users.id` ON DELETE CASCADE |
| `full_name` | VARCHAR(255) | NOT NULL | — | Farmer's full name |
| `farm_name` | VARCHAR(255) | NULL | — | Name of the farm |
| `village` | VARCHAR(100) | NULL | — | Village location |
| `district` | VARCHAR(100) | NULL | — | District/county |
| `state` | VARCHAR(100) | NULL | — | State/province |
| `latitude` | DOUBLE PRECISION | NULL | — | GPS latitude |
| `longitude` | DOUBLE PRECISION | NULL | — | GPS longitude |
| `animal_count` | INT | NOT NULL | 0 | Denormalized count (updated on animal add/remove) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |

**Constraints:** `farmer_profiles_user_id_key` (UNIQUE on user_id — one profile per user)  
**Indexes:** `idx_farmer_profiles_user_id`, `idx_farmer_profiles_district`  
**Design Notes:**
- `animal_count` is denormalized for dashboard performance. It is updated atomically when animals are added or removed.
- `latitude`/`longitude` are for farm geolocation (disease radius queries). Not displayed publicly without consent.

---

### `vet_profiles`

Extended profile for Veterinarian users.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | UUID | NOT NULL | `uuid_generate_v4()` | Primary key |
| `user_id` | UUID | NOT NULL | — | FK → `users.id` ON DELETE CASCADE |
| `full_name` | VARCHAR(255) | NOT NULL | — | Veterinarian's full name |
| `registration_number` | VARCHAR(100) | NOT NULL | — | Government-issued vet license number (unique) |
| `qualification` | VARCHAR(255) | NULL | — | Degree/certification |
| `specialization` | VARCHAR(255) | NULL | — | Area of specialization |
| `clinic_name` | VARCHAR(255) | NULL | — | Practice name |
| `years_experience` | INT | NOT NULL | 0 | Years of practice |
| `is_available` | BOOLEAN | NOT NULL | TRUE | Availability for new appointments |
| `latitude` | DOUBLE PRECISION | NULL | — | Clinic/practice GPS latitude |
| `longitude` | DOUBLE PRECISION | NULL | — | Clinic/practice GPS longitude |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |

**Constraints:** UNIQUE on `user_id`, UNIQUE on `registration_number`  
**Indexes:** `idx_vet_profiles_user_id`, `idx_vet_profiles_reg_no`, `idx_vet_profiles_availability`

---

### `animals`

All livestock animals registered on the platform.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | UUID | NOT NULL | `uuid_generate_v4()` | Primary key |
| `farmer_id` | UUID | NOT NULL | — | FK → `farmer_profiles.id` ON DELETE CASCADE |
| `tag_number` | VARCHAR(100) | NOT NULL | — | Physical ear/collar tag number |
| `qr_code_id` | VARCHAR(100) | NULL | — | Unique QR code identifier. Globally unique. |
| `species` | VARCHAR(30) | NOT NULL | — | `CATTLE`, `BUFFALO`, `SHEEP`, `GOAT`, `HORSE`, `PIG`, `POULTRY`, `OTHER` |
| `breed` | VARCHAR(100) | NULL | — | Breed name |
| `gender` | VARCHAR(20) | NOT NULL | — | `MALE`, `FEMALE` |
| `birth_date` | DATE | NULL | — | Animal's date of birth |
| `name` | VARCHAR(100) | NULL | — | Common name given by farmer |
| `photo_url` | VARCHAR(512) | NULL | — | URL to animal photo (cloud storage, future) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |

**Constraints:** UNIQUE on `qr_code_id`  
**Cascades:** DELETE CASCADE from `farmer_profiles` — when a farmer profile is deleted, all their animals are deleted.

---

### `refresh_tokens`

Server-side refresh token store for JWT token rotation.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | UUID | NOT NULL | `uuid_generate_v4()` | Primary key |
| `user_id` | UUID | NOT NULL | — | FK → `users.id` |
| `token` | TEXT | NOT NULL | — | Hashed refresh token value |
| `expires_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | — | Expiry timestamp (7 days from issuance) |
| `revoked` | BOOLEAN | NOT NULL | FALSE | Whether the token has been revoked |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |

**Design Notes:**
- Token rotation: on each refresh, the old token is revoked and a new one is issued.
- Expired and revoked tokens should be pruned by a scheduled job (future: Spring `@Scheduled`).
- Token values are stored hashed (not plaintext).

---

### `appointments`

Clinical appointments between a Veterinarian and a Farmer's Animal.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | UUID | NOT NULL | `uuid_generate_v4()` | Primary key |
| `farmer_id` | UUID | NOT NULL | — | FK → `farmer_profiles.id` |
| `veterinarian_id` | UUID | NOT NULL | — | FK → `vet_profiles.id` |
| `animal_id` | UUID | NOT NULL | — | FK → `animals.id` |
| `appointment_date` | DATE | NOT NULL | — | Scheduled date |
| `appointment_time` | TIME | NOT NULL | — | Scheduled time |
| `visit_type` | VARCHAR(50) | NOT NULL | — | `GENERAL_CHECKUP`, `VACCINATION`, `TREATMENT`, `EMERGENCY`, `FOLLOW_UP` |
| `status` | VARCHAR(30) | NOT NULL | `PENDING` | State machine state |
| `reason` | TEXT | NOT NULL | — | Farmer's description of the issue |
| `veterinarian_notes` | TEXT | NULL | — | Vet's consultation notes |
| `cancellation_reason` | TEXT | NULL | — | Populated when status = `CANCELLED` |
| `version` | BIGINT | NOT NULL | 0 | Optimistic locking version |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |

**Indexes:** `idx_appointments_farmer_id`, `idx_appointments_veterinarian_id`, `idx_appointments_animal_id`, `idx_appointments_status`, `idx_appointments_date`

---

### `medical_records`

Immutable Electronic Veterinary Medical Records (EVMR).

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | UUID | NOT NULL | `uuid_generate_v4()` | Primary key |
| `appointment_id` | UUID | NOT NULL | — | FK → `appointments.id`. UNIQUE — one record per appointment. |
| `animal_id` | UUID | NOT NULL | — | FK → `animals.id` |
| `farmer_id` | UUID | NOT NULL | — | FK → `farmer_profiles.id` |
| `veterinarian_id` | UUID | NOT NULL | — | FK → `vet_profiles.id` |
| `diagnosis` | TEXT | NOT NULL | — | Clinical diagnosis |
| `symptoms` | TEXT | NULL | — | Observed symptoms |
| `treatment` | TEXT | NOT NULL | — | Treatment applied or prescribed |
| `prescription` | TEXT | NULL | — | Medication instructions (TEXT; future: structured `prescriptions` table) |
| `weight` | NUMERIC(6,2) | NULL | — | Animal weight in kg |
| `temperature` | NUMERIC(4,1) | NULL | — | Animal body temperature in °C |
| `follow_up_date` | DATE | NULL | — | Recommended next visit date |
| `notes` | TEXT | NULL | — | Additional clinical notes |
| `version` | BIGINT | NOT NULL | 0 | Optimistic locking (creation-time only; no updates allowed) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | `CURRENT_TIMESTAMP` | — |

**Constraints:** UNIQUE on `appointment_id`  
**Cascades:** ON DELETE CASCADE from `appointments`, `animals`, `farmer_profiles`, `vet_profiles`  
**Indexes:** `idx_medical_records_animal_id`, `idx_medical_records_veterinarian_id`, `idx_medical_records_farmer_id`, `idx_medical_records_appointment_id`

> **Design Note:** `prescription` is stored as `TEXT` intentionally for Stage 7. The justification: structured prescriptions require a normalized `prescriptions` table with drug codes, dosage units, and frequency enums. This normalization is deferred to a future stage when the regulatory and clinical data requirements are better understood. This decision is documented in ADL-007.

---

## Index Strategy

All indexes are created explicitly in Flyway migrations. The index strategy follows these rules:

1. **Every foreign key has an index** — prevents full table scans on joins.
2. **Every frequently-filtered column has an index** — `status`, `appointment_date`, `is_available`.
3. **Composite indexes** are added only when single-column indexes are insufficient (measured via `EXPLAIN ANALYZE`).
4. **Partial indexes** are preferred for boolean flags: `CREATE INDEX ... WHERE is_available = TRUE`.

---

## Future Schema Plans

| Planned Table | Purpose | Stage |
|---|---|---|
| `prescriptions` | Structured medication records (normalize `prescription` TEXT) | Stage 9+ |
| `disease_reports` | Disease outbreak reports with PostGIS location | Stage 8 |
| `notifications` | Push notification records | Stage 9 |
| `audit_log` | Immutable audit trail of all data changes | Stage 10 |
