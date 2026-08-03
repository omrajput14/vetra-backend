# Runbook: HikariCP Database Connection Pool Exhaustion

**Alerts:** `VetraDbConnectionPoolExhausted`, `VetraDbConnectionTimeout`, `VetraDbHighConnectionUsage`, `VetraCacheHitRatioLow`
**Severity:** CRITICAL / WARNING
**Target SLA:** Response < 15 minutes (Critical) / < 1 hour (Warning)

---

## Symptoms
- Threads pending for DB connections $> 5$
- HikariCP connection acquisition timeouts occurring
- Connection pool occupancy $> 80\%$ (24+ active connections out of 30 max)

## Likely Causes
1. Unindexed database queries causing long-running transactions
2. Connection leaks (transactions opened without closing connections)
3. Heavy write traffic or slow PostgreSQL disk I/O
4. Sudden cache miss spike redirecting all traffic to DB

## Investigation Steps
1. **Check Grafana PostgreSQL Dashboard:** `http://localhost:3000/d/vetra-postgres`
2. **Inspect connection pool state:**
   ```bash
   curl -s http://localhost:8080/actuator/prometheus | grep hikaricp_connections
   ```
3. **Check active queries in PostgreSQL container:**
   ```bash
   docker compose exec postgres psql -U vetra_user -d vetra_db -c \
     "SELECT pid, now() - query_start AS duration, query, state FROM pg_stat_activity WHERE state != 'idle' ORDER BY duration DESC;"
   ```

## Verification Commands
```bash
# Query pool usage recording rule
curl -s "http://localhost:9090/api/v1/query?query=vetra:db_connection_usage:instant" | python3 -m json.tool

# Query timeout rate recording rule
curl -s "http://localhost:9090/api/v1/query?query=vetra:db_connection_timeout_rate:5m" | python3 -m json.tool
```

## Recovery Procedure
1. **Kill long-running blocking queries in PostgreSQL (if appropriate):**
   ```bash
   docker compose exec postgres psql -U vetra_user -d vetra_db -c \
     "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE now() - query_start > interval '30 seconds' AND state != 'idle';"
   ```
2. **If connection leak confirmed, restart backend container:**
   ```bash
   docker compose restart vetra-backend
   ```

## Escalation Guidance
Escalate to DBA / Senior Backend Engineer if database connections exhaust immediately upon restart.
