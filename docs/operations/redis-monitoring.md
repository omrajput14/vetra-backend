# Vetra Platform — Redis Infrastructure Monitoring Manual

**Stage:** 12.4.4 — Redis Exporter & Infrastructure Metrics
**Role:** Principal SRE & Platform Reliability Engineer
**Status:** PRODUCTION READY
**Last Updated:** August 2026

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                 Vetra Redis Infrastructure Observability                 │
│                                                                          │
│   Redis 7 Container (vetra-redis:6379)                                   │
│   └── Password Auth (${REDIS_PASSWORD})                                  │
│            │                                                            │
│            ▼ (INFO command scraping)                                    │
│   Redis Exporter Container (oliver006/redis_exporter:v1.67.0-alpine:9121)│
│   └── Exposes /metrics                                                   │
│            │                                                            │
│            ▼ (5s scrape interval)                                       │
│   Prometheus (:9090)                                                    │
│   ├── Recording Rules (80-redis-recording-rules.yml)                   │
│   │   ├── vetra:redis_memory_usage:instant                              │
│   │   ├── vetra:redis_cache_hit_ratio:5m                                │
│   │   └── vetra:redis_ops_rate:5m                                       │
│   ├── Alert Rules (90-redis-alerts.yml)                                 │
│   └── Alertmanager Router (:9093)                                       │
│            │                                                            │
│            ▼ (Dashboard auto-provisioned)                               │
│   Grafana (:3000/d/vetra-redis-infra)                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Metric Catalogue

| Metric | Source | Type | Description |
|---|---|---|---|
| `redis_up` | redis_exporter | Gauge | 1 if Redis server is reachable, 0 otherwise |
| `redis_memory_used_bytes` | redis_exporter | Gauge | Total memory allocated by Redis in bytes |
| `redis_memory_max_bytes` | redis_exporter | Gauge | Max memory configured in redis.conf / CLI |
| `redis_mem_fragmentation_ratio` | redis_exporter | Gauge | Ratio of memory allocated by OS to allocated by Redis |
| `redis_connected_clients` | redis_exporter | Gauge | Number of client connections |
| `redis_blocked_clients` | redis_exporter | Gauge | Number of clients pending on blocking calls |
| `redis_commands_processed_total` | redis_exporter | Counter | Total commands processed by server |
| `redis_keyspace_hits_total` | redis_exporter | Counter | Successful key lookups in main dictionary |
| `redis_keyspace_misses_total` | redis_exporter | Counter | Failed key lookups in main dictionary |
| `redis_evicted_keys_total` | redis_exporter | Counter | Keys evicted due to maxmemory limit |
| `redis_expired_keys_total` | redis_exporter | Counter | Keys expired naturally via TTL |
| `redis_rejected_connections_total` | redis_exporter | Counter | Connections rejected due to maxclients |
| `redis_net_input_bytes_total` | redis_exporter | Counter | Total bytes received |
| `redis_net_output_bytes_total` | redis_exporter | Counter | Total bytes sent |

---

## 3. Recording Rules Catalog (`80-redis-recording-rules.yml`)

| Recording Rule | PromQL Expression | Purpose |
|---|---|---|
| `vetra:redis_up:instant` | `up{job="redis-exporter"}` | Canonical Redis health indicator |
| `vetra:redis_memory_usage:instant` | `redis_memory_used_bytes / (redis_memory_max_bytes > 0)` | Memory occupancy ratio (0-1) |
| `vetra:redis_mem_fragmentation_ratio:instant` | `redis_mem_fragmentation_ratio` | Fragmentation index |
| `vetra:redis_cache_hit_ratio:5m` | `hits / (hits + misses)` rate ratio | Server-level keyspace hit ratio |
| `vetra:redis_ops_rate:5m` | `sum(rate(redis_commands_processed_total[5m]))` | Operations throughput |
| `vetra:redis_eviction_rate:5m` | `sum(rate(redis_evicted_keys_total[5m]))` | Key eviction rate |
| `vetra:redis_connected_clients:instant` | `redis_connected_clients` | Connected client count |
| `vetra:redis_blocked_clients:instant` | `redis_blocked_clients` | Blocked client count |

---

## 4. Alert Catalog (`90-redis-alerts.yml`)

| Alert Name | Severity | Condition | For | Runbook |
|---|---|---|---|---|
| **VetraRedisDown** | CRITICAL | `vetra:redis_up:instant == 0` | 1m | `RedisDown.md` |
| **VetraRedisMemoryCritical** | CRITICAL | `vetra:redis_memory_usage:instant > 0.90` | 5m | `RedisHighMemory.md` |
| **VetraRedisMemoryWarning** | WARNING | `vetra:redis_memory_usage:instant > 0.80` | 10m | `RedisHighMemory.md` |
| **VetraRedisHighEvictionRate** | WARNING | `vetra:redis_eviction_rate:5m > 10` | 5m | `RedisHighEviction.md` |
| **VetraRedisRejectedConnections** | CRITICAL | `vetra:redis_rejected_connections_rate:5m > 0` | 1m | `RedisHighConnections.md` |
| **VetraRedisBlockedClients** | WARNING | `vetra:redis_blocked_clients:instant > 2` | 3m | `RedisHighConnections.md` |
| **VetraRedisHighConnectedClients** | WARNING | `vetra:redis_connected_clients:instant > 200` | 5m | `RedisHighConnections.md` |
| **VetraRedisCacheHitRatioLow** | WARNING | `vetra:redis_cache_hit_ratio:5m < 0.70` | 10m | `RedisHighEviction.md` |

---

## 5. Security & Operations Guidelines

- `redis_exporter` connects to `redis:6379` using the `REDIS_PASSWORD` environment variable.
- The exporter container runs isolated on `vetra-network` and exposes metrics at `:9121`.
- Never expose port 9121 directly to public internet without reverse proxy authentication.
