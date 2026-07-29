# Domain Model
**Document ID:** DOMAIN-03  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** All Vetra repositories  
**References:** [Engineering Principles](../engineering/00-principles.md), [Database Design](../database/04-database-design.md)

---

## Overview

This document defines the **ubiquitous language** of the Vetra domain — the shared vocabulary used by engineers, product managers, and domain experts (farmers, veterinarians). All code, APIs, database schemas, and documentation must use these terms consistently.

---

## Ubiquitous Language Glossary

| Term | Definition | Notes |
|---|---|---|
| **Animal** | A livestock animal registered by a Farmer on the platform. | Has a unique QR code (Animal Passport). |
| **Animal Passport** | The complete digital identity document of an Animal, including its medical history, QR code, and ownership record. | Conceptual — not a separate database table. |
| **Appointment** | A scheduled or completed consultation between a Veterinarian and a Farmer's Animal. | Transitions through a defined state machine. |
| **Appointment Status** | The current state of an Appointment: `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`. | |
| **Diagnosis** | The Veterinarian's professional conclusion about an Animal's condition, recorded in a Medical Record. | Free text in current design; future normalization planned. |
| **Disease Outbreak** | A reported cluster of illness in a geographic area, tracked by the platform for alert and surveillance purposes. | Feature planned for Stage 8+. |
| **EVMR** | Electronic Veterinary Medical Record. The official clinical document created by a Veterinarian for a completed Appointment. | Immutable once created. |
| **Farmer** | A registered user with the `FARMER` role. Owns Animals, books Appointments, and reads Medical Records. | Has a `FarmerProfile`. |
| **FarmerProfile** | The extended profile of a Farmer, including farm name, location, and animal count. | 1:1 with the `users` table. |
| **Follow-up Date** | A date recommended by the Veterinarian in a Medical Record for the Animal's next visit. | Optional field on Medical Record. |
| **Medical Record** | Synonym for EVMR. The permanent clinical history entry for an Appointment. | Immutable. |
| **Medical History** | The chronological collection of all Medical Records for a given Animal. | A view, not a stored entity. |
| **Prescription** | Free-text medication instructions included in a Medical Record. | Intentionally TEXT in current design; future normalization to structured `prescriptions` table planned. |
| **QR Code** | A machine-readable identifier printed and attached to a physical animal, used to instantly retrieve the Animal Passport. | Stored as `qr_code_id` on the `animals` table. |
| **Registration Number** | A government-issued identifier for a licensed Veterinarian. | Unique, required for Vet registration. |
| **Refresh Token** | A long-lived token used to obtain a new access token without re-authentication. | Stored server-side; revocable. |
| **Role** | The user type: `FARMER` or `VET`. | Determines data access, API authorization, and UI presentation. |
| **Species** | The type of livestock: `CATTLE`, `BUFFALO`, `SHEEP`, `GOAT`, `HORSE`, `PIG`, `POULTRY`, `OTHER`. | Enum on the `animals` table. |
| **Symptoms** | The clinical observations reported at the time of the Appointment, recorded in the Medical Record. | |
| **Treatment** | The medical intervention applied during or prescribed after the Appointment. | Required field on Medical Record. |
| **Veterinarian** | A registered user with the `VET` role. Confirms Appointments, creates Medical Records. | Has a `VetProfile`. |
| **VetProfile** | The extended profile of a Veterinarian, including registration number, specialization, and availability status. | 1:1 with the `users` table. |
| **Visit Type** | The category of an Appointment: `GENERAL_CHECKUP`, `VACCINATION`, `TREATMENT`, `EMERGENCY`, `FOLLOW_UP`. | Enum on the `appointments` table. |

---

## Bounded Contexts

Vetra's domain is divided into bounded contexts. Each context owns its data and enforces its own rules. Cross-context communication uses defined interfaces (API calls or Riverpod providers).

```
┌─────────────────────────────────────────────────────────────────┐
│                        VETRA DOMAIN                             │
│                                                                 │
│  ┌──────────────┐   ┌──────────────┐   ┌────────────────────┐  │
│  │              │   │              │   │                    │  │
│  │   Identity   │   │   Livestock  │   │  Clinical Records  │  │
│  │   Context    │   │   Context    │   │  Context           │  │
│  │              │   │              │   │                    │  │
│  │  User        │   │  Animal      │   │  Appointment       │  │
│  │  FarmerProfile│   │  QR Code     │   │  MedicalRecord     │  │
│  │  VetProfile  │   │  Species     │   │  Prescription      │  │
│  │  Role        │   │  AnimalName  │   │  Diagnosis         │  │
│  │  JWT         │   │              │   │  Treatment         │  │
│  │  RefreshToken│   │              │   │  FollowUpDate      │  │
│  │              │   │              │   │                    │  │
│  └──────┬───────┘   └──────┬───────┘   └────────┬───────────┘  │
│         │                  │                    │              │
│         └──────────────────┴────────────────────┘              │
│                    Shared Kernel: User Identity                 │
│                                                                 │
│  ┌───────────────────┐   ┌──────────────────────────────────┐  │
│  │                   │   │                                  │  │
│  │  Disease          │   │  Analytics & Dashboard           │  │
│  │  Surveillance     │   │  Context                         │  │
│  │  Context          │   │                                  │  │
│  │  (Planned Stage 8)│   │  DashboardMetrics                │  │
│  │                   │   │  (current: minimal)              │  │
│  └───────────────────┘   └──────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Aggregates

### User (Aggregate Root)

The User is the root identity in the Identity Context. A user always has exactly one role and exactly one associated profile (either `FarmerProfile` or `VetProfile`).

```
User
├── id: UUID (aggregate root identifier)
├── email: String (unique)
├── phone: String (optional, unique)
├── passwordHash: String
├── role: Role (FARMER | VET)
├── isActive: Boolean
└── profile → FarmerProfile OR VetProfile (exactly one)
```

**Invariants:**
- A user must have either an email or a phone (or both).
- A user's role is immutable after registration.
- A user cannot exist without a corresponding profile.

---

### Animal (Aggregate Root)

An Animal is owned by a FarmerProfile. It has a unique QR code that forms the basis of the Animal Passport.

```
Animal
├── id: UUID
├── farmerId: FarmerProfile.id (owner)
├── tagNumber: String (physical ear tag)
├── qrCodeId: String (unique, used for QR scanning)
├── species: Species
├── breed: String
├── gender: Gender
├── birthDate: Date
├── name: String
└── medicalHistory → List<MedicalRecord> (read via Clinical Records context)
```

**Invariants:**
- An Animal must belong to exactly one FarmerProfile.
- `tagNumber` is unique within a FarmerProfile's herd.
- `qrCodeId` is globally unique.

---

### Appointment (Aggregate Root)

An Appointment is the scheduled or historical meeting between a Veterinarian and a Farmer about a specific Animal.

```
Appointment
├── id: UUID
├── farmerId: FarmerProfile.id
├── veterinarianId: VetProfile.id
├── animalId: Animal.id
├── appointmentDate: Date
├── appointmentTime: Time
├── visitType: VisitType
├── status: AppointmentStatus
├── reason: String
├── veterinarianNotes: String (optional)
├── cancellationReason: String (optional)
└── version: Long (optimistic locking)
```

**State Machine:**
```
PENDING ──→ CONFIRMED ──→ COMPLETED
   │              │
   └──────────────└──→ CANCELLED
```

**Invariants:**
- Only the assigned Veterinarian may confirm or complete an Appointment.
- Either the Farmer or the Veterinarian may cancel a `PENDING` or `CONFIRMED` appointment.
- A `COMPLETED` or `CANCELLED` appointment cannot change state.
- A Medical Record can only be created for a `COMPLETED` Appointment.

---

### MedicalRecord (Aggregate Root — Immutable)

A Medical Record is a permanent clinical document created by a Veterinarian for a completed Appointment.

```
MedicalRecord
├── id: UUID
├── appointmentId: Appointment.id (unique — one record per appointment)
├── animalId: Animal.id
├── farmerId: FarmerProfile.id
├── veterinarianId: VetProfile.id
├── diagnosis: String (required)
├── symptoms: String (optional)
├── treatment: String (required)
├── prescription: String (optional — TEXT, future normalization planned)
├── weight: Decimal (kg, optional)
├── temperature: Decimal (°C, optional)
├── followUpDate: Date (optional)
└── notes: String (optional)
```

**Invariants:**
- Exactly one MedicalRecord per Appointment.
- A MedicalRecord cannot be modified or deleted after creation.
- Only the Veterinarian assigned to the linked Appointment may create the record.
- A Farmer may only view MedicalRecords belonging to their own Animals.

---

## Domain Events (Planned)

Domain events are not yet implemented but are documented here as the intended evolution:

| Event | Produced By | Consumed By |
|---|---|---|
| `AppointmentBooked` | Appointment context | Notification context |
| `AppointmentConfirmed` | Appointment context | Notification context |
| `AppointmentCompleted` | Appointment context | Clinical Records context |
| `MedicalRecordCreated` | Clinical Records context | Animal Passport context |
| `DiseaseOutbreakReported` | Disease Surveillance context | Notification context |
| `AnimalRegistered` | Livestock context | Dashboard context |

---

## Anti-Corruption Layer

The Flutter client is a separate system from the backend. It must not model the backend's internal domain directly. Instead:

- Flutter has its own `model` classes (e.g., `MedicalRecordModel`) that are deserialized from the API response DTOs.
- Flutter's models use camelCase Dart conventions; the API uses snake_case JSON.
- Transformation between API JSON and Flutter models happens in the `data/` layer of each feature module.
- Flutter models are never used as API request bodies directly — separate request models or maps are used.
