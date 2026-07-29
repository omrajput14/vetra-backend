# API Versioning Strategy
**Document ID:** API-22  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [API Specification](./06-specification.md), [Engineering Principles](../engineering/00-principles.md)

---

## Overview

This document defines how Vetra manages API versioning, deprecation, and backward compatibility. It is the authoritative reference for any change that affects the public API contract between the backend and the Flutter client.

---

## Versioning Strategy

Vetra uses **URL path versioning**:

```
/api/v{major}/{resource}
```

**Examples:**
```
GET  /api/v1/animals
POST /api/v1/appointments
GET  /api/v1/medical-records/{id}
```

### Why URL Versioning

URL versioning was chosen over header-based or query-parameter versioning because:
1. **Visible in logs and monitoring** — the version is immediately apparent in access logs, request traces, and browser network tabs without custom parsing.
2. **Easy to route** — load balancers and API gateways can route by URL prefix without inspecting headers.
3. **Easy to deprecate** — a version can be sunset at the infrastructure level (return `410 Gone`) without modifying application code.
4. **Industry standard** — used by Stripe, GitHub, Twilio, and most production REST APIs.

### What Constitutes a Version

The URL version corresponds to the API's **major version** in semantic versioning terms:

| Change Type | Version Impact | Examples |
|---|---|---|
| New endpoint | None (additive) | `POST /api/v1/prescriptions` |
| New optional field in response | None (additive) | Adding `clinic_address` to vet response |
| New optional request field | None (additive) | Adding optional `notes` to a create request |
| Renamed field | **Breaking** | `full_name` → `name` |
| Removed field | **Breaking** | Removing `registration_number` from vet response |
| Changed field type | **Breaking** | `weight: String` → `weight: Number` |
| Changed HTTP status code | **Breaking** | `200` → `204` for deletion |
| Changed authentication requirement | **Breaking** | Removing auth from a protected endpoint |
| Changed business rule | **May be breaking** | Evaluated case by case |

---

## Current Versions

| Version | Status | Base Path | Released |
|---|---|---|---|
| `v1` | **Active** | `/api/v1/` | Stage 1 (2026-07-27) |

---

## Breaking Change Policy

### Definition

A breaking change is any modification to the API that requires the client (Flutter app) to update its code to continue functioning correctly.

### Process for Breaking Changes

1. **Propose** — document the breaking change in the Architecture Decision Log before any code changes.
2. **New version** — implement the new behavior under `/api/v2/`. Do not modify the existing v1 behavior.
3. **Deprecate v1** — mark the affected v1 endpoint as deprecated (see below).
4. **Minimum notice period** — maintain v1 alongside v2 for a minimum of **3 months**.
5. **Client migration** — coordinate Flutter app update to use v2 endpoints.
6. **Sunset v1** — after the notice period and confirmed client migration, remove v1 (return `410 Gone`).

### Emergency Breaking Changes

For security-critical changes (e.g., a vulnerability in an auth endpoint), the notice period may be reduced. The decision and rationale must be documented in the Architecture Decision Log.

---

## Deprecation Policy

### Marking an Endpoint as Deprecated

When an endpoint is deprecated:

1. Add it to the deprecation table in this document.
2. Return the `Deprecation` response header:
   ```
   Deprecation: true
   Sunset: Sat, 01 Nov 2026 00:00:00 GMT
   Link: <https://api.vetra.app/api/v2/resource>; rel="successor-version"
   ```
3. Document in [`docs/api/06-specification.md`](./06-specification.md) with a `⚠️ DEPRECATED` notice.
4. Log a warning in the backend for every call to the deprecated endpoint.

### Current Deprecations

| Endpoint | Deprecated Since | Sunset Date | Replacement |
|---|---|---|---|
| — | — | — | — |

*No endpoints are currently deprecated.*

---

## Backward Compatibility Rules

To avoid breaking existing clients without a version bump, all changes must be **additive only** within a version:

### ✅ Allowed (Additive)

- Adding a new endpoint
- Adding a new optional field to a response body
- Adding a new optional field to a request body
- Adding a new valid enum value to a response field
- Adding a new HTTP header
- Making a previously required request field optional
- Returning additional HTTP 2xx status codes

### ❌ Not Allowed (Breaking)

- Removing a field from a response
- Renaming a field
- Changing the type of a field
- Removing an endpoint
- Making a previously optional request field required
- Changing the HTTP method of an endpoint
- Changing authentication requirements
- Removing a previously valid enum value

### Special Case: New Required Request Fields

If a new required field must be added to an existing endpoint, it must go to a new version. The old version should accept requests without that field, applying a sensible default.

---

## Version Lifecycle

```
Development → Active → Deprecated → Sunset
```

| Phase | Description |
|---|---|
| **Development** | Version is in implementation. Not available externally. |
| **Active** | Version is production-ready and fully supported. |
| **Deprecated** | Version continues to work but is marked for removal. No new features. |
| **Sunset** | Version returns `410 Gone` with a deprecation message body. |

---

## Client Compatibility Contract

The Flutter client (`omrajput14/vetra`) targets a specific API version. The current contract:

| Client Version | API Version | Status |
|---|---|---|
| 0.7.x | v1 | Active |

When a new API version is released:
1. The Flutter team is notified via a documented PR in the backend repo.
2. The Flutter app is updated to use the new version within the deprecation period.
3. The client version targeting the new API version is documented here.
