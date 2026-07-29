# Disaster Recovery & Backup Plan
**Document ID:** OPS-17  
**Status:** Active (Strategy) / Planned (Implementation)  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Deployment Architecture](../architecture/09-deployment.md), [Security Design](../security/11-security-design.md)

---

## Objectives

This document defines Vetra's approach to disaster recovery (DR) and data backup. The goal is to ensure that a critical failure — hardware failure, accidental data deletion, database corruption, or security breach — does not result in permanent data loss or unacceptable service downtime.

---

## Recovery Targets

| Metric | Definition | Target |
|---|---|---|
| **RTO** (Recovery Time Objective) | Maximum acceptable downtime | **4 hours** (current) → **30 minutes** (production target) |
| **RPO** (Recovery Point Objective) | Maximum acceptable data loss | **24 hours** (current) → **1 hour** (production target) |

**Current RTO/RPO are for the local/dev environment only.** Production targets must be achieved by the time Vetra has paying users.

---

## Backup Strategy

### Database Backups

#### Local Development
No formal backup required. PostgreSQL data is stored in a Docker volume (`postgres-data/`). Data loss on `docker compose down -v` is expected and acceptable.

#### Production (Planned — AWS RDS)

| Backup Type | Frequency | Retention | Storage |
|---|---|---|---|
| Automated RDS backup | Daily | 7 days | AWS S3 (managed by RDS) |
| Manual snapshot (pre-migration) | Before every Flyway migration | 30 days | AWS S3 |
| Point-in-time recovery (PITR) | Continuous (5-min granularity) | 7 days | AWS S3 |
| Cross-region replica | Continuous | N/A | Secondary AWS region |

#### Backup Verification

Backups must be tested regularly:
- **Monthly:** Restore the most recent automated backup to a test RDS instance and run `./mvnw verify` integration tests against it.
- **Quarterly:** Full DR drill — simulate a production database failure and measure actual RTO.
- All test restores must be documented with timestamp and outcome.

---

## Disaster Scenarios and Response Plans

### Scenario 1: Application Server Failure

**Cause:** ECS instance failure, JVM crash, OOM kill  
**Detection:** ALB health check fails, CloudWatch alarm fires  
**Impact:** API unavailable; database unaffected

**Recovery Steps:**
1. ECS auto-restart launches a new task from the existing Docker image (automatic — target 2 min)
2. If auto-restart fails, manually trigger a new ECS deployment
3. Verify API health: `curl https://api.vetra.app/actuator/health`
4. Check CloudWatch logs for root cause
5. Document incident in the decision log

**Target RTO:** 2–5 minutes (auto-recovery)

---

### Scenario 2: Database Corruption or Accidental Data Deletion

**Cause:** Buggy migration, `DELETE` without `WHERE`, SQL injection (if input validation bypassed)  
**Detection:** Application errors, user complaints, monitoring alert  
**Impact:** Data loss; service degraded or down

**Recovery Steps:**
1. **Immediately:** Put the application in maintenance mode (return `503` from load balancer)
2. Assess the scope: which tables, how many rows, time window of corruption
3. Identify the last clean point-in-time recovery (PITR) before corruption
4. Restore to PITR on a new RDS instance (do not overwrite production)
5. Compare restored data with production — export the delta
6. Apply the delta to production database manually (with transaction)
7. Take a manual RDS snapshot of the repaired state
8. Restore the application from maintenance mode
9. Document the incident and root cause

**Target RTO:** 2–4 hours  
**Target RPO:** Last 5-minute PITR checkpoint

---

### Scenario 3: Security Breach — Compromised JWT Secret

**Cause:** JWT secret exposed in logs, committed to source control, or exfiltrated  
**Impact:** All active sessions vulnerable to forgery; attacker can impersonate any user

**Recovery Steps (Immediate — within minutes):**
1. Rotate the `JWT_SECRET` environment variable immediately
2. Restart all application instances (new secret takes effect)
3. All existing JWT tokens are immediately invalid — users must re-login
4. Revoke all refresh tokens in the database: `UPDATE refresh_tokens SET revoked = TRUE;`
5. Investigate how the secret was exposed
6. Review all recent API access logs for suspicious patterns
7. Document and report

**Target RTO:** 5 minutes (secret rotation + restart)

---

### Scenario 4: Complete Region Failure (Planned)

**Cause:** AWS region outage (rare)  
**Detection:** All services unreachable from monitoring in secondary region  
**Impact:** Complete service unavailability

**Recovery Steps (Planned — requires cross-region infrastructure):**
1. Promote cross-region RDS read replica to primary (manual, ~5 min)
2. Update Route 53 DNS to point to secondary region load balancer
3. Scale up application instances in secondary region
4. Verify API health in secondary region
5. Notify users of service restoration

**Target RTO:** 30 minutes (secondary region warm standby)

---

## Data Retention Policy

| Data Type | Retention | Reason |
|---|---|---|
| Medical records | Permanent (never deleted) | Legal/regulatory requirement |
| Appointment records | Permanent | Clinical audit trail |
| User accounts | Duration of account + 2 years | Legal requirement |
| Refresh tokens (revoked/expired) | Purged after 30 days | Storage hygiene |
| Application logs | 90 days (production) | Incident investigation |
| Database backups | 7 days (automated), 30 days (manual) | Recovery capability |

> [!CAUTION]
> Medical records are **legally required to be retained permanently** in most jurisdictions. Never implement a medical record deletion feature without explicit legal review. The current API has no DELETE endpoint for medical records — this is intentional.

---

## Runbooks (Planned)

Detailed step-by-step runbooks for each disaster scenario will be created in `docs/operations/runbooks/` before production launch:

- `runbook-01-application-restart.md`
- `runbook-02-database-restore.md`
- `runbook-03-secret-rotation.md`
- `runbook-04-region-failover.md`
