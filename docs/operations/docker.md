# Vetra Backend Core — Docker & Containerization Operations Guide

This document specifies the operational guidelines for building, running, and troubleshooting the containerized **Vetra Backend Platform** using Docker and Docker Compose.

---

## 1. Architecture Overview

The containerized local production environment consists of two isolated services running on a dedicated bridge network (`vetra-network`):

```
                     ┌─────────────────────────────────────────┐
                     │          Host Environment               │
                     └────────────────────┬────────────────────┘
                                          │  Port 8080 / 5432
                                          ▼
 ┌──────────────────────────────────────────────────────────────────────────┐
 │                         vetra-network (Bridge)                           │
 │                                                                          │
 │   ┌────────────────────────┐             ┌───────────────────────────┐   │
 │   │     vetra-backend      │────────────►│         postgres          │   │
 │   │  (Spring Boot JRE 21)  │  JDBC 5432  │  (PostgreSQL 17 + PostGIS)│   │
 │   │   Port: 8080           │             │   Port: 5432              │   │
 │   └────────────────────────┘             └───────────────────────────┘   │
 └──────────────────────────────────────────────────────────────────────────┘
```

1. **`postgres` Service:** Official `postgis/postgis:17-3.5-alpine` container running PostgreSQL 17 with PostGIS extensions. Data is persisted to named Docker volume `postgres_data`.
2. **`vetra-backend` Service:** Production-optimized multi-stage Docker container running Eclipse Temurin JRE 21 with non-root user security enforcement.

---

## 2. Prerequisites

- **Docker Engine:** `24.0+`
- **Docker Compose:** `v2.20+`

---

## 3. Quick Start (One-Command Startup)

To launch the complete Vetra backend platform and PostgreSQL database with a single command:

```bash
# 1. Clone environment variables template
cp .env.example .env

# 2. Build and start container services in detached mode
docker compose up -d --build
```

### Verification Endpoints

Once launched, verify application health and services:

- **Actuator Overall Health:** `http://localhost:8080/actuator/health`
- **Actuator Liveness Probe:** `http://localhost:8080/actuator/health/liveness`
- **Actuator Readiness Probe:** `http://localhost:8080/actuator/health/readiness`
- **Swagger OpenAPI UI:** `http://localhost:8080/swagger-ui.html`

---

## 4. Multi-Stage Dockerfile Specification

The build pipeline utilizes a 2-stage `Dockerfile`:

- **Stage 1 (Builder):** Uses `eclipse-temurin:21-jdk-alpine` to compile the Java 21 codebase and package an executable fat JAR with Maven.
- **Stage 2 (Runner):** Uses `eclipse-temurin:21-jre-alpine` for a minimal production runtime image.
  - User: Non-root user `vetra:vetra` (`uid=10001`).
  - JVM Flags: `-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom`.
  - Healthcheck: Periodically checks `http://localhost:8080/actuator/health/liveness`.

```bash
# Building production container manually
docker build -t vetra-backend:latest .
```

---

## 5. Environment Variables Configuration

The application loads environment variables dynamically from `.env`:

| Variable | Default Value | Description |
|---|---|---|
| `PORT` | `8080` | External HTTP container port mapping |
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring active profile (`prod` / `dev` / `test`) |
| `DB_HOST` | `postgres` | Database container hostname |
| `DB_PORT` | `5432` | Database container port |
| `DB_NAME` | `vetra_db` | Target PostgreSQL database name |
| `DB_USER` | `vetra_user` | Database master username |
| `DB_PASSWORD` | `vetra_password_secret` | Database master password |
| `JWT_SECRET` | *(Random 256-bit string)* | HMAC-SHA256 JWT signing secret key |
| `GEMINI_API_KEY` | *(Empty)* | Google Gemini Vision API Key |

---

## 6. Container Management & Useful Commands

### Viewing Real-Time Logs

```bash
# Stream logs for all containers
docker compose logs -f

# Stream logs for backend container only
docker compose logs -f vetra-backend
```

### Stopping Services

```bash
# Stop containers without destroying volume data
docker compose down

# Stop containers and erase database volumes
docker compose down -v
```

---

## 7. Troubleshooting & FAQ

### 1. Backend container fails to connect to database
- **Cause:** PostgreSQL container initialization is still in progress.
- **Fix:** `docker-compose.yml` implements `depends_on: postgres: condition: service_healthy` to delay backend startup until `pg_isready` succeeds.

### 2. Flyway migration error on startup
- **Cause:** Schema conflict or invalid Flyway version state.
- **Fix:** Reset local container volume using `docker compose down -v && docker compose up -d --build`.
