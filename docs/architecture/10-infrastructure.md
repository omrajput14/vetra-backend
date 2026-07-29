# Infrastructure Diagram
**Document ID:** ARCH-10  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Deployment Architecture](./09-deployment.md), [Security Design](../security/11-security-design.md)

---

## Current Infrastructure (Local Development)

```mermaid
graph TB
    subgraph "Developer Machine"
        Flutter["Flutter App<br/>(Android Emulator)"]
        SpringBoot["Spring Boot<br/>:8080<br/>Profile: dev"]
        subgraph Docker["Docker Desktop"]
            PG["PostgreSQL 15<br/>:5432<br/>vetra_db"]
            pgAdmin["pgAdmin 4<br/>:5050<br/>(optional)"]
        end
    end

    Flutter -- "HTTP<br/>10.0.2.2:8080" --> SpringBoot
    SpringBoot -- "JDBC<br/>localhost:5432" --> PG
    pgAdmin -- "Query" --> PG
```

---

## Planned Production Infrastructure (AWS)

```mermaid
graph TB
    subgraph "Users"
        FarmerApp["Farmer<br/>Android/iOS App"]
        VetApp["Vet<br/>Android/iOS App"]
    end

    subgraph "DNS & CDN"
        R53["AWS Route 53<br/>api.vetra.app"]
        CF["CloudFront<br/>(Static assets future)"]
    end

    subgraph "Load Balancing"
        ALB["Application Load Balancer<br/>HTTPS :443<br/>SSL Termination"]
    end

    subgraph "Application Tier — ECS Fargate"
        App1["Spring Boot<br/>Instance 1<br/>:8080"]
        App2["Spring Boot<br/>Instance 2<br/>:8080"]
    end

    subgraph "Data Tier — RDS"
        PGPrimary["PostgreSQL 15<br/>Primary (Read/Write)<br/>db.t3.small"]
        PGReplica["PostgreSQL 15<br/>Read Replica (future)<br/>db.t3.small"]
    end

    subgraph "Secrets & Config"
        SecretsManager["AWS Secrets Manager<br/>(DB creds, JWT secret)"]
        ParameterStore["SSM Parameter Store<br/>(App config)"]
    end

    subgraph "Observability (Planned)"
        CW["CloudWatch<br/>Logs & Metrics"]
        Grafana["Grafana<br/>Dashboards"]
    end

    FarmerApp -- "HTTPS" --> R53
    VetApp -- "HTTPS" --> R53
    R53 --> ALB
    ALB -- "Health Check<br/>/actuator/health" --> App1
    ALB -- "Health Check<br/>/actuator/health" --> App2
    App1 -- "JDBC SSL" --> PGPrimary
    App2 -- "JDBC SSL" --> PGPrimary
    PGPrimary -.->|replication| PGReplica
    App1 -- "Fetch secrets at startup" --> SecretsManager
    App2 -- "Fetch secrets at startup" --> SecretsManager
    App1 -- "Logs" --> CW
    App2 -- "Logs" --> CW
    CW --> Grafana
```

---

## Network Security Groups (Planned)

| Component | Inbound | Outbound |
|---|---|---|
| ALB | 443 from 0.0.0.0/0 | 8080 to App tier |
| App tier (ECS) | 8080 from ALB only | 5432 to DB tier, 443 to AWS services |
| DB tier (RDS) | 5432 from App tier only | None |

**Principle:** Minimum necessary network access. The database is never directly reachable from the internet.

---

## Data Flow: Farmer Books Appointment

```mermaid
sequenceDiagram
    participant App as Flutter App
    participant ALB as Load Balancer
    participant API as Spring Boot
    participant DB as PostgreSQL

    App->>ALB: POST /api/v1/appointments<br/>Authorization: Bearer <JWT>
    ALB->>API: Forward request
    API->>API: Validate JWT (JwtAuthFilter)
    API->>API: Extract farmer identity from JWT
    API->>DB: INSERT INTO appointments<br/>(farmer_id, vet_id, animal_id, ...)
    DB-->>API: Return created appointment (UUID)
    API-->>ALB: 201 Created + AppointmentResponse JSON
    ALB-->>App: 201 Created
    App->>App: Show confirmation screen
```

---

## Port Reference

| Component | Port | Protocol | Environment |
|---|---|---|---|
| Spring Boot (local) | 8080 | HTTP | Local only |
| PostgreSQL (Docker) | 5432 | TCP | Local only |
| pgAdmin (Docker) | 5050 | HTTP | Local only |
| ALB (production) | 443 | HTTPS | Staging/Production |
| RDS (production) | 5432 | TCP/SSL | App tier only |
