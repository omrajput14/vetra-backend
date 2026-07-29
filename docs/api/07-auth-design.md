# Authentication & Authorization Design
**Document ID:** API-07  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Security Design](../security/11-security-design.md), [API Specification](./06-specification.md), [Error Catalogue](./23-error-catalogue.md)

---

## Overview

Vetra uses **stateless JWT-based authentication** with **server-side refresh tokens** and **role-based access control (RBAC)**. This document describes the complete authentication lifecycle, token model, Spring Security configuration, and authorization enforcement patterns.

---

## Token Model

### Access Token

| Property | Value |
|---|---|
| Format | JSON Web Token (JWT) |
| Algorithm | HMAC-SHA256 (HS256) |
| Signing key | `JWT_SECRET` environment variable (minimum 256 bits) |
| Expiry | 15 minutes |
| Claims | `sub` (userId), `role`, `iat`, `exp` |
| Storage (Flutter) | In-memory (not persisted across app restarts) |

**Access token payload:**
```json
{
  "sub": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "role": "FARMER",
  "iat": 1722240000,
  "exp": 1722240900
}
```

### Refresh Token

| Property | Value |
|---|---|
| Format | Opaque UUID string |
| Expiry | 7 days |
| Storage (server) | `refresh_tokens` table, hashed |
| Storage (Flutter) | `flutter_secure_storage` (Android Keystore / iOS Keychain) |
| Rotation | New token issued on every use; old token revoked |

---

## Authentication Flow

### Login

```
POST /api/v1/auth/login
{ "email": "farmer@example.com", "password": "password123" }
```

```
1. Look up user by email or phone
2. Verify bcrypt(password) against password_hash
3. If incorrect → 401 AUTH_001
4. If account inactive → 403 AUTH_010
5. Generate access_token (15 min, signed JWT)
6. Generate refresh_token (UUID)
7. Hash refresh_token and store in refresh_tokens table
8. Return: { access_token, refresh_token, role, expires_in }
```

### Token Refresh

```
POST /api/v1/auth/refresh
{ "refreshToken": "..." }
```

```
1. Hash the provided refresh_token
2. Look up in refresh_tokens table
3. If not found → 401 AUTH_005
4. If expired → 401 AUTH_004
5. If revoked → 401 AUTH_005
6. Mark old token as revoked = TRUE
7. Generate new access_token + new refresh_token
8. Store new refresh_token (hashed)
9. Return: { access_token, refresh_token, expires_in }
```

### Logout

```
POST /api/v1/auth/logout
Authorization: Bearer <access_token>
{ "refreshToken": "..." }
```

```
1. Validate access_token (must be valid, not expired)
2. Find refresh_token in DB
3. Mark as revoked = TRUE
4. Return 204 No Content
```

---

## Spring Security Configuration

### Filter Chain

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .csrf(csrf -> csrf.disable())  // Disabled — API uses JWT, not cookies
        .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/register/**").permitAll()
            .requestMatchers("/api/v1/auth/login").permitAll()
            .requestMatchers("/api/v1/auth/refresh").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### JwtAuthFilter

The `JwtAuthFilter` runs on every request to a protected endpoint:

```
Request arrives
    │
    ▼
Extract "Authorization: Bearer <token>" header
    │ No header → proceed without authentication (will fail in authorizeHttpRequests)
    ▼
Parse and validate JWT:
  - Signature valid? (HMAC-SHA256 with JWT_SECRET)
  - Not expired?
  - Claims present (sub, role)?
    │ Invalid → 401 AUTH_003
    ▼
Load UserDetails from database (fetch user by userId from claims)
    │ User not found → 401 AUTH_003
    │ User inactive → 403 AUTH_010
    ▼
Set Authentication in SecurityContextHolder
    │
    ▼
Proceed to controller
```

---

## Authorization Enforcement Pattern

Authorization is a **two-step process** enforced in the **service layer**:

### Step 1: Role Check (Spring Security)

Spring Security's `authorizeHttpRequests` ensures the authenticated user has the required role:

```java
.requestMatchers(HttpMethod.POST, "/api/v1/medical-records").hasRole("VET")
.requestMatchers(HttpMethod.GET, "/api/v1/animals/**").hasRole("FARMER")
```

### Step 2: Ownership Check (Service Layer)

Role checks are necessary but not sufficient. After confirming the role, the service layer verifies that the authenticated user owns the resource being accessed:

```java
@GetMapping("/{id}")
public ResponseEntity<AnimalResponse> getAnimal(@PathVariable UUID id,
        @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(animalService.getAnimal(id, UUID.fromString(userDetails.getUsername())));
}

// In AnimalService:
public AnimalResponse getAnimal(UUID animalId, UUID authenticatedUserId) {
    Animal animal = animalRepository.findById(animalId)
        .orElseThrow(() -> new EntityNotFoundException("ANIMAL_001"));

    FarmerProfile farmer = farmerProfileRepository.findByUserId(authenticatedUserId)
        .orElseThrow(() -> new AccessDeniedException("AUTH_006"));

    if (!animal.getFarmerId().equals(farmer.getId())) {
        throw new AccessDeniedException("ANIMAL_002"); // Not their animal
    }
    return mapToResponse(animal);
}
```

> **Rule:** The authenticated user's identity is always extracted from the JWT via `@AuthenticationPrincipal`. The request body or query parameters must never be trusted for identity.

---

## ID Enumeration Prevention

UUID primary keys prevent sequential ID enumeration. However, the API must also ensure:
- A `404 Not Found` is returned for resources that exist but belong to another user — **not** `403 Forbidden`. This prevents attackers from confirming resource existence.
- Timing side-channels are avoided (response time is consistent regardless of whether the resource exists or belongs to another user).

Implementation:
```java
// Check existence AND ownership in one query:
Optional<Animal> animal = animalRepository.findByIdAndFarmerId(animalId, farmer.getId());
if (animal.isEmpty()) {
    throw new EntityNotFoundException("ANIMAL_001"); // Always 404 — never reveal 403
}
```

---

## Multi-Device Session Support

A user may be logged in on multiple devices simultaneously. Each device holds:
- Its own refresh token (stored in `refresh_tokens` table)
- Its own access token (in-memory, not stored)

Logging out on one device does not affect other devices. A "logout all devices" feature would revoke all refresh tokens for a user:
```sql
UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = ? AND revoked = FALSE;
```
This feature is not currently implemented but is planned.
