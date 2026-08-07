# Enterprise AI Observability & Analytics Architecture

## Overview

The Vetra AI Observability Subsystem delivers operational visibility into request execution, provider performance, cache efficiency, governance rejections, token consumption, and cost accounting across the Vetra Platform.

Instrumentation is added **passively** to the frozen execution pipeline without altering request flow or business logic:

```text
AIOrchestrator
      │
      ▼
DefaultAIGateway
      │
      ▼
AIGovernancePipeline ──> AIMetricsCollector (Governance Rejections)
      │
      ▼
AICacheManager       ──> AIMetricsCollector (Cache Lookups, Token/Cost Savings)
      │
      ▼
FailoverManager      ──> AIMetricsCollector (Provider Latency, Retries, Failovers)
      │
      ▼
ProviderRouter → AIProvider
```

---

## Metric Catalog

| Metric Name | Type | Tags | Description |
| :--- | :--- | :--- | :--- |
| `ai_requests_total` | Counter | `status` | Total gateway requests by execution status (`success`/`failure`) |
| `ai_request_duration_seconds` | Timer/Histogram | `provider`, `prompt_id`, `status` | Gateway execution latency timer with p50, p95, p99 percentiles |
| `ai_provider_requests_total` | Counter | `provider`, `status` | Total requests dispatched to provider |
| `ai_provider_latency_seconds` | Timer/Histogram | `provider`, `model` | Direct provider network execution latency timer |
| `ai_governance_rejections_total` | Counter | `governance_type`, `prompt_id`, `prompt_version` | Governance rejections by type (`safety`/`policy`/`budget`) |
| `ai_cache_lookup_total` | Counter | `result` | Cache lookups by result (`hit`/`miss`/`stampede_hit`/`bypass`/`error`) |
| `ai_token_saved_total` | Counter | None | Total tokens saved via cache hits |
| `ai_cost_saved_total` | Counter | None | Total estimated USD cost saved via cache hits |
| `ai_prompt_tokens_total` | Counter | `provider`, `model` | Input prompt tokens consumed |
| `ai_completion_tokens_total` | Counter | `provider`, `model` | Output completion tokens consumed |
| `ai_estimated_cost_total` | Counter | `provider`, `model` | Estimated USD spend based on provider token rates |

---

## Low-Cardinality Metric Tag Policy

To protect Prometheus performance and prevent time-series cardinality explosions, metric tags are strictly restricted to:
- `provider`: Normalized provider identifier (e.g. `gemini`, `noop`)
- `model`: Model alias or ID (e.g. `gemini-1.5-flash`, `noop-default`)
- `prompt_id`: Registered prompt ID (e.g. `diagnosis.visual.v1`)
- `prompt_version`: Prompt template version string (e.g. `1.0.0`)
- `status`: Execution outcome (`success`, `failure`)
- `governance_type`: Rejection phase (`safety`, `policy`, `budget`)
- `result`: Cache outcome (`hit`, `miss`, `stampede_hit`, `bypass`, `error`)

**Strict Exclusions**: User IDs, tenant IDs, correlation IDs, image URLs, cache keys, UUIDs, and prompt text are **never** included in Prometheus tags. High-cardinality metadata is recorded strictly via OpenTelemetry span attributes and structured `AI_AUDIT` log events.

---

## Grafana AI Platform Dashboard

Located at `./docker/grafana/dashboards/ai-platform.json` (`uid: vetra-ai-platform`).

### Panels
1. **Executive Overview**: Total Gateway Requests, Request Success Rate SLA %, p95 Latency, Total Spend (USD).
2. **Provider SLA & Latency**: Provider p95 Latency Timeseries & Throughput by Provider/Status.
3. **Cache Efficiency**: Cache Hit Ratio %, Tokens Saved, Cost Saved (USD), Lookups by Result.
4. **Governance Telemetry**: Safety, Policy, and Budget Rejections timeseries.
5. **Resilience Telemetry**: Retries, Failovers, and Circuit Breaker Open events.

---

## Prometheus Alert Rules

Defined in `./docker/prometheus/rules/100-ai-alerts.yml`:

- **`AISuccessRateLow`** (Critical): Gateway request success rate < 95% over 5m.
- **`AIProviderUnavailable`** (Critical): Provider failures > 5 in 3m.
- **`AICircuitBreakerOpen`** (Critical): Circuit breaker tripped open.
- **`AIRetrySpike`** (Warning): Request retries > 10 in 5m.
- **`AICacheHitRatioLow`** (Warning): Cache hit ratio < 20% over 10m.
- **`AIGovernanceRejectionSpike`** (Warning): Governance rejections > 15 in 5m.
- **`AIP95LatencyHigh`** (Warning): p95 request latency > 5.0s.

---

## Operational Runbook: AI Provider Degradation

### Symptom
`AIProviderUnavailable` or `AIRetrySpike` alert fires in Alertmanager / Slack.

### Diagnostics
1. Open Grafana `Vetra AI Platform Dashboard`.
2. Check **Provider SLA & Latency** panel to isolate failing provider.
3. Inspect OpenTelemetry traces in Grafana Tempo filtered by `provider=<failing_provider>`.

### Remediation
1. Verify provider API status page and API key quota limits.
2. If provider is down, `FailoverManager` automatically routes traffic to secondary provider (`noop` or fallback).
3. If necessary, adjust provider priorities or disable failing provider dynamically in `application.yml` / environment variables (`GEMINI_ENABLED=false`).
