# Runbook: High Redis Eviction Rate / Low Cache Hit Ratio

**Alerts:** `VetraRedisHighEvictionRate`, `VetraRedisCacheHitRatioLow`
**Severity:** WARNING
**Target SLA:** Response < 1 hour

---

## Symptoms
- Key eviction rate $> 10\text{ keys/s}$
- Keyspace hit ratio $< 70\%$
- Database query load increasing due to cache misses

## Likely Causes
1. Redis `maxmemory` policy evicting keys before TTL expires
2. Cache key invalidation logic triggering prematurely
3. Hot keys expiring simultaneously (cache stampede)

## Investigation Steps
1. **Check Grafana Redis Dashboard:** `http://localhost:3000/d/vetra-redis-infra`
2. **Inspect eviction stats:**
   ```bash
   docker compose exec redis redis-cli -a vetra_redis_password_secret info stats | grep evicted
   ```

## Recovery Procedure
1. **Increase Redis maxmemory allocation if host memory available.**
2. **Review TTL configuration for high-churn cache regions.**
