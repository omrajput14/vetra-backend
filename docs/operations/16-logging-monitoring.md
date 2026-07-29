# Logging & Monitoring Strategy
**Document ID:** OPS-16  
**Status:** Active (Logging) / Planned (Monitoring)  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Engineering Principles](../engineering/00-principles.md), [Security Design](../security/11-security-design.md)

---

## Overview

This document defines Vetra's logging standards, log format, log level strategy, and the monitoring architecture (currently planned, partially implemented).

---

## Logging Implementation

### Framework

| Component | Library |
|---|---|
| Logging API | SLF4J (`org.slf4j.Logger`) |
| Implementation | Logback (included with Spring Boot) |
| Configuration | `src/main/resources/logback-spring.xml` |

All logging must use the SLF4J API. Direct use of `System.out.println()` or `java.util.logging` is prohibited.

### Logger Declaration Pattern

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MedicalRecordService {
    private static final Logger log = LoggerFactory.getLogger(MedicalRecordService.class);

    public MedicalRecordResponse createRecord(CreateMedicalRecordRequest request) {
        log.info("Creating medical record for appointment={}", request.appointmentId());
        // ...
        log.debug("Medical record created: id={}", record.getId());
    }
}
```

---

## Log Levels

| Level | When to Use | Example |
|---|---|---|
| `ERROR` | Unrecoverable failure — requires immediate investigation. Always alerts in production. | Database connection lost, unhandled exception, security breach |
| `WARN` | Unexpected situation, recoverable, but indicates a potential problem | Deprecated endpoint called, retry attempt, slow query (>1s) |
| `INFO` | Normal business operations — key lifecycle events | User logged in, appointment created, medical record saved |
| `DEBUG` | Detailed operational information — disabled in production | SQL parameters, JWT claims, repository method entry/exit |
| `TRACE` | Extremely verbose — used only for deep debugging sessions | HTTP headers, full request/response bodies |

### Level Configuration by Environment

| Environment | Root Level | App Package Level |
|---|---|---|
| `local` / `dev` | `WARN` | `DEBUG` |
| `qa` | `WARN` | `INFO` |
| `staging` | `WARN` | `INFO` |
| `production` | `ERROR` | `INFO` |

---

## Log Format

### Development (Human-Readable)

```
2026-07-29 12:00:00.123 INFO  [main] a.v.auth.service.AuthService - User logged in: userId=f47ac10b email=farmer@example.com
```

### Production (Structured JSON)

```json
{
  "timestamp": "2026-07-29T12:00:00.123Z",
  "level": "INFO",
  "logger": "app.vetra.auth.service.AuthService",
  "thread": "http-nio-8080-exec-1",
  "traceId": "abc123def456",
  "message": "User logged in",
  "userId": "f47ac10b-...",
  "email": "farmer@example.com"
}
```

Structured JSON logging is configured via Logback JSON encoder in production profile. This enables log aggregation systems (CloudWatch Logs, Elasticsearch, Loki) to parse and query logs efficiently.

---

## What Must Be Logged

### ✅ Always Log

| Event | Level | Fields to Include |
|---|---|---|
| Successful authentication | INFO | `userId`, `email`, `role` |
| Failed authentication | WARN | `email` (not password), `reason` |
| Authorization failure | WARN | `userId`, `resource`, `action` |
| Resource creation | INFO | `resourceType`, `resourceId`, `userId` |
| State transitions (appointments) | INFO | `appointmentId`, `fromStatus`, `toStatus`, `userId` |
| Unhandled exception | ERROR | Full stack trace, `requestPath`, `userId` (if available) |
| Slow database query (>500ms) | WARN | Query time, table name |
| Application startup | INFO | Spring profile, port, database URL (masked) |
| Database migration applied | INFO | Flyway version, description |

### ❌ Never Log

| Data | Reason |
|---|---|
| Passwords (plaintext or hash) | Security |
| JWT tokens (access or refresh) | Security |
| Full credit card / payment data | Compliance (PCI-DSS) |
| Full database connection strings with credentials | Security |
| Personal health information beyond what is necessary | Privacy |
| Private keys or API secrets | Security |

---

## Request Tracing

A `traceId` is assigned to each incoming HTTP request and propagated through all log statements within that request's thread.

**Implementation:** Spring Boot `Slf4jMdcAdapter` + MDC (Mapped Diagnostic Context)

```java
// In JwtAuthFilter, after successful authentication:
MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8));
MDC.put("userId", claims.getSubject());
```

This ensures all log lines for a single request share the same `traceId`, enabling request tracing in log aggregation systems.

---

## PLANNED: Monitoring Architecture

### Metrics Collection — Spring Boot Actuator + Micrometer

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Key Metrics to Track:**

| Metric | Type | Alert Threshold |
|---|---|---|
| `http_server_requests_seconds` | Histogram | p99 > 2s |
| `http_server_requests_errors_total` | Counter | Error rate > 5% over 5 min |
| `jvm_memory_used_bytes` | Gauge | > 80% of heap |
| `hikaricp_connections_active` | Gauge | > 90% of pool size |
| `db_query_duration_seconds` | Histogram | p99 > 1s |
| `appointments_created_total` | Counter | — (business metric) |
| `medical_records_created_total` | Counter | — (business metric) |

### Dashboard Stack (Planned)

```
Spring Boot (Micrometer) → Prometheus (scrape every 15s) → Grafana (dashboards + alerts)
```

### Alert Rules (Planned)

| Alert | Condition | Severity | Action |
|---|---|---|---|
| High error rate | HTTP 5xx > 5% for 5 min | Critical | Page on-call |
| Slow responses | p99 latency > 3s for 5 min | Warning | Investigate |
| High memory | JVM heap > 85% for 10 min | Warning | Check for memory leak |
| DB connection exhaustion | HikariCP pool > 90% for 5 min | Critical | Scale up |
| Application down | Health check fails 3× | Critical | Page on-call, auto-restart |

---

## Log Retention

| Environment | Retention |
|---|---|
| Local | Not retained (terminal output only) |
| Dev | 7 days |
| Staging | 30 days |
| Production | 90 days (regulatory minimum) |
