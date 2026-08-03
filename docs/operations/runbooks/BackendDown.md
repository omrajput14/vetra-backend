# Runbook: Vetra Backend Down / Unreachable

**Alerts:** `VetraBackendDown`, `VetraPrometheusDown`
**Severity:** CRITICAL
**Target SLA:** Response < 15 minutes

---

## Symptoms
- Prometheus cannot scrape `vetra-backend:8080/actuator/prometheus`
- API calls returning `502 Bad Gateway` or `Connection Refused`
- Health check `/actuator/health` failing

## Likely Causes
1. Container crash due to OutOfMemory (OOMKilled)
2. Database / Redis startup dependency failure
3. Unhandled JVM panic or native stack overflow
4. Docker network / bridge interface failure

## Investigation Steps
1. **Check container status:**
   ```bash
   docker compose ps vetra-backend
   ```
2. **Inspect recent container logs:**
   ```bash
   docker compose logs vetra-backend --tail=100
   ```
3. **Check host resource usage (CPU / Memory):**
   ```bash
   docker stats vetra-backend --no-stream
   ```
4. **Check if OOMKilled by kernel:**
   ```bash
   docker inspect vetra-backend --format='{{.State.OOMKilled}}'
   ```

## Verification Commands
```bash
# Test internal actuator endpoint
curl -v http://localhost:8080/actuator/health

# Test Prometheus scrape target
curl -v http://localhost:8080/actuator/prometheus | head -10
```

## Recovery Procedure
1. **Restart backend container:**
   ```bash
   docker compose restart vetra-backend
   ```
2. **If container fails to start, rebuild image:**
   ```bash
   docker compose build vetra-backend && docker compose up -d vetra-backend
   ```
3. **Verify alert resolution in Alertmanager:**
   `http://localhost:9093/#/alerts`

## Escalation Guidance
If restart fails due to persistent DB/Redis connection issues, check `vetra-postgres` and `vetra-redis` health. Escalate to Lead Backend Engineer.
