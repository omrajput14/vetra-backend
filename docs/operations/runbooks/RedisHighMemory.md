# Runbook: High Redis Memory Usage

**Alerts:** `VetraRedisMemoryCritical`, `VetraRedisMemoryWarning`
**Severity:** CRITICAL / WARNING
**Target SLA:** Response < 15 minutes (Critical) / < 1 hour (Warning)

---

## Symptoms
- Redis memory usage $> 80\%$ or $> 90\%$ of configured `maxmemory`
- Increasing key eviction rate or out-of-memory errors on `SET` commands

## Likely Causes
1. Unbounded key insertion without TTL configured
2. Memory fragmentation high (`redis_mem_fragmentation_ratio > 1.5`)
3. `maxmemory` setting too restrictive for working dataset

## Investigation Steps
1. **Check Grafana Redis Dashboard:** `http://localhost:3000/d/vetra-redis-infra`
2. **Inspect memory usage details:**
   ```bash
   docker compose exec redis redis-cli -a vetra_redis_password_secret info memory
   ```
3. **Analyze key distribution by pattern:**
   ```bash
   docker compose exec redis redis-cli -a vetra_redis_password_secret --bigkeys
   ```

## Recovery Procedure
1. **Purge expired keys manually or invoke defragmentation:**
   ```bash
   docker compose exec redis redis-cli -a vetra_redis_password_secret memory purge
   ```
2. **If emergency release needed, flush non-critical cache region (e.g. analytics):**
   ```bash
   docker compose exec redis redis-cli -a vetra_redis_password_secret EVAL "return redis.call('del', unpack(redis.call('keys', 'analytics:*')))" 0
   ```
