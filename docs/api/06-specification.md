# API Specification (OpenAPI / REST Reference)
**Document ID:** API-06  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Auth Design](./07-auth-design.md), [API Versioning](./22-versioning.md), [Error Catalogue](./23-error-catalogue.md)

---

## Overview

This specification documents all public REST API endpoints provided by `vetra-backend` v1. 

- **Base URL:** `/api/v1`
- **Protocol:** HTTPS (HTTP for local dev)
- **Data Format:** JSON (`Content-Type: application/json`)
- **Authentication:** Bearer Token (JWT in `Authorization` header) unless marked Public.

---

## Common Structures

### Standard Error Response Envelope

All non-2xx responses return this format:

```json
{
  "error": {
    "code": "AUTH_001",
    "message": "Invalid credentials. Check your email or password.",
    "details": null,
    "timestamp": "2026-07-29T12:00:00Z",
    "path": "/api/v1/auth/login"
  }
}
```

---

## 1. Authentication Module (`/api/v1/auth`)

### 1.1 Register Farmer Account
- **Endpoint:** `POST /api/v1/auth/register/farmer`
- **Auth:** Public
- **Description:** Registers a new user with the `FARMER` role and creates their initial `farmer_profile`.

**Request Body:**
```json
{
  "email": "farmer@example.com",
  "phone": "+919876543210",
  "password": "SecurePassword123!",
  "fullName": "Ramesh Kumar",
  "farmName": "Green Valley Dairy",
  "village": "Kheri",
  "district": "Karnal",
  "state": "Haryana",
  "latitude": 29.6857,
  "longitude": 76.9905
}
```

**Response (201 Created):**
```json
{
  "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email": "farmer@example.com",
  "role": "FARMER",
  "profileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "fullName": "Ramesh Kumar"
}
```

**Error Responses:**
- `409 Conflict`: `USER_001` (Email registered), `USER_002` (Phone registered)
- `400 Bad Request`: `SYS_002` (Validation failure)

---

### 1.2 Register Veterinarian Account
- **Endpoint:** `POST /api/v1/auth/register/vet`
- **Auth:** Public
- **Description:** Registers a new user with the `VET` role and creates their `vet_profile`.

**Request Body:**
```json
{
  "email": "dr.sharma@example.com",
  "phone": "+919876543211",
  "password": "VetPassword123!",
  "fullName": "Dr. Suresh Sharma",
  "registrationNumber": "VET-HAR-2024-8891",
  "qualification": "B.V.Sc & A.H., M.V.Sc (Surgery)",
  "specialization": "Bovine Surgery & Reproduction",
  "clinicName": "Karnal Veterinary Clinic",
  "yearsExperience": 12,
  "latitude": 29.6900,
  "longitude": 76.9800
}
```

**Response (201 Created):**
```json
{
  "userId": "e89ac10b-58cc-4372-a567-0e02b2c3d999",
  "email": "dr.sharma@example.com",
  "role": "VET",
  "profileId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "fullName": "Dr. Suresh Sharma",
  "registrationNumber": "VET-HAR-2024-8891"
}
```

**Error Responses:**
- `409 Conflict`: `USER_001` (Email registered), `USER_003` (License registered)

---

### 1.3 Login
- **Endpoint:** `POST /api/v1/auth/login`
- **Auth:** Public
- **Description:** Authenticates credentials and returns short-lived access JWT + long-lived refresh token.

**Request Body:**
```json
{
  "email": "farmer@example.com",
  "password": "SecurePassword123!"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "role": "FARMER",
  "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

**Error Responses:**
- `401 Unauthorized`: `AUTH_001` (Invalid credentials)
- `403 Forbidden`: `AUTH_010` (Account inactive)

---

### 1.4 Refresh Token
- **Endpoint:** `POST /api/v1/auth/refresh`
- **Auth:** Public
- **Description:** Rotates refresh token and issues a fresh access token.

**Request Body:**
```json
{
  "refreshToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "8d0f7780-8536-51ef-a55c-f18gd2ga1bf8",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

---

### 1.5 Get Vet Directory
- **Endpoint:** `GET /api/v1/auth/vets`
- **Auth:** Required (FARMER or VET)
- **Description:** Fetches all available veterinarian profiles (for nearby vet directory and booking).

**Response (200 OK):**
```json
[
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "userId": "e89ac10b-58cc-4372-a567-0e02b2c3d999",
    "fullName": "Dr. Suresh Sharma",
    "registrationNumber": "VET-HAR-2024-8891",
    "qualification": "B.V.Sc & A.H.",
    "specialization": "Bovine Surgery",
    "clinicName": "Karnal Vet Care",
    "yearsExperience": 12,
    "isAvailable": true,
    "latitude": 29.6900,
    "longitude": 76.9800
  }
]
```

---

## 2. Animal Management Module (`/api/v1/animals`)

### 2.1 Register Animal
- **Endpoint:** `POST /api/v1/animals`
- **Auth:** Required (FARMER only)
- **Description:** Registers a new livestock animal under the authenticated farmer's account.

**Request Body:**
```json
{
  "tagNumber": "TAG-88219",
  "qrCodeId": "QR-PASS-2026-9011",
  "species": "CATTLE",
  "breed": "Holstein Friesian Cross",
  "gender": "FEMALE",
  "birthDate": "2022-04-15",
  "name": "Gauri"
}
```

**Response (201 Created):**
```json
{
  "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "farmerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tagNumber": "TAG-88219",
  "qrCodeId": "QR-PASS-2026-9011",
  "species": "CATTLE",
  "breed": "Holstein Friesian Cross",
  "gender": "FEMALE",
  "birthDate": "2022-04-15",
  "name": "Gauri",
  "createdAt": "2026-07-29T10:00:00Z"
}
```

---

### 2.2 Get Farmer's Animals
- **Endpoint:** `GET /api/v1/animals`
- **Auth:** Required (FARMER only)
- **Description:** Returns all animals belonging to the authenticated farmer.

**Response (200 OK):** List of `AnimalResponse` objects.

---

### 2.3 Get Animal by ID
- **Endpoint:** `GET /api/v1/animals/{id}`
- **Auth:** Required (FARMER only)
- **Description:** Returns single animal passport by ID if owned by authenticated farmer.

---

## 3. Appointment Management Module (`/api/v1/appointments`)

### 3.1 Book Appointment
- **Endpoint:** `POST /api/v1/appointments`
- **Auth:** Required (FARMER only)
- **Description:** Books a clinical appointment for an animal with a selected veterinarian. Initial status is `PENDING`.

**Request Body:**
```json
{
  "veterinarianId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "animalId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "appointmentDate": "2026-08-01",
  "appointmentTime": "10:30:00",
  "visitType": "TREATMENT",
  "reason": "Animal has high fever and reduced milk yield since yesterday morning."
}
```

**Response (201 Created):**
```json
{
  "id": "d4e5f6a7-b8c9-0123-def0-234567890123",
  "farmerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "veterinarianId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "animalId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "animalName": "Gauri",
  "farmerName": "Ramesh Kumar",
  "veterinarianName": "Dr. Suresh Sharma",
  "appointmentDate": "2026-08-01",
  "appointmentTime": "10:30:00",
  "visitType": "TREATMENT",
  "status": "PENDING",
  "reason": "Animal has high fever and reduced milk yield...",
  "version": 0
}
```

---

### 3.2 List Appointments
- **Endpoint:** `GET /api/v1/appointments`
- **Auth:** Required (FARMER or VET)
- **Description:** Returns appointments related to the user (farmer's booked appointments or vet's assigned appointments).

---

### 3.3 Update Appointment Status
- **Endpoint:** `PUT /api/v1/appointments/{id}/status`
- **Auth:** Required (VET or FARMER depending on transition)
- **Description:** Executes state machine transition (`PENDING → CONFIRMED → COMPLETED` or `CANCELLED`). Uses optimistic locking.

**Request Body:**
```json
{
  "status": "CONFIRMED",
  "veterinarianNotes": "Confirmed for morning visit. Please keep animal in shaded area."
}
```

**Response (200 OK):** Updated `AppointmentResponse` object.

---

## 4. Electronic Veterinary Medical Records (EVMR) Module (`/api/v1/medical-records`)

### 4.1 Create Medical Record
- **Endpoint:** `POST /api/v1/medical-records`
- **Auth:** Required (VET only — must be assigned to appointment)
- **Description:** Creates an immutable clinical medical record for a `COMPLETED` appointment.

**Request Body:**
```json
{
  "appointmentId": "d4e5f6a7-b8c9-0123-def0-234567890123",
  "diagnosis": "Bovine Ephemeral Fever (Three-day Sickness)",
  "symptoms": "High fever (40.5°C), muscle stiffness, nasal discharge, anorexia.",
  "treatment": "Administered Flunixin Meglumine IV and broad-spectrum antibiotic cover.",
  "prescription": "Oxytetracycline 20% LA - 20ml IM single dose.\nMeloxicam bolus - 2 bolus twice daily for 3 days.",
  "weight": 420.50,
  "temperature": 40.5,
  "followUpDate": "2026-08-04",
  "notes": "Advised farmer to provide fresh water and soft palatable green fodder."
}
```

**Response (201 Created):**
```json
{
  "id": "e5f6a7b8-c9d0-1234-ef01-345678901234",
  "appointmentId": "d4e5f6a7-b8c9-0123-def0-234567890123",
  "animalId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "farmerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "veterinarianId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "veterinarianName": "Dr. Suresh Sharma",
  "diagnosis": "Bovine Ephemeral Fever (Three-day Sickness)",
  "symptoms": "High fever (40.5°C)...",
  "treatment": "Administered Flunixin Meglumine...",
  "prescription": "Oxytetracycline 20% LA...",
  "weight": 420.50,
  "temperature": 40.5,
  "followUpDate": "2026-08-04",
  "notes": "Advised farmer...",
  "createdAt": "2026-08-01T11:45:00Z"
}
```

**Error Responses:**
- `409 Conflict`: `MEDICAL_004` (Record already exists for this appointment)
- `422 Unprocessable`: `APPT_008` (Appointment is not COMPLETED)
- `403 Forbidden`: `MEDICAL_003` (Vet not assigned to appointment)

---

### 4.2 Get Medical Record by ID
- **Endpoint:** `GET /api/v1/medical-records/{id}`
- **Auth:** Required (FARMER owning animal OR VET who created it)
- **Description:** Returns single medical record by ID.

---

### 4.3 Get Animal Medical History Timeline
- **Endpoint:** `GET /api/v1/animals/{animalId}/medical-history`
- **Auth:** Required (FARMER owning animal)
- **Description:** Returns full chronological clinical history for an animal.

---

## 5. Dashboard Aggregation Module (`/api/v1/dashboard`)

### 5.1 Get Role-Specific Dashboard Metrics
- **Endpoint:** `GET /api/v1/dashboard`
- **Auth:** Required (FARMER or VET)
- **Description:** Aggregates metrics and upcoming activity for the authenticated user based on their role.

**Farmer Response Example (200 OK):**
```json
{
  "role": "FARMER",
  "totalAnimals": 12,
  "pendingAppointments": 1,
  "upcomingAppointments": [ ... ],
  "recentMedicalRecords": [ ... ]
}
```

**Vet Response Example (200 OK):**
```json
{
  "role": "VET",
  "pendingRequests": 3,
  "todayAppointments": 2,
  "completedConsultations": 48,
  "upcomingSchedule": [ ... ]
}
```
