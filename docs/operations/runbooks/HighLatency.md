# Runbook: High HTTP Latency / High Error Rate

**Alerts:** `VetraHighLatencyWarning`, `VetraHighLatencyCritical`, `VetraHighErrorRateWarning`, `VetraHighErrorRateCritical`, `VetraSloBurnRateCritical`, `VetraSloBurnRateWarning`, `VetraJvmThreadExplosion`
**Severity:** CRITICAL / WARNING
**Target SLA:** Response < 15 minutes (Critical) / < 1 hour (Warning)

---

## Symptoms
- 95th percentile HTTP response latency exceeds $1.0\text{ s}$ or $3.0\text{ s}$
- 5xx Server Error rate exceeds $1\%$ or $5\%$
- Rapid error budget burn rate detected on SLO alerts

## Likely Causes
1. Slow database queries due to missing indexes or lock contention
2. External API timeout (AWS S3, Gemini AI, FCM)
3. Thread pool saturation / thread starvation
4. Redis cache miss storm forcing heavy DB load

## Investigation Steps
1. **Check Grafana Spring Overview Dashboard:** `http://localhost:3000/d/vetra-spring-overview`
2. **Identify high-error or slow HTTP endpoints:**
   ```bash
   curl -s http://localhost:8080/actuator/prometheus | grep http_server_requests_seconds_count | grep 'status="5'
   ```
3. **Check HikariCP database pool state:**
   ```bash
   curl -s http://localhost:8080/actuator/prometheus | grep hikaricp_connections
   ```
4. **Inspect backend logs for exceptions:**
   ```bash
   docker compose logs vetra-backend --tail=200 | grep -i "error\|exception"
   ```

## Verification Commands
```bash
# Query p95 latency recording rule
curl -s "http://localhost:9090/api/v1/query?query=vetra:http_latency_p95:5m" | python3 -m json.tool

# Query HTTP success rate recording rule
curl -s "http://localhost:9090/api/v1/query?query=vetra:http_request_success_rate:5m" | python3 -m json.tool
```

## Recovery Procedure
1. **If cause is DB lock contention or slow queries:** Check `DatabaseConnectionsExhausted.md` runbook.
2. **If cause is external service timeout:** Check third-party API status (AWS/Gemini).
3. **Restart application instance if threads deadlocked:**
   ```bash
   docker compose restart vetra-backend
   ```

## Escalation Guidance
Escalate to Tech Lead if 5xx errors persist across multiple core endpoints after application restart.
