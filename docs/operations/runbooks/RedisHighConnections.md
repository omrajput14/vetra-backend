# Runbook: High Redis Connections / Blocked Clients / Rejected Connections

**Alerts:** `VetraRedisRejectedConnections`, `VetraRedisBlockedClients`, `VetraRedisHighConnectedClients`
**Severity:** CRITICAL / WARNING
**Target SLA:** Response < 15 minutes (Critical) / < 1 hour (Warning)

---

## Symptoms
- Redis rejecting client connections
- Blocked clients $> 2$
- Connected clients $> 200$

## Likely Causes
1. Connection pool leaks in application client (Lettuce / Jedis)
2. `maxclients` limit reached in `redis.conf`
3. Long-running blocking commands (e.g. `KEYS *` or `BLPOP`) executed against server

## Investigation Steps
1. **Inspect connected clients:**
   ```bash
   docker compose exec redis redis-cli -a vetra_redis_password_secret client list
   ```
2. **Check slow log for blocking commands:**
   ```bash
   docker compose exec redis redis-cli -a vetra_redis_password_secret slowlog get 10
   ```

## Recovery Procedure
1. **Kill rogue client connections if necessary:**
   ```bash
   docker compose exec redis redis-cli -a vetra_redis_password_secret client kill TYPE normal
   ```
2. **If connection leak confirmed in application, restart backend container:**
   ```bash
   docker compose restart vetra-backend
   ```
