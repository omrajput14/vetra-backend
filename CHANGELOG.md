# Changelog

All notable changes to the Vetra backend will be documented here.

## [Unreleased]

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
