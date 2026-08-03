# Runbook: Redis Server Down / Unreachable

**Alert:** `VetraRedisDown`
**Severity:** CRITICAL
**Target SLA:** Response < 15 minutes

---

## Symptoms
- Prometheus metric `vetra:redis_up:instant == 0`
- Spring Boot backend logging `RedisConnectionFailureException`
- API calls requiring cache falling back to DB or failing

## Likely Causes
1. `vetra-redis` container crashed or stopped
2. `vetra-redis-exporter` authentication failure (wrong password)
3. Docker bridge network disruption

## Investigation Steps
1. **Check container status:**
   ```bash
   docker compose ps vetra-redis vetra-redis-exporter
   ```
2. **Inspect Redis container logs:**
   ```bash
   docker compose logs vetra-redis --tail=100
   ```
3. **Inspect Redis Exporter container logs:**
   ```bash
   docker compose logs vetra-redis-exporter --tail=100
   ```

## Verification Commands
```bash
# Ping Redis directly using redis-cli
docker compose exec redis redis-cli -a vetra_redis_password_secret ping

# Check exporter health endpoint
curl -v http://localhost:9121/health
```

## Recovery Procedure
1. **Restart Redis container:**
   ```bash
   docker compose restart vetra-redis vetra-redis-exporter
   ```
2. **Verify alert resolution in Alertmanager:** `http://localhost:9093/#/alerts`
