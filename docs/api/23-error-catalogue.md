# Error Catalogue
**Document ID:** API-23  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`, `omrajput14/vetra` (Flutter client)  
**References:** [API Specification](./06-specification.md), [Engineering Principles](../engineering/00-principles.md)

---

## Overview

This catalogue defines every error code, HTTP status mapping, and user-facing message used across the Vetra platform. Both the backend and the Flutter client must use this catalogue consistently.

> **Rule:** If an error condition occurs and it does not have an entry in this catalogue, a new entry must be added before the code is merged.

---

## Error Response Format

All API errors return a consistent JSON envelope:

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

| Field | Type | Description |
|---|---|---|
| `code` | String | Machine-readable error code from this catalogue |
| `message` | String | Human-readable message safe to display to the user |
| `details` | Object \| null | Optional structured details (e.g., field validation errors) |
| `timestamp` | ISO 8601 | UTC time the error occurred |
| `path` | String | The request path that produced the error |

### Validation Error Details Format

For `400 Bad Request` validation errors, `details` contains field-level errors:

```json
{
  "error": {
    "code": "SYS_002",
    "message": "Request validation failed.",
    "details": {
      "fields": [
        { "field": "email", "issue": "must be a valid email address" },
        { "field": "password", "issue": "must be at least 8 characters" }
      ]
    }
  }
}
```

---

## Error Code Format

```
{DOMAIN}_{NNN}
```

- `DOMAIN` — 3–6 uppercase letters identifying the error group
- `NNN` — 3-digit zero-padded sequence number within that group

---

## Error Groups

---

### AUTH — Authentication & Authorization

| Code | HTTP Status | Description | User-Facing Message |
|---|---|---|---|
| `AUTH_001` | 401 | Invalid email or password | "Invalid credentials. Check your email or password." |
| `AUTH_002` | 401 | Access token expired | "Your session has expired. Please log in again." |
| `AUTH_003` | 401 | Access token invalid or malformed | "Authentication token is invalid." |
| `AUTH_004` | 401 | Refresh token expired | "Your session has expired. Please log in again." |
| `AUTH_005` | 401 | Refresh token not found or revoked | "Your session is no longer valid. Please log in again." |
| `AUTH_006` | 403 | Authenticated user lacks permission for this resource | "You do not have permission to perform this action." |
| `AUTH_007` | 403 | Farmer attempting to access another farmer's resource | "Access denied. This resource belongs to another account." |
| `AUTH_008` | 403 | Veterinarian attempting to access another vet's appointments | "Access denied. This appointment is not assigned to you." |
| `AUTH_009` | 401 | Missing Authorization header | "Authentication is required. Please log in." |
| `AUTH_010` | 403 | Account is inactive or suspended | "Your account has been suspended. Contact support." |

---

### USER — User & Profile Management

| Code | HTTP Status | Description | User-Facing Message |
|---|---|---|---|
| `USER_001` | 409 | Email already registered | "This email address is already in use." |
| `USER_002` | 409 | Phone number already registered | "This phone number is already in use." |
| `USER_003` | 409 | Registration number already registered | "This veterinary registration number is already in use." |
| `USER_004` | 404 | User not found | "User account not found." |
| `USER_005` | 400 | Neither email nor phone provided | "Please provide either an email address or a phone number." |
| `USER_006` | 400 | Invalid phone number format | "Please enter a valid phone number." |
| `USER_007` | 400 | Password does not meet requirements | "Password must be at least 8 characters long." |

---

### ANIMAL — Animal Management

| Code | HTTP Status | Description | User-Facing Message |
|---|---|---|---|
| `ANIMAL_001` | 404 | Animal not found | "Animal record not found." |
| `ANIMAL_002` | 403 | Animal belongs to a different farmer | "Access denied. This animal is not registered to your account." |
| `ANIMAL_003` | 409 | QR code ID already in use | "This QR code is already registered to another animal." |
| `ANIMAL_004` | 400 | Invalid species value | "Please select a valid species." |
| `ANIMAL_005` | 400 | Invalid gender value | "Please select a valid gender." |
| `ANIMAL_006` | 422 | Animal cannot be deleted — has active appointments | "This animal has active appointments. Cancel them before removing the animal." |

---

### APPT — Appointment Management

| Code | HTTP Status | Description | User-Facing Message |
|---|---|---|---|
| `APPT_001` | 404 | Appointment not found | "Appointment not found." |
| `APPT_002` | 403 | User is not a party to this appointment | "Access denied. You are not a party to this appointment." |
| `APPT_003` | 403 | Veterinarian cannot modify another vet's appointment | "This appointment is not assigned to you." |
| `APPT_004` | 422 | Invalid status transition | "This appointment cannot be moved to the requested status from its current state." |
| `APPT_005` | 422 | Appointment is already in a terminal state | "This appointment is already completed or cancelled and cannot be changed." |
| `APPT_006` | 409 | Optimistic lock conflict — concurrent update detected | "This appointment was updated by another request. Please refresh and try again." |
| `APPT_007` | 400 | Appointment date is in the past | "Appointment date must be today or in the future." |
| `APPT_008` | 422 | Cannot create medical record — appointment not completed | "A medical record can only be created for a completed appointment." |

---

### MEDICAL — Medical Records (EVMR)

| Code | HTTP Status | Description | User-Facing Message |
|---|---|---|---|
| `MEDICAL_001` | 404 | Medical record not found | "Medical record not found." |
| `MEDICAL_002` | 403 | Farmer requesting another farmer's medical record | "Access denied. This medical record belongs to another account." |
| `MEDICAL_003` | 403 | Vet creating record for another vet's appointment | "Access denied. This appointment is not assigned to you." |
| `MEDICAL_004` | 409 | Medical record already exists for this appointment | "A medical record has already been created for this appointment." |
| `MEDICAL_005` | 422 | Missing required clinical fields | "Diagnosis and treatment are required fields." |

---

### SYS — System & Infrastructure

| Code | HTTP Status | Description | User-Facing Message |
|---|---|---|---|
| `SYS_001` | 500 | Unhandled internal server error | "An unexpected error occurred. Please try again or contact support." |
| `SYS_002` | 400 | Request body validation failure | "Request validation failed. Check the details for field-level errors." |
| `SYS_003` | 400 | Malformed JSON in request body | "The request body is not valid JSON." |
| `SYS_004` | 415 | Unsupported content type | "Content-Type must be application/json." |
| `SYS_005` | 429 | Rate limit exceeded | "Too many requests. Please wait before trying again." |
| `SYS_006` | 503 | Database unavailable | "The service is temporarily unavailable. Please try again shortly." |
| `SYS_007` | 404 | Route not found | "The requested endpoint does not exist." |
| `SYS_008` | 405 | HTTP method not allowed | "HTTP method not supported for this endpoint." |

---

## Flutter Client Error Handling

The Flutter client maps backend error codes to UI behavior using the following strategy:

### Error Code → UI Behavior Matrix

| Code Range | Flutter Behavior |
|---|---|
| `AUTH_001` | Show login error snackbar, clear password field |
| `AUTH_002`, `AUTH_004` | Clear stored tokens, redirect to login |
| `AUTH_003`, `AUTH_005`, `AUTH_009` | Clear stored tokens, redirect to login |
| `AUTH_006`, `AUTH_007`, `AUTH_008` | Show permission denied dialog |
| `AUTH_010` | Show account suspended screen |
| `USER_001`, `USER_002`, `USER_003` | Show inline field error on registration form |
| `ANIMAL_001`, `APPT_001`, `MEDICAL_001` | Show "Not found" empty state |
| `ANIMAL_002`, `APPT_002`, `APPT_003` | Show permission denied error screen |
| `APPT_006` | Show "please refresh" snackbar, reload data |
| `APPT_005`, `MEDICAL_004` | Show informational dialog |
| `SYS_001` | Show generic error snackbar with retry option |
| `SYS_002` | Show field validation errors inline |
| `SYS_005` | Show rate limit warning, disable submit button for 10s |
| `SYS_006` | Show offline/maintenance banner |

### Implementation Pattern (Flutter)

```dart
class ApiException implements Exception {
  final String code;
  final String message;
  final int statusCode;

  ApiException({
    required this.code,
    required this.message,
    required this.statusCode,
  });

  bool get isAuthError => code.startsWith('AUTH_');
  bool get isSessionExpired => code == 'AUTH_002' || code == 'AUTH_004';
  bool get isPermissionDenied => code == 'AUTH_006' || statusCode == 403;
  bool get isConflict => statusCode == 409;
  bool get isNotFound => statusCode == 404;
  bool get isServerError => statusCode >= 500;
}
```

---

## Adding New Error Codes

When adding a new error condition:

1. Identify the correct domain group (AUTH, USER, ANIMAL, APPT, MEDICAL, SYS).
2. Assign the next available sequence number in that group.
3. Add the entry to this catalogue with HTTP status, description, and user-facing message.
4. Update the Flutter error handling matrix if a new UI behavior pattern is needed.
5. Include both the catalogue update and the implementation in the same PR.

> [!IMPORTANT]
> Error codes must never be reused. If an error code is retired, its slot stays empty in the catalogue with a "RETIRED" note. This prevents confusion if older log entries reference it.
