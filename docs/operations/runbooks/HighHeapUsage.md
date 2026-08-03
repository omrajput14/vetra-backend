# Runbook: High JVM Heap Usage / GC Pressure

**Alerts:** `VetraJvmHeapCritical`, `VetraJvmHeapWarning`, `VetraJvmGcPauseHigh`
**Severity:** CRITICAL / WARNING
**Target SLA:** Response < 15 minutes (Critical) / < 1 hour (Warning)

---

## Symptoms
- JVM Heap usage $> 80\%$ or $> 90\%$ sustained
- Frequent G1 GC evacuation pauses exceeding $500\text{ ms}$
- Slow HTTP response times due to Stop-The-World GC pauses

## Likely Causes
1. Memory leak in Spring singleton scope or cached collections
2. Unbounded query result set loaded into heap without pagination
3. Large payload upload / JSON deserialization in memory
4. Insufficient max heap size allocation (`-Xmx`)

## Investigation Steps
1. **Check Grafana JVM Dashboard:** `http://localhost:3000/d/vetra-jvm`
2. **Inspect memory breakdown in Prometheus:**
   ```bash
   curl -s http://localhost:8080/actuator/prometheus | grep jvm_memory_used_bytes
   ```
3. **Check GC pause statistics:**
   ```bash
   curl -s http://localhost:8080/actuator/prometheus | grep jvm_gc_pause_seconds
   ```

## Verification Commands
```bash
# Query current heap occupancy ratio via Prometheus API
curl -s "http://localhost:9090/api/v1/query?query=vetra:jvm_heap_usage:instant" | python3 -m json.tool
```

## Recovery Procedure
1. **Trigger manual GC (if management endpoint enabled):**
   Or gracefully restart backend container to release heap leak:
   ```bash
   docker compose restart vetra-backend
   ```
2. **If heap leak recurs rapidly, obtain heap dump for analysis:**
   ```bash
   docker compose exec vetra-backend jcmd 1 GC.heap_dump /tmp/heap.hprof
   ```

## Escalation Guidance
Escalate to JVM Performance Specialist with heap dump artifact if heap grows linearly under steady-state load.
