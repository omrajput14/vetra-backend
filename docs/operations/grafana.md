# Vetra Platform — Grafana Observability Operations Manual

**Stage:** 12.4.2 — Enterprise Grafana Dashboards & Business Metrics
**Role:** Principal Observability Engineer & Platform SRE
**Status:** PRODUCTION READY
**Last Updated:** August 2026

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Vetra Observability Stack                         │
│                                                                          │
│   Vetra Backend (Spring Boot 3.5.3)                                      │
│   ├── Micrometer Registry                                                │
│   │   ├── JVM metrics          (jvm_memory_*, jvm_gc_*, jvm_threads_*)   │
│   │   ├── HTTP metrics         (http_server_requests_seconds_*)           │
│   │   ├── HikariCP metrics     (hikaricp_connections_*)                   │
│   │   ├── Spring Cache metrics (cache_gets_total, cache_puts_total)       │
│   │   └── Business metrics     (vetra.animal.*, vetra.auth.*, ...)        │
│   └── /actuator/prometheus ─────────────────────────────────────────┐    │
│                                                                      │    │
│   Prometheus (prom/prometheus:v3.2.1)                                │    │
│   ├── Scrape interval: 5s                                            │    │
│   ├── Target: vetra-backend:8080/actuator/prometheus ◄───────────────┘    │
│   ├── Retention: 15 days                                                  │
│   └── TSDB /prometheus ──────────────────────────────────────────────┐    │
│                                                                       │    │
│   Grafana (grafana/grafana-oss:11.4.0)                                │    │
│   ├── Datasource: Prometheus ◄─────────────────────────────────────────┘   │
│   ├── Auto-provisioned dashboards (5 dashboards)                           │
│   └── http://localhost:3000                                                │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Service Ports

| Service | Host Port | Container Port | Default |
|---|---|---|---|
| Vetra Backend | `$PORT` | 8080 | `8080` |
| Prometheus | `$PROMETHEUS_PORT` | 9090 | `9090` |
| Grafana | `$GRAFANA_PORT` | 3000 | `3000` |

---

## 3. Access URLs

| Interface | URL |
|---|---|
| Grafana UI | http://localhost:3000 |
| Prometheus UI | http://localhost:9090 |
| Prometheus Targets | http://localhost:9090/targets |
| Actuator Prometheus Metrics | http://localhost:8080/actuator/prometheus |
| Actuator Health | http://localhost:8080/actuator/health |

---

## 4. Login Credentials

| Service | Username | Password | Override |
|---|---|---|---|
| Grafana | `admin` | `admin` | `GRAFANA_ADMIN_PASSWORD` in `.env` |

> ⚠️ **Production:** Override `GRAFANA_ADMIN_PASSWORD` in `.env` or environment before deploying to any non-local environment.

---

## 5. Provisioning Flow

Everything is provisioned automatically. After `docker compose up -d`, the following happens without any manual steps:

```
docker compose up -d
       │
       ├── postgres (healthy) ──────────────────────────────────────┐
       ├── redis (healthy) ─────────────────────────────────────────┤
       │                                                            ▼
       ├── vetra-backend (healthy) ◄── depends_on postgres, redis
       │                    │
       │                    └── exposes /actuator/prometheus
       │                                    │
       ├── prometheus (healthy) ◄───────────┘   scrapes every 5s
       │         │
       └── grafana (healthy) ◄─── depends_on prometheus
                 │
                 ├── mounts provisioning/datasources/prometheus.yml
                 │      └── registers Prometheus datasource (uid: vetra-prometheus)
                 │
                 ├── mounts provisioning/dashboards/dashboards.yml
                 │      └── dashboard provider scans /var/lib/grafana/dashboards
                 │
                 └── mounts docker/grafana/dashboards/*.json
                        └── imports 5 dashboards automatically
```

---

## 6. Provisioning Directory Structure

```
docker/
└── grafana/
    ├── provisioning/
    │   ├── datasources/
    │   │   └── prometheus.yml          # Prometheus datasource (auto-registered)
    │   └── dashboards/
    │       └── dashboards.yml          # Dashboard provider config
    └── dashboards/
        ├── spring-overview.json        # Dashboard 1: HTTP metrics
        ├── jvm.json                    # Dashboard 2: JVM internals
        ├── postgres.json               # Dashboard 3: HikariCP/PostgreSQL
        ├── cache.json                  # Dashboard 4: Spring Cache layer
        └── business.json              # Dashboard 5: Business KPIs
```

---

## 7. Dashboard Catalogue

| # | Dashboard | UID | Tags | Purpose |
|---|---|---|---|---|
| 1 | Spring Boot Overview | `vetra-spring-overview` | spring-boot, http | HTTP request rate, response time p50/p95/p99, error rate, uptime |
| 2 | JVM Internals | `vetra-jvm` | jvm, memory, gc | Heap/non-heap, GC pauses, CPU, thread states, class loading |
| 3 | PostgreSQL & HikariCP | `vetra-postgres` | postgresql, hikaricp | Connection pool active/idle/pending, timeouts, acquisition latency |
| 4 | Cache Layer | `vetra-cache` | cache, spring-cache | Per-region hit ratio, hits, misses, puts, evictions |
| 5 | Business Metrics | `vetra-business` | business, kpi | Animal registrations, appointments, AI diagnoses, notifications, logins |

---

## 8. Metric Catalogue

### 8.1 Infrastructure Metrics (Auto-Collected by Micrometer)

| Metric | Labels | Description |
|---|---|---|
| `http_server_requests_seconds_*` | `uri`, `method`, `status` | HTTP request latency histogram |
| `jvm_memory_used_bytes` | `area`, `id` | JVM memory usage (heap + non-heap) |
| `jvm_memory_max_bytes` | `area`, `id` | JVM memory max |
| `jvm_memory_committed_bytes` | `area`, `id` | JVM memory committed |
| `jvm_gc_pause_seconds_*` | `action`, `cause` | GC pause duration |
| `jvm_threads_live_threads` | — | Current live thread count |
| `jvm_threads_states_threads` | `state` | Thread count by state |
| `jvm_classes_loaded_classes` | — | Currently loaded class count |
| `process_cpu_usage` | — | Process CPU (0.0 – 1.0) |
| `system_cpu_usage` | — | System CPU (0.0 – 1.0) |
| `process_uptime_seconds` | — | Process uptime |
| `hikaricp_connections_active` | `pool` | Active DB connections |
| `hikaricp_connections_idle` | `pool` | Idle DB connections |
| `hikaricp_connections_pending` | `pool` | Pending connection requests |
| `hikaricp_connections_timeout_total` | `pool` | Connection timeout counter |
| `hikaricp_connection_acquired_nanos_*` | `pool` | Connection acquisition duration |
| `cache_gets_total` | `name`, `result` (hit/miss) | Cache lookup outcomes |
| `cache_puts_total` | `name` | Cache put operations |
| `cache_evictions_total` | `name` | Cache evictions |

### 8.2 Business Metrics (VetraMetrics Component)

| Metric | Labels | Source |
|---|---|---|
| `vetra_user_registrations_total` | `role` (FARMER, VETERINARIAN) | `AuthService.registerFarmer/Vet()` |
| `vetra_auth_login_total` | `role`, `result` (success, failure) | `AuthService.loginFarmer/Vet()` |
| `vetra_animal_registrations_total` | — | `AnimalService.createAnimal()` |
| `vetra_appointments_created_total` | — | `AppointmentService.createAppointment()` |
| `vetra_appointments_status_total` | `status` (CONFIRMED, COMPLETED, CANCELLED, REJECTED) | `AppointmentService.applyStateTransition()` |
| `vetra_ai_diagnosis_requests_total` | — | `AIScanService.createScan()` |
| `vetra_notifications_dispatched_total` | `result` (success, failure, queued) | `NotificationService.sendNotification()` |
| `vetra_appointments_create_duration_*` | — | Timer: appointment creation latency |
| `vetra_animal_create_duration_*` | — | Timer: animal registration latency |

> **Low-Cardinality Policy:** No UUIDs, user IDs, email addresses, or entity IDs are used as metric labels. All tag values are enumerable constants.

---

## 9. Operational Verification Procedures

### 9.1 Full Stack Startup

```bash
# Start all services
docker compose up -d

# Wait for all services to reach healthy state (~60s)
docker compose ps

# Expected output:
# vetra-postgres    healthy
# vetra-redis       healthy
# vetra-backend     healthy
# vetra-prometheus  healthy
# vetra-grafana     healthy
```

### 9.2 Verify Prometheus Datasource

```bash
# Grafana API — datasource health check
curl -s -u admin:admin http://localhost:3000/api/datasources | python3 -m json.tool

# Prometheus target health
curl -s http://localhost:9090/api/v1/targets | python3 -m json.tool | grep -A5 '"health"'
```

### 9.3 Verify Dashboard Provisioning

```bash
# List all provisioned dashboards
curl -s -u admin:admin "http://localhost:3000/api/search?type=dash-db" | python3 -m json.tool

# Expected: 5 dashboards returned
```

### 9.4 Verify Metrics Endpoint

```bash
# Confirm Prometheus metrics are being exposed
curl -s http://localhost:8080/actuator/prometheus | grep -E "^(jvm_memory|http_server|cache_gets|hikaricp|vetra_)"
```

### 9.5 Smoke-Test Business Metrics

```bash
# Register a farmer (increments vetra_user_registrations_total)
curl -s -X POST http://localhost:8080/api/v1/auth/farmer/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@smoke.com","password":"Test1234!","fullName":"Smoke Test","phone":"9999999999"}'

# Verify metric appeared
curl -s http://localhost:8080/actuator/prometheus | grep vetra_user_registrations_total
```

---

## 10. Troubleshooting

### Grafana does not start

```bash
docker compose logs grafana --tail=50
```

Common causes:
- Port 3000 already in use — set `GRAFANA_PORT=3001` in `.env`
- Provisioning YAML syntax error — check `docker/grafana/provisioning/`

### Prometheus datasource shows "Error" in Grafana

```bash
# Check Prometheus is healthy
curl http://localhost:9090/-/healthy

# Check Prometheus can reach the backend
docker compose exec prometheus wget -qO- http://vetra-backend:8080/actuator/prometheus | head -5
```

### Dashboards show "No data"

1. Confirm Prometheus scrape target is UP: http://localhost:9090/targets
2. Check time range in Grafana (some business metrics only appear after exercising the API)
3. Confirm backend is healthy: `curl http://localhost:8080/actuator/health`

### Dashboard fails to import (provisioning)

```bash
# View provisioning errors
docker compose exec grafana cat /var/log/grafana/grafana.log | grep -i "error\|provision"
```

### Business metrics are all zero

Business metrics (`vetra_*`) require actual API usage to increment. They start at 0 and accumulate as the application receives real traffic. Run the smoke test from section 9.5.

---

## 11. Adding New Dashboards

1. Create a JSON file in `docker/grafana/dashboards/`:
   ```bash
   touch docker/grafana/dashboards/my-dashboard.json
   ```

2. Set `"uid"` to a unique string (e.g., `"vetra-my-dashboard"`).

3. Set the datasource UID to `"vetra-prometheus"` in all panel targets.

4. Restart Grafana to pick up the new dashboard:
   ```bash
   docker compose restart grafana
   ```

5. Commit the JSON file to version control.

> **Policy:** Dashboard JSON files are the source of truth. Do not edit dashboards in the UI without exporting and committing the JSON.

---

## 12. Adding New Business Metrics

All new business metrics **must** be added to `VetraMetrics.java`:

```java
// Example: adding a disease report metric
private final Counter diseaseReports;

// In constructor:
diseaseReports = buildCounter(registry, "vetra.disease.reports",
    "Total disease reports submitted");

// Public method:
public void recordDiseaseReport() {
    diseaseReports.increment();
}
```

Rules:
- Only `Counter`, `Timer`, or `Gauge` — no raw `MeterRegistry` calls in service classes
- No high-cardinality tags (no UUIDs, emails, IDs)
- Name must follow convention: `vetra.<domain>.<event>`
- Update `docs/operations/grafana.md` metric catalogue

---

## 13. Dashboard Ownership

| Dashboard | Owner | Review Cadence |
|---|---|---|
| Spring Boot Overview | Backend Team | Per Sprint |
| JVM Internals | Infrastructure / SRE | Quarterly |
| PostgreSQL & HikariCP | Backend / DBA | Quarterly |
| Cache Layer | Backend Team | Per Sprint |
| Business Metrics | Product + Backend | Per Sprint |

---

## 14. Security Notes

- Grafana is network-isolated within `vetra-network` — only Prometheus (internal) is a datasource
- Prometheus scrapes only `vetra-backend:8080` (internal Docker network — not internet-accessible)
- `/actuator/prometheus` is publicly accessible within the cluster — it does not expose secrets
- Grafana admin credentials should be rotated before any non-local deployment using `GRAFANA_ADMIN_PASSWORD` in `.env`
- Anonymous access is disabled (`GF_USERS_ALLOW_SIGN_UP=false`)

---

## 15. Future Observability Milestones

- **Stage 12.4.3:** Alertmanager — SLO/SLA alert rules, PagerDuty/Slack integration
- **Stage 12.4.4:** redis_exporter sidecar — Redis server-level metrics (memory, keyspace, latency, connected clients)
- **Stage 12.4.5:** Loki log aggregation + Grafana log panels
- **Stage 12.4.6:** Tempo distributed tracing (OpenTelemetry)
