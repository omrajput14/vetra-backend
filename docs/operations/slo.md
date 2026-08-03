# Vetra Platform — Service Level Objectives (SLO) & Error Budget Manual

**Stage:** 12.4.3 — Enterprise Alerting & SLO Monitoring
**Role:** Principal SRE & Software Architect
**Status:** PRODUCTION READY

---

## 1. Overview & Framework

Service Level Objectives (SLOs) define the target reliability for Vetra Platform services. Each SLO is tied to a quantifiable Service Level Indicator (SLI) measured continuously by Prometheus.

$$ \text{Error Budget} = 100\% - \text{SLO Target} $$

For a 30-day calendar month ($43,200 \text{ minutes}$):

| Target | Allowed Downtime / Error Budget |
|---|---|
| 99.9% | 43.2 minutes / month |
| 99.5% | 3.6 hours / month |
| 99.0% | 7.2 hours / month |

---

## 2. Core Service Level Objectives

### 2.1 Backend Availability SLO
- **SLI:** `vetra:backend_up:instant == 1`
- **SLO Target:** 99.9% monthly availability
- **Error Budget:** 43.2 minutes of unreachability per 30-day window
- **PromQL Measurement:** `avg_over_time(up{job="vetra-backend"}[30d])`

### 2.2 HTTP Success Rate SLO
- **SLI:** `vetra:http_request_success_rate:5m`
- **SLO Target:** 99.5% non-5xx HTTP response status
- **Error Budget:** 0.5% failed requests out of total throughput
- **PromQL Measurement:** `1 - (sum(rate(http_server_requests_seconds_count{outcome="SERVER_ERROR"}[30d])) / sum(rate(http_server_requests_seconds_count}[30d])))`

### 2.3 HTTP Response Latency SLO (User Experience)
- **SLI:** `vetra:http_latency_p95:5m`
- **SLO Target:** 95% of HTTP requests completed in $< 250\text{ ms}$
- **PromQL Measurement:** `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[30d])))`

### 2.4 Cache Layer Efficiency SLO (Quality of Service)
- **SLI:** `vetra:cache_hit_ratio:5m`
- **SLO Target:** $> 85\%$ overall cache hit ratio across warm cache regions
- **PromQL Measurement:** `sum(rate(cache_gets_total{result="hit"}[30d])) / (sum(rate(cache_gets_total{result="hit"}[30d])) + sum(rate(cache_gets_total{result="miss"}[30d])))`

### 2.5 Database Availability SLO (Infrastructure Proxy)
- **SLI:** `hikaricp_connections_timeout_total` rate per second
- **SLO Target:** 99.95% connection acquisition success ($< 0.05\%$ timeout rate)
- **Error Budget:** 21.6 minutes of connection exhaustion per 30-day window

---

## 3. Burn Rate Alerting Strategy

Rather than alerting only when an error budget is completely exhausted, Vetra implements multi-window burn rate alerting based on Google SRE principles:

| Burn Rate | % Budget Consumed | Window | Severity | Action |
|---|---|---|---|---|
| **2x Fast Burn** | > 1.0% error rate | 5 minutes | CRITICAL | Immediate On-Call Page (`VetraSloBurnRateCritical`) |
| **1x Slow Burn** | > 0.5% error rate | 15 minutes | WARNING | Ticket / Slack Notification (`VetraSloBurnRateWarning`) |
