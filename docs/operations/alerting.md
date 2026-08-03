# Vetra Platform — Enterprise Alerting Operations Manual

**Stage:** 12.4.3 — Enterprise Alerting & SLO Monitoring
**Role:** Principal SRE & Observability Architect
**Status:** PRODUCTION READY
**Last Updated:** August 2026

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Vetra Alerting & Monitoring Stack                    │
│                                                                          │
│   Vetra Backend (:8080)                                                 │
│   └── /actuator/prometheus                                              │
│            │                                                            │
│            │ (Scrape interval: 5s)                                      │
│            ▼                                                            │
│   Prometheus (:9090)                                                    │
│   ├── Recording Rules (00-recording-rules.yml)                          │
│   │   ├── vetra:http_request_success_rate:5m                             │
│   │   ├── vetra:jvm_heap_usage:instant                                  │
│   │   └── vetra:db_connection_usage:instant                             │
│   │                                                                     │
│   ├── Alert Rules (10-70 alert rule files)                              │
│   │   ├── Infrastructure, JVM, HTTP, Cache, DB, Business, SLO           │
│   │   └── Watchdog Canary (Always Firing)                               │
│   │                                                                     │
│   └── Alerting Engine ───────────────────────────────────────────┐      │
│                                                                  │      │
│   Alertmanager (:9093) ◄─────────────────────────────────────────┘      │
│   ├── Grouping: (alertname, service, component)                         │
│   ├── Inhibition: Suppress dependent alerts when Backend is down         │
│   ├── Routing:                                                          │
│   │   ├── Watchdog  → watchdog-receiver                                 │
│   │   ├── Critical  → critical-receiver                                 │
│   │   └── Warning   → warning-receiver                                  │
│   └── Receivers:                                                        │
│       ├── Dev: dev-null (drops silently after pipeline evaluation)      │
│       └── Prod: Slack / PagerDuty / Email Webhooks                      │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Service Ports & Endpoints

| Service | Host Port | Container Port | Health Check Endpoint |
|---|---|---|---|
| Vetra Backend | 8080 | 8080 | `/actuator/health` |
| Prometheus | 9090 | 9090 | `/-/healthy` |
| Grafana | 3000 | 3000 | `/api/health` |
| Alertmanager | 9093 | 9093 | `/api/v2/status` |

---

## 3. Recording Rules Catalog

Recording rules pre-compute expensive PromQL expressions every 5 seconds. All alert rules and Grafana panels consume these recording rules.

| Metric | Expression | Description |
|---|---|---|
| `vetra:backend_up:instant` | `up{job="vetra-backend"}` | Canonical backend uptime signal |
| `vetra:http_5xx_rate:5m` | `sum(rate(http_server_requests_seconds_count{outcome="SERVER_ERROR"}[5m]))` | 5xx HTTP error rate |
| `vetra:http_total_rate:5m` | `sum(rate(http_server_requests_seconds_count}[5m]))` | Total HTTP throughput rate |
| `vetra:http_request_success_rate:5m` | `1 - (vetra:http_5xx_rate:5m / vetra:http_total_rate:5m)` | Overall HTTP success ratio (0-1) |
| `vetra:http_latency_p95:5m` | `histogram_quantile(0.95, sum by (le) (rate(..._bucket[5m])))` | 95th percentile HTTP response latency |
| `vetra:jvm_heap_usage:instant` | `sum(jvm_memory_used_bytes{area="heap"}) / sum(jvm_memory_max_bytes{area="heap"} > 0)` | JVM Old Gen Heap occupancy ratio (0-1) |
| `vetra:jvm_gc_pause_avg_seconds:5m` | `rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])` | Average GC pause duration |
| `vetra:db_connection_usage:instant` | `hikaricp_connections_active / hikaricp_connections_max` | HikariCP pool occupancy ratio (0-1) |
| `vetra:db_connection_timeout_rate:5m` | `rate(hikaricp_connections_timeout_total[5m])` | HikariCP pool timeout rate |
| `vetra:cache_hit_ratio:5m` | `hits / (hits + misses)` across all regions | Overall Spring Cache hit ratio (0-1) |
| `vetra:login_failure_rate:5m` | `sum(rate(vetra_auth_login_total{result="failure"}[5m]))` | Combined auth login failure rate |

---

## 4. Alert Rules Catalog

| Rule Name | Severity | Component | For | Trigger Condition | Runbook |
|---|---|---|---|---|---|
| **Watchdog** | INFO | infrastructure | 0s | `vector(1)` (always firing) | `BackendDown.md` |
| **VetraBackendDown** | CRITICAL | infrastructure | 1m | `up{job="vetra-backend"} == 0` | `BackendDown.md` |
| **VetraPrometheusDown** | CRITICAL | infrastructure | 30s | `up{job="prometheus"} == 0` | `BackendDown.md` |
| **VetraJvmHeapCritical** | CRITICAL | jvm | 5m | `vetra:jvm_heap_usage:instant > 0.90` | `HighHeapUsage.md` |
| **VetraJvmHeapWarning** | WARNING | jvm | 10m | `vetra:jvm_heap_usage:instant > 0.80` | `HighHeapUsage.md` |
| **VetraJvmGcPauseHigh** | WARNING | jvm | 5m | `vetra:jvm_gc_pause_avg_seconds:5m > 0.5` | `HighHeapUsage.md` |
| **VetraJvmThreadExplosion** | WARNING | jvm | 5m | `jvm_threads_live_threads > 250` | `HighLatency.md` |
| **VetraHighErrorRateCritical** | CRITICAL | http | 5m | `vetra:http_request_success_rate:5m < 0.95` | `HighLatency.md` |
| **VetraHighErrorRateWarning** | WARNING | http | 5m | `vetra:http_request_success_rate:5m < 0.99` | `HighLatency.md` |
| **VetraHighLatencyWarning** | WARNING | http | 5m | `vetra:http_latency_p95:5m > 1.0` | `HighLatency.md` |
| **VetraHighLatencyCritical** | CRITICAL | http | 2m | `vetra:http_latency_p95:5m > 3.0` | `HighLatency.md` |
| **VetraCacheHitRatioLow** | WARNING | cache | 10m | `vetra:cache_hit_ratio:5m < 0.70` | `DatabaseConnectionsExhausted.md` |
| **VetraDbConnectionPoolExhausted** | CRITICAL | database | 2m | `hikaricp_connections_pending > 5` | `DatabaseConnectionsExhausted.md` |
| **VetraDbConnectionTimeout** | CRITICAL | database | 1m | `vetra:db_connection_timeout_rate:5m > 0` | `DatabaseConnectionsExhausted.md` |
| **VetraDbHighConnectionUsage** | WARNING | database | 5m | `vetra:db_connection_usage:instant > 0.80` | `DatabaseConnectionsExhausted.md` |
| **VetraHighLoginFailureRate** | WARNING | business | 5m | `vetra:login_failure_rate:5m > 0.2` | `HighLoginFailureRate.md` |
| **VetraSloBurnRateCritical** | CRITICAL | slo | 5m | `vetra:http_request_success_rate:5m < 0.99` | `HighLatency.md` |
| **VetraSloBurnRateWarning** | WARNING | slo | 15m | `vetra:http_request_success_rate:5m < 0.995` | `HighLatency.md` |

---

## 5. Inhibition Rules

To prevent alert storms, Alertmanager evaluates inhibition rules before dispatching notifications:

1. **Root Cause Suppress:** If `VetraBackendDown` is firing, all other warning/critical alerts for `service="vetra-backend"` are inhibited.
2. **Severity Suppress:** If a `critical` alert is firing for a component, `warning` alerts for the same component are inhibited.

---

## 6. Alert Silencing & Maintenance Mode

During planned maintenance or deployment windows, suppress alerts via Alertmanager API or UI:

```bash
# Silence all warning/critical alerts for 1 hour
curl -X POST http://localhost:9093/api/v2/silences \
  -H "Content-Type: application/json" \
  -d '{
    "matchers": [
      {"name": "service", "value": "vetra-backend", "isRegex": false}
    ],
    "startsAt": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
    "endsAt": "'$(date -u -d "+1 hour" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+1H +%Y-%m-%dT%H:%M:%SZ)'",
    "createdBy": "SRE On-Call",
    "comment": "Scheduled maintenance window"
  }'
```

---

## 7. Escalation Policy

1. **CRITICAL Alerts (Page On-Call SRE):**
   - Target response time: < 15 minutes
   - Automated routing to PagerDuty / High-Priority Slack channel
   - Primary action: Follow corresponding runbook immediately

2. **WARNING Alerts (Create Ticket / Notify Channel):**
   - Target response time: Next business hour / < 4 hours
   - Automated routing to Slack `#vetra-alerts-warning`
   - Primary action: Review metrics, investigate trend, schedule remediation

3. **INFO / Watchdog Alerts (Canary):**
   - Continuous verification of alerting engine
