# Security Design Document
**Document ID:** SEC-11  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Engineering Principles](../engineering/00-principles.md), [Auth Design](../api/07-auth-design.md), [Error Catalogue](../api/23-error-catalogue.md)

---

## Overview

This document defines Vetra's security architecture, threat model, implemented controls, and planned security improvements. Security is a first-class engineering concern — not a feature to be added later.

---

## Threat Model

### Assets

| Asset | Sensitivity | Impact if Compromised |
|---|---|---|
| User passwords | Critical | Account takeover |
| JWT signing secret | Critical | Forgery of any user's identity |
| Refresh tokens | High | Session hijacking |
| Medical records | High | Privacy violation, legal liability |
| Farmer/Vet personal data | Medium | Privacy violation |
| Animal data | Low | Competitive information |

### Threat Actors

| Actor | Capability | Motivation |
|---|---|---|
| Unauthenticated internet user | HTTP requests, common exploit tools | Data theft, vandalism |
| Authenticated farmer | Valid JWT, API access | Access other farmers' data |
| Authenticated veterinarian | Valid JWT, API access | Access unauthorized medical records |
| Compromised JWT | Stolen/forged token | Impersonate another user |
| Internal threat (developer) | Database access | Exfiltrate sensitive data |

---

## Authentication Architecture

Full details in [`docs/api/07-auth-design.md`](../api/07-auth-design.md).

### JWT Token Lifecycle

```
Login Request (email/password)
       │
       ▼
bcrypt password verification
       │ ✓
       ▼
Issue access_token (15 min, signed with JWT_SECRET)
Issue refresh_token (7 days, stored in DB)
       │
       ▼
Client stores tokens securely (Flutter SecureStorage)
       │
       ▼
Access token expires
       │
       ▼
POST /api/v1/auth/refresh  (with refresh_token)
       │
       ▼
Verify refresh_token in DB (not revoked, not expired)
       │ ✓
       ▼
Revoke old refresh_token
Issue new access_token + new refresh_token (rotation)
```

### Token Security Properties

| Property | Implementation |
|---|---|
| Signing algorithm | HMAC-SHA256 (HS256) |
| Access token expiry | 15 minutes |
| Refresh token expiry | 7 days |
| Token storage (Flutter) | `flutter_secure_storage` (Keystore/Keychain) |
| Token revocation | Refresh tokens stored in DB; revocable on logout |
| Token rotation | New refresh token issued on every use |

---

## Authorization Model (RBAC)

Vetra uses Role-Based Access Control with two roles: `FARMER` and `VET`.

### Role-to-Endpoint Matrix

| Endpoint | FARMER | VET | Notes |
|---|---|---|---|
| `POST /api/v1/auth/register/**` | Public | Public | — |
| `POST /api/v1/auth/login` | Public | Public | — |
| `POST /api/v1/auth/refresh` | Public | Public | — |
| `GET/PUT /api/v1/auth/profile` | ✅ Own only | ✅ Own only | |
| `GET /api/v1/auth/vets` | ✅ | ✅ | Vet directory |
| `GET/POST /api/v1/animals` | ✅ Own only | ❌ | |
| `GET/PUT/DELETE /api/v1/animals/{id}` | ✅ Own only | ❌ | Ownership verified |
| `GET/POST /api/v1/appointments` | ✅ Own only | ✅ Assigned only | |
| `PUT /api/v1/appointments/{id}/status` | ❌ | ✅ Assigned only | Vet updates status |
| `POST /api/v1/medical-records` | ❌ | ✅ Assigned vet only | |
| `GET /api/v1/medical-records/{id}` | ✅ Own animal only | ✅ Created by them | |
| `GET /api/v1/animals/{id}/medical-history` | ✅ Own animal only | ❌ | |
| `GET /api/v1/appointments/{id}/medical-record` | ✅ Own appointment | ✅ Assigned | |
| `GET /api/v1/dashboard` | ✅ | ✅ | Role-specific response |

### Ownership Enforcement

Authorization is enforced in the **service layer**, not the controller. The authenticated user's identity is extracted from the JWT — never from the request body or query parameters.

```java
// In MedicalRecordService
public MedicalRecordResponse createRecord(
        CreateMedicalRecordRequest request,
        UUID authenticatedVetUserId) {

    // 1. Fetch the appointment
    Appointment appointment = appointmentRepository.findById(request.appointmentId())
        .orElseThrow(() -> new EntityNotFoundException("APPT_001"));

    // 2. Resolve the vet's profile ID from their user ID
    VetProfile vet = vetProfileRepository.findByUserId(authenticatedVetUserId)
        .orElseThrow(() -> new AccessDeniedException("AUTH_006"));

    // 3. Verify ownership — the authenticated vet must own this appointment
    if (!appointment.getVeterinarianId().equals(vet.getId())) {
        throw new AccessDeniedException("MEDICAL_003");
    }
    // ...
}
```

---

## Input Validation

All incoming data is validated at the controller/DTO layer before reaching the service layer.

### Validation Strategy

1. **Bean Validation (Jakarta):** `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max` on DTO fields.
2. **Custom validators** for domain-specific rules (e.g., registration number format).
3. **SQL injection prevention:** All database access uses Spring Data JPA with parameterized queries. String concatenation in JPQL or native SQL is prohibited.
4. **Deserialization protection:** Jackson configured to reject unknown properties and to not resolve type information from request payloads.

```java
// Example DTO validation
public record CreateMedicalRecordRequest(
    @NotNull UUID appointmentId,
    @NotBlank @Size(max = 5000) String diagnosis,
    @NotBlank @Size(max = 5000) String treatment,
    @Size(max = 5000) String symptoms,
    @DecimalMin("0.0") @DecimalMax("9999.99") BigDecimal weight,
    @DecimalMin("30.0") @DecimalMax("45.0") BigDecimal temperature
) {}
```

---

## Secrets Management

| Secret | Storage | Rotation |
|---|---|---|
| `JWT_SECRET` | Environment variable → Secrets Manager (production) | On compromise; annually otherwise |
| `DB_PASSWORD` | Environment variable → Secrets Manager (production) | Every 90 days |
| Refresh tokens | Hashed in database | Per-use rotation |
| User passwords | bcrypt hash (cost factor 10) | User-initiated |

**bcrypt Cost Factor:** 10 provides ~100–200 ms verification time, which is acceptable latency for login while being computationally expensive for brute-force attacks.

---

## CORS Configuration

The API is configured to accept requests from:
- `http://localhost:*` — local development
- `https://vetra.app` — production web client (future)

Mobile apps (Flutter) do not send `Origin` headers and bypass CORS checks — they are authenticated via JWT only.

---

## Security Headers

The following HTTP security headers must be returned on all responses (planned via Spring Security):

| Header | Value |
|---|---|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `Content-Security-Policy` | Appropriate for API responses |
| `X-XSS-Protection` | `1; mode=block` |

---

## Security Audit Checklist

Before each release:

- [ ] All new endpoints have authorization enforced in service layer
- [ ] No new endpoints accept user identity from request body
- [ ] All new DTOs have input validation annotations
- [ ] No secrets committed to source control (`git log --all -- "*.env"`)
- [ ] No sensitive data in log statements
- [ ] Dependency vulnerability scan passed (OWASP)
- [ ] All new database queries use parameterized statements
- [ ] Refresh token table pruned of expired tokens

---

## Planned Security Improvements

| Improvement | Priority | Stage |
|---|---|---|
| Rate limiting (per-IP, per-user) | High | Before production |
| Account lockout after N failed logins | High | Before production |
| Audit log table (immutable) | Medium | Stage 10 |
| HTTPS enforcement (HSTS) | High | Before production |
| Security headers via Spring Security | Medium | Before production |
| Penetration test by external firm | High | Before production launch |
| OWASP dependency check in CI | High | CI/CD pipeline |
