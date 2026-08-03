# Runbook: High User Login Failure Rate

**Alerts:** `VetraHighLoginFailureRate`
**Severity:** WARNING
**Target SLA:** Response < 1 hour

---

## Symptoms
- Combined login failure rate exceeds $0.2\text{ failures/sec}$ ($> 12\text{ failures/min}$) sustained for 5 minutes
- Spikes in `vetra_auth_login_total{result="failure"}`

## Likely Causes
1. Credential stuffing or credential brute-force attack targeting `/api/v1/auth/*/login`
2. Frontend/Mobile application bug submitting corrupted payloads
3. Authentication service issue (e.g. password encoder failure or DB lookup issue)

## Investigation Steps
1. **Check Grafana Business Metrics Dashboard:** `http://localhost:3000/d/vetra-business`
2. **Inspect login failure breakdown by role:**
   ```bash
   curl -s http://localhost:8080/actuator/prometheus | grep vetra_auth_login_total
   ```
3. **Inspect backend HTTP logs for requesting IPs and status codes:**
   ```bash
   docker compose logs vetra-backend --tail=100 | grep -i "login"
   ```

## Verification Commands
```bash
# Query login failure rate recording rule
curl -s "http://localhost:9090/api/v1/query?query=vetra:login_failure_rate:5m" | python3 -m json.tool
```

## Recovery Procedure
1. **If brute-force attack from specific IP detected:** Block IP at gateway / WAF firewall.
2. **If frontend payload issue:** Coordinate with Mobile/Frontend team to roll back buggy release.
3. **Ensure rate limiting filters are operational.**

## Escalation Guidance
Escalate to Security Operations Lead if login failure rate exceeds 5/sec (potential active credential stuffing campaign).
