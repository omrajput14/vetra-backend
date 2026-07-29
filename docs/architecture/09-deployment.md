# Deployment Architecture
**Document ID:** ARCH-09  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Infrastructure Diagram](./10-infrastructure.md), [Environment Configuration](../operations/24-environment-config.md), [Engineering Principles](../engineering/00-principles.md)

---

## Current Deployment: Local Development

> [!NOTE]
> Vetra currently runs in a local development environment only. Cloud deployment infrastructure is planned. All production, staging, and CI/CD architecture described in this document represents the target design — not yet implemented. Target sections are labeled **PLANNED**.

---

## Local Development Topology

The local development environment uses **Docker Compose** for the database layer and the host JVM for the Spring Boot application.

```
Developer Machine
│
├── Docker Desktop
│   └── docker-compose.dev.yml
│       ├── postgres:15          ← Port 5432
│       │   Database: vetra_db
│       │   User:     vetra_user
│       │
│       └── pgAdmin 4            ← Port 5050 (optional, for DB inspection)
│
└── Host JVM (Spring Boot)       ← Port 8080
    ./mvnw spring-boot:run
    Profile: dev
    Connects to: localhost:5432
```

### Starting the Local Environment

```bash
# 1. Start PostgreSQL via Docker Compose
docker compose -f docker-compose.dev.yml up -d

# 2. Verify database is running
docker compose -f docker-compose.dev.yml ps

# 3. Start the Spring Boot application
./mvnw spring-boot:run

# 4. Verify API is responding
curl http://localhost:8080/actuator/health
```

### Flutter Connectivity from Android Emulator

The Android emulator runs in a virtual network. It accesses the host machine via the special IP `10.0.2.2`:

```
Android Emulator → http://10.0.2.2:8080 → Spring Boot (host:8080)
```

For physical devices, use the host machine's local network IP:

```bash
# Find your local IP
ifconfig | grep "inet " | grep -v 127.0.0.1
# Then use: http://<your-local-ip>:8080
```

---

## Docker Image Build

The application is containerised via `Dockerfile` for deployment to any container runtime.

### Dockerfile Strategy: Multi-Stage Build

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why multi-stage:** The final image contains only the JRE and the compiled JAR — no Maven, no source code, no build tools. Smaller image size (~200 MB vs. ~700 MB).

---

## PLANNED: Production Deployment Architecture

### Cloud Provider

Target: **AWS** (primary) or **GCP** (alternative). Provider decision deferred to production launch.

### Topology

```
Internet
    │
    ▼
AWS Route 53 (DNS)
    │
    ▼
CloudFront / ALB (HTTPS termination, SSL)
    │
    ▼
ECS Fargate / EC2 (Spring Boot containers)
    │           │
    │           ├── Instance 1 (app.jar)
    │           └── Instance 2 (app.jar)  ← Horizontal scaling
    │
    ▼
RDS PostgreSQL 15 (Multi-AZ, managed)
    │
    ├── Primary (writes)
    └── Read Replica (reads — future)
```

### Resource Sizing (Initial Production — Planned)

| Component | Specification | Notes |
|---|---|---|
| Application | 2 vCPU, 2 GB RAM per instance | Start with 1 instance, scale to 2 on launch |
| Database | RDS db.t3.small (2 vCPU, 2 GB RAM) | Upgrade to db.t3.medium at 500+ daily active users |
| Load Balancer | AWS ALB | Health check: `GET /actuator/health` → 200 |
| Storage | RDS 20 GB gp3 SSD | Auto-scaling enabled |

### Environment URL Structure

| Environment | URL |
|---|---|
| Development | `http://dev.vetra.app` |
| Staging | `https://staging.vetra.app` |
| Production | `https://api.vetra.app` |

---

## PLANNED: Health Checks and Readiness

Spring Boot Actuator provides health endpoints:

```
GET /actuator/health     → {"status": "UP"} — for load balancer health checks
GET /actuator/info       → Application metadata
GET /actuator/metrics    → Prometheus-compatible metrics (future)
```

Production health check configuration:
- **Check interval:** 30 seconds
- **Healthy threshold:** 2 consecutive successes
- **Unhealthy threshold:** 3 consecutive failures
- **Timeout:** 5 seconds

---

## PLANNED: Zero-Downtime Deployment

Rolling deployment strategy:

1. Build new Docker image with version tag (e.g., `vetra-backend:v1.1.0`)
2. Deploy to 1 of N instances (rolling update)
3. Wait for health check to pass
4. Drain connections from old instance
5. Replace remaining instances

Database migrations (Flyway) run at application startup. Migrations must be backward-compatible with the previous application version to support the rolling deployment window.

---

## Rollback Strategy

| Scenario | Rollback Method |
|---|---|
| Application bug | Re-deploy previous Docker image tag |
| Database migration issue | Forward-only migration fix (no rollback migration) |
| Data corruption | Restore from RDS automated backup |
| Security breach | Rotate all secrets, redeploy with new secrets |
