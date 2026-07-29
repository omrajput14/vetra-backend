# Entity Relationship Diagram (ERD)
**Document ID:** DATA-05  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Database Design](./04-database-design.md), [Domain Model](../domain/03-domain-model.md)

---

## Overview

This document provides the Entity Relationship Diagram for the Vetra PostgreSQL database, covering all tables from Flyway migrations V1 through V6.

---

## Entity Relationship Diagram

```mermaid
erDiagram
    USERS {
        uuid id PK
        varchar email UK
        varchar phone UK
        varchar password_hash
        varchar role
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    FARMER_PROFILES {
        uuid id PK
        uuid user_id FK
        varchar full_name
        varchar farm_name
        varchar village
        varchar district
        varchar state
        float8 latitude
        float8 longitude
        int animal_count
        timestamptz created_at
        timestamptz updated_at
    }

    VET_PROFILES {
        uuid id PK
        uuid user_id FK
        varchar full_name
        varchar registration_number UK
        varchar qualification
        varchar specialization
        varchar clinic_name
        int years_experience
        boolean is_available
        float8 latitude
        float8 longitude
        timestamptz created_at
        timestamptz updated_at
    }

    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        text token
        timestamptz expires_at
        boolean revoked
        timestamptz created_at
    }

    ANIMALS {
        uuid id PK
        uuid farmer_id FK
        varchar tag_number
        varchar qr_code_id UK
        varchar species
        varchar breed
        varchar gender
        date birth_date
        varchar name
        varchar photo_url
        timestamptz created_at
        timestamptz updated_at
    }

    APPOINTMENTS {
        uuid id PK
        uuid farmer_id FK
        uuid veterinarian_id FK
        uuid animal_id FK
        date appointment_date
        time appointment_time
        varchar visit_type
        varchar status
        text reason
        text veterinarian_notes
        text cancellation_reason
        bigint version
        timestamptz created_at
        timestamptz updated_at
    }

    MEDICAL_RECORDS {
        uuid id PK
        uuid appointment_id FK
        uuid animal_id FK
        uuid farmer_id FK
        uuid veterinarian_id FK
        text diagnosis
        text symptoms
        text treatment
        text prescription
        numeric weight
        numeric temperature
        date follow_up_date
        text notes
        bigint version
        timestamptz created_at
        timestamptz updated_at
    }

    USERS ||--o| FARMER_PROFILES : "has profile"
    USERS ||--o| VET_PROFILES : "has profile"
    USERS ||--o{ REFRESH_TOKENS : "has tokens"

    FARMER_PROFILES ||--o{ ANIMALS : "owns"
    FARMER_PROFILES ||--o{ APPOINTMENTS : "books"
    FARMER_PROFILES ||--o{ MEDICAL_RECORDS : "receives"

    VET_PROFILES ||--o{ APPOINTMENTS : "attends"
    VET_PROFILES ||--o{ MEDICAL_RECORDS : "creates"

    ANIMALS ||--o{ APPOINTMENTS : "subject of"
    ANIMALS ||--o{ MEDICAL_RECORDS : "documented in"

    APPOINTMENTS ||--o| MEDICAL_RECORDS : "generates"
```

---

## Relationship Descriptions

### USERS → FARMER_PROFILES / VET_PROFILES (One-to-One)

A `USERS` record is the authentication identity. Every user has exactly one role (`FARMER` or `VET`) and exactly one corresponding profile. Profiles are created atomically during registration (same transaction as user creation).

**Delete behavior:** `ON DELETE CASCADE` — deleting a user deletes their profile.

---

### USERS → REFRESH_TOKENS (One-to-Many)

A user may have multiple refresh tokens (one per device/session). Tokens are revoked on logout or invalidated on password change. Expired tokens are candidates for pruning.

---

### FARMER_PROFILES → ANIMALS (One-to-Many)

A farmer owns zero or more animals. When a farmer's profile is deleted (which cascades from user deletion), all their animals are deleted.

**Business Rule:** An animal cannot be transferred between farmers in the current implementation. Transfer ownership is planned for a future stage.

---

### FARMER_PROFILES + VET_PROFILES + ANIMALS → APPOINTMENTS (Many-to-One from each)

An appointment is the intersection of a farmer, a veterinarian, and a specific animal. All three foreign keys are required and non-nullable.

**Note:** The appointment belongs to one farmer, one veterinarian, and one animal. There is no many-to-many junction table — the appointment itself is the relationship.

---

### APPOINTMENTS → MEDICAL_RECORDS (One-to-One)

A completed appointment may generate exactly one medical record. `appointment_id` is `UNIQUE` in `medical_records`, enforcing this constraint at the database level.

**Business Rule:** A medical record can only be created once a corresponding appointment has `status = COMPLETED`.

---

### ANIMALS → MEDICAL_RECORDS (One-to-Many)

An animal accumulates medical records over its lifetime. The collection of all records for an animal constitutes its medical history (EVMR timeline).

---

## Cascade Delete Map

| Delete Event | Cascades To |
|---|---|
| `DELETE FROM users` | `farmer_profiles`, `vet_profiles`, `refresh_tokens` |
| `DELETE FROM farmer_profiles` | `animals`, `appointments` (farmer side), `medical_records` (farmer side) |
| `DELETE FROM animals` | `appointments` (animal side), `medical_records` (animal side) |
| `DELETE FROM appointments` | `medical_records` |

> [!CAUTION]
> Deleting a `users` record is a destructive operation that cascades through the entire user data hierarchy. In production, users should be soft-deleted (`is_active = FALSE`) rather than hard-deleted. Hard delete should only be used for regulatory data erasure requests.

---

## Cardinality Summary

| Relationship | Cardinality |
|---|---|
| User → FarmerProfile | 1:0..1 |
| User → VetProfile | 1:0..1 |
| User → RefreshTokens | 1:0..N |
| FarmerProfile → Animals | 1:0..N |
| FarmerProfile → Appointments | 1:0..N |
| VetProfile → Appointments | 1:0..N |
| Animal → Appointments | 1:0..N |
| Appointment → MedicalRecord | 1:0..1 (max 1) |
| Animal → MedicalRecords | 1:0..N |
