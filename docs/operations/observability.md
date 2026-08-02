# Vetra Platform — Enterprise Observability & Monitoring Specification

**Stage:** 12.4.1 — Production Observability Foundation  
**Role:** Principal SRE & Platform Engineer  
**Date:** August 2026  
**Status:** PRODUCTION READY  

---

## 1. Architecture Overview

The Vetra Platform Observability architecture follows standard cloud-native SRE patterns using **Micrometer** metric collection, **Spring Boot Actuator** metric publishing, and **Prometheus** pull-based metric scraping.

```
┌─────────────────────────┐          ┌───────────────────────────┐
│   Vetra Backend         │          │   Prometheus Scraper      │
│  (Spring Boot 3.5.3)    │          │  (prom/prometheus:v3.2)   │
│                         │          │                           │
│  Micrometer Registry    │          │  Scrape Interval: 5s      │
│     │                   │          │  Target: vetra-backend:8080│
│     ▼                   │          │                           │
│  /actuator/prometheus ──┼─[HTTP]──►│  TSDB Time Series Store   │
└─────────────────────────┘          └───────────────────────────┘
```

---

## 2. Metric Endpoints & Exposure

| Endpoint | Security | Format | Purpose |
|---|---|---|---|
| `/actuator/health` | Public (`PUBLIC_ENDPOINTS`) | JSON | Liveness & Readiness probes |
| `/actuator/info` | Public (`PUBLIC_ENDPOINTS`) | JSON | Application build & profile version |
| `/actuator/metrics` | Authenticated | JSON | Micrometer metric registry index |
| `/actuator/prometheus` | Public (`PUBLIC_ENDPOINTS`) | OpenMetrics / Plaintext | Prometheus exposition format |

---

## 3. Micrometer Core Metrics Catalogue

### 3.1 JVM & Process Telemetry

- `jvm_memory_used_bytes` / `jvm_memory_max_bytes`: Heap and non-heap memory utilization.
- `jvm_gc_pause_seconds`: Garbage Collection pause duration histograms.
- `jvm_threads_live_threads` / `jvm_threads_peak_threads`: Thread pool dynamics.
- `system_cpu_usage` / `process_cpu_usage`: CPU utilization ratios.
- `process_uptime_seconds`: Application uptime duration.

### 3.2 HTTP Server Request Metrics

- `http_server_requests_seconds_count`: Total HTTP request count.
- `http_server_requests_seconds_sum`: Accumulated response duration.
- `http_server_requests_seconds_bucket`: Latency distribution SLA histograms (`50ms`, `100ms`, `200ms`, `500ms`, `1s`).

### 3.3 Database & HikariCP Metrics

- `hikaricp_connections_active`: Active PostgreSQL database connections.
- `hikaricp_connections_idle`: Idle database connections in Hikari pool.
- `hikaricp_connections_timeout_total`: Connection acquisition timeouts.

### 3.4 Spring Cache & Redis Metrics

- `cache_gets_total`: Cache lookup requests tagged by `cache` region name and `result` (`hit` / `miss`).
- `cache_puts_total`: Cache write operations per region.
- `cache_evictions_total`: Cache evictions per region.

---

## 4. Prometheus Configuration

Configured in [`docker/prometheus/prometheus.yml`](file:///Users/0mrajput/vetra-backend/docker/prometheus/prometheus.yml):

```yaml
global:
  scrape_interval: 5s
  evaluation_interval: 5s

scrape_configs:
  - job_name: 'vetra-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['vetra-backend:8080']
```

---

## 5. Operational Verification Procedures

### 5.1 Local Verification of `/actuator/prometheus`

```bash
curl -s http://localhost:8080/actuator/prometheus | grep -E "jvm_memory_used_bytes|http_server_requests_seconds|cache_gets"
```

### 5.2 Prometheus Container Health Check

```bash
docker compose exec prometheus wget -qO- http://localhost:9090/-/healthy
```

---

## 6. Next Observability Milestones

- **Stage 12.4.2:** Grafana Dashboard Provisioning & Custom Business Metrics (Counters for animal registrations, appointment state transitions, EVMR creations, AI diagnoses).
- **Stage 12.4.3:** Alertmanager Rules, SLO/SLA Definitions, and Operational Incident Runbooks.
