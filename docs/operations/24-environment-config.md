# Environment Configuration
**Document ID:** OPS-24  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend` (primary), `omrajput14/vetra` (Flutter)  
**References:** [Engineering Principles](../engineering/00-principles.md), [Security Design](../security/11-security-design.md), [Deployment Architecture](../architecture/09-deployment.md)

---

## Overview

This document defines every runtime environment in the Vetra platform, the configuration variables required in each, and the strategy for managing secrets. Any new environment variable must be documented here before it is used in code.

> **Rule:** Secrets are never hardcoded. Environment variables are never committed to source control. `.env` files are gitignored. The only committed file is `.env.example`.

---

## Environments

Vetra defines the following runtime environments:

| Environment | Purpose | Audience | Database |
|---|---|---|---|
| `local` | Individual developer machine | Developer only | Docker Compose PostgreSQL |
| `dev` | Shared development server | Engineering team | Shared dev DB |
| `qa` | Quality assurance testing | QA team | Isolated QA DB |
| `staging` | Pre-production validation | Engineering + stakeholders | Staging DB (production-like) |
| `production` | Live platform | End users | Production DB |

> **Current Status:** Only `local` is fully operational. `dev`, `qa`, `staging`, and `production` are planned infrastructure. See [CI/CD Pipeline](./15-cicd.md) for the deployment roadmap.

---

## Spring Profile Naming

Spring Boot profiles map to environments:

| Environment | Spring Profile | Activated By |
|---|---|---|
| `local` | `dev` | `SPRING_PROFILES_ACTIVE=dev` |
| `dev` | `dev` | `SPRING_PROFILES_ACTIVE=dev` |
| `qa` | `qa` | `SPRING_PROFILES_ACTIVE=qa` |
| `staging` | `staging` | `SPRING_PROFILES_ACTIVE=staging` |
| `production` | `prod` | `SPRING_PROFILES_ACTIVE=prod` |

Spring profile configuration files:
```
src/main/resources/
├── application.yml          ← Shared defaults
├── application-dev.yml      ← Dev/local overrides
├── application-qa.yml       ← QA overrides (planned)
├── application-staging.yml  ← Staging overrides (planned)
└── application-prod.yml     ← Production overrides (planned)
```

---

## Backend Environment Variables

### Core Application

| Variable | Required | Example | Description |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | `dev` | Active Spring profile |
| `SERVER_PORT` | No | `8080` | HTTP server port. Default: `8080`. |

### Database

| Variable | Required | Example | Description |
|---|---|---|---|
| `DB_URL` | Yes | `jdbc:postgresql://localhost:5432/vetra_db` | Full JDBC connection URL |
| `DB_USERNAME` | Yes | `vetra_user` | PostgreSQL username |
| `DB_PASSWORD` | Yes | `changeme_local` | PostgreSQL password. **Never commit a real value.** |
| `DB_POOL_SIZE` | No | `10` | HikariCP connection pool size. Default: `10`. |

### Security / JWT

| Variable | Required | Example | Description |
|---|---|---|---|
| `JWT_SECRET` | Yes | `your-256-bit-base64-secret` | HMAC-SHA256 signing key. Minimum 256 bits (32 bytes). Must be randomly generated. |
| `JWT_ACCESS_EXPIRY_MS` | No | `900000` | Access token lifetime in milliseconds. Default: `900000` (15 minutes). |
| `JWT_REFRESH_EXPIRY_MS` | No | `604800000` | Refresh token lifetime in milliseconds. Default: `604800000` (7 days). |

### Docker Compose (local only — in `docker-compose.dev.yml`)

| Variable | Description |
|---|---|
| `POSTGRES_DB` | Database name (`vetra_db`) |
| `POSTGRES_USER` | Database user (`vetra_user`) |
| `POSTGRES_PASSWORD` | Database password (local only — use weak password) |

---

## `.env.example` — The Source of Truth

The `.env.example` file in the repository root documents every required variable with placeholder values. When setting up a local environment, copy this file:

```bash
cp .env.example .env
# Edit .env with real local values
```

Current `.env.example` content:
```dotenv
# ─── Spring ───────────────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

# ─── Database ─────────────────────────────────────────────────────────
DB_URL=jdbc:postgresql://localhost:5432/vetra_db
DB_USERNAME=vetra_user
DB_PASSWORD=changeme_local
DB_POOL_SIZE=10

# ─── JWT ──────────────────────────────────────────────────────────────
# Generate with: openssl rand -base64 32
JWT_SECRET=REPLACE_WITH_SECURE_RANDOM_BASE64_STRING
JWT_ACCESS_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=604800000
```

> [!CAUTION]
> The `.env` file must never be committed to source control. Verify `.gitignore` includes `.env` before creating the file.

---

## JWT Secret Generation

For local development, generate a secure secret with:

```bash
openssl rand -base64 32
```

For production:
- Minimum 256 bits (32 bytes) of cryptographically random data
- Stored in a secrets manager (AWS Secrets Manager, HashiCorp Vault, or GCP Secret Manager) — not in environment files
- Rotated on a schedule or after any suspected compromise

---

## Secrets Management Strategy

| Environment | Secrets Storage | Method |
|---|---|---|
| `local` | `.env` file (gitignored) | Manual copy from `.env.example` |
| `dev` | Environment variables on server | Set by deployment script |
| `qa` | Environment variables on server | Set by CI/CD pipeline |
| `staging` | Secrets Manager (planned) | Injected at deploy time |
| `production` | Secrets Manager (planned) | Injected at deploy time, never in files |

### Production Secrets Principles

- Secrets never appear in source code, Docker images, or CI/CD logs.
- Database passwords are rotated every 90 days.
- JWT secrets are rotated on any suspected compromise, requiring all users to re-authenticate.
- Access to production secrets is logged and audited.

---

## Flutter Environment Configuration

The Flutter app has no direct secrets but is configured with the backend URL.

| Variable | Location | Description |
|---|---|---|
| `BASE_URL` | `lib/core/config/app_config.dart` | Backend API base URL |

```dart
// lib/core/config/app_config.dart
class AppConfig {
  static const String baseUrl = String.fromEnvironment(
    'BASE_URL',
    defaultValue: 'http://10.0.2.2:8080', // Android emulator → localhost
  );
}
```

### Flutter Environment URLs

| Environment | URL |
|---|---|
| `local` (Android emulator) | `http://10.0.2.2:8080` |
| `local` (physical device) | `http://<your-machine-local-ip>:8080` |
| `dev` | `http://dev.vetra.app` (planned) |
| `staging` | `https://staging.vetra.app` (planned) |
| `production` | `https://api.vetra.app` (planned) |

Build with a specific environment:
```bash
# Development build
flutter run --dart-define=BASE_URL=http://10.0.2.2:8080

# Staging build
flutter build apk --dart-define=BASE_URL=https://staging.vetra.app
```

---

## Adding a New Environment Variable

1. Add the variable to `.env.example` with a placeholder value and comment.
2. Add it to this document with description, whether it is required, and the default value.
3. Reference it in `application.yml` (backend) or `app_config.dart` (Flutter).
4. Document in the PR description.

> [!WARNING]
> Never add an environment variable with a default value that is secure enough for production. Defaults are for developer convenience only. Production values must always be explicitly set.
