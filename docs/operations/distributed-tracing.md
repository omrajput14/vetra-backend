# Distributed Tracing Operations Guide

**Stage:** 12.5.0  
**Stack:** Spring Boot 3.5.3 → Micrometer Tracing → OpenTelemetry → Grafana Tempo

---

## Architecture

```
HTTP Request
    │
    ▼
TraceHeaderFilter (Order 3)
    │  Reads active Micrometer span → adds X-Trace-Id / X-Span-Id to response headers
    ▼
Spring MVC (auto-instrumented)
    │  Creates root HTTP span: method, URI, status
    │  Populates traceId + spanId MDC keys for structured logs
    ▼
Service Layer (manual span attributes)
    │  AnimalService: span.tag("animal.species", ...)
    │  AppointmentService: span.tag("appointment.visit_type", ...)
    │  AuthService: span.tag("user.role", ...)
    ▼
JPA / HikariCP (auto-instrumented via datasource-micrometer)
    │  SQL query spans, connection acquisition spans
    ▼
Redis Lettuce (auto-instrumented via Spring Data Redis observation)
    ▼
OTLP/HTTP exporter → Tempo :4318
    ▼
Grafana Tempo (query API :3200)
    ▼
Grafana "Vetra — Distributed Traces" dashboard
```

---

## Searching Traces in Grafana

1. Open Grafana → **Explore** → Select **Tempo** datasource.
2. Use **TraceQL** to query traces:

   ```
   # All recent traces for vetra-backend
   {resource.service.name="vetra-backend"}

   # Only failed traces (span.error)
   {resource.service.name="vetra-backend" && status=error}

   # Traces for animal registrations
   {resource.service.name="vetra-backend" && span.animal.species="CATTLE"}

   # Traces by user role
   {resource.service.name="vetra-backend" && span.user.role="FARMER"}

   # Slow traces (>500ms)
   {resource.service.name="vetra-backend" && duration>500ms}
   ```

3. Click any trace row to open the **trace waterfall view**.
4. Inspect individual spans for SQL queries, Redis commands, and HTTP calls.

---

## Correlating Trace → Log

Every log line produced during a request includes `traceId` and `spanId`:

```
2026-08-04 14:00:00.123 [http-nio-8080-exec-1] [traceId=64e1a3b2c4d5e6f7a8b9c0d1e2f30001] [spanId=a1b2c3d4e5f60001] [req-abc123] INFO  app.vetra.animal.service.AnimalService - ...
```

To find logs for a specific trace:

1. Copy the `X-Trace-Id` response header from the client request.
2. Run: `grep "traceId=<value>" logs/vetra.log`
3. Or in Grafana Tempo, click **Logs** on the trace detail panel (when Loki is configured).

---

## Correlating Metric → Trace (Exemplars)

When exemplar storage is enabled in Prometheus:

1. Open any latency time-series panel in Grafana.
2. Enable **Exemplars** toggle on the panel.
3. Orange dots appear on the chart — these are individual request samples.
4. Click an orange dot → Grafana opens the corresponding trace in Tempo.

---

## X-Trace-Id Response Header

Every HTTP response from Vetra Backend includes:

| Header | Value | Example |
|---|---|---|
| `X-Trace-Id` | W3C trace ID (hex) | `64e1a3b2c4d5e6f7a8b9c0d1e2f30001` |
| `X-Span-Id` | Current span ID (hex) | `a1b2c3d4e5f60001` |

**Use case:** A customer reports an error. The client can capture `X-Trace-Id` from the response and provide it to support, who can then find the exact trace in Grafana Tempo.

If tracing is disabled or the request is not sampled, these headers are absent.

---

## Sampling Configuration

| Environment | Setting | Default |
|---|---|---|
| Development | `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | `1.0` (100%) |
| Production | `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | Recommend `0.1` (10%) |
| Staging | `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | Recommend `0.5` (50%) |

To change the sampling rate without rebuilding:

```bash
# docker-compose .env file
TRACING_SAMPLE_RATE=0.1
```

---

## Adding a Manual Span

Only add manual spans where automatic instrumentation does not capture business-meaningful context.

**Pattern — add a span attribute to the current active span:**

```java
@Service
public class MyService {

  private final Tracer tracer;

  public MyService(Tracer tracer) {
    this.tracer = tracer;
  }

  public void doWork(SomeEnum type) {
    // Auto-instrumented HTTP/JDBC spans are created automatically.
    // Only tag the current span with business context.
    if (tracer.currentSpan() != null && type != null) {
      tracer.currentSpan().tag("my.business.tag", type.name());
    }
    // ... business logic
  }
}
```

**Rules — span attributes MUST be:**

| Allowed | Forbidden |
|---|---|
| Enum values (`FARMER`, `CATTLE`) | UUIDs, database IDs |
| Status codes (`CONFIRMED`, `ERROR`) | Email addresses |
| Role names (`VETERINARIAN`) | Phone numbers |
| Operation types (`EMERGENCY_VISIT`) | JWT tokens |
| Boolean flags | IP addresses |

---

## JDBC Tracing (datasource-micrometer)

SQL query spans appear automatically inside every trace. Each span includes:

- `db.operation` — `SELECT`, `INSERT`, `UPDATE`, `DELETE`
- `db.system` — `postgresql`
- Connection acquisition time

No additional configuration required. The `datasource-micrometer-spring-boot` library instruments the HikariCP DataSource automatically via Spring Boot auto-configuration.

---

## Exception Tracing

When a request fails, the `GlobalExceptionHandler` records:

- **`span.status = ERROR`** (for 5xx errors) — surfaces the span in red in Tempo
- **`exception.type`** span tag — the exception class name (e.g., `ResourceNotFoundException`)
- **Span event** — a timeline event on the span showing the exception type

To find all error traces:

```
{resource.service.name="vetra-backend" && status=error}
```

To find traces for a specific exception type:

```
{resource.service.name="vetra-backend" && span.exception.type="ResourceNotFoundException"}
```

---

## Service Identity in Tempo

Every trace carries standard OTel resource attributes:

| Attribute | Value |
|---|---|
| `service.name` | `vetra-backend` |
| `service.namespace` | `vetra` |
| `service.version` | `0.12.5.0` (or `$SERVICE_VERSION`) |
| `service.instance.id` | `vetra-backend-01` |
| `deployment.environment` | `dev` / `prod` |

These are configurable via environment variables without rebuilding the image.

---

## Tempo Storage

- **Backend:** Local filesystem (`/var/tempo`)
- **Retention:** 7 days (168 hours)
- **Volume:** `tempo_data` (Docker named volume, survives restarts)
- **Query API:** `http://localhost:3200`
- **OTLP receiver:** `http://localhost:4318`

---

## Runbook: Traces Not Appearing in Tempo

1. Verify Tempo is healthy: `curl http://localhost:3200/ready`
2. Verify the backend is sending spans: `curl http://localhost:8080/actuator/health`
3. Check OTLP exporter logs in backend: `docker logs vetra-backend | grep -i "otlp\|trace\|otel"`
4. Verify the Tempo datasource in Grafana → Administration → Data Sources → Tempo → Test
5. If Tempo just started, allow 10–30s for the first traces to be indexed
6. Check sampling probability: is `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` > 0?

## Runbook: X-Trace-Id Header Missing

1. Confirm the request URL is not an excluded path (health probes, actuator endpoints are excluded from tracing scope in Micrometer by default)
2. Confirm sampling probability > 0
3. A no-op Tracer is used when `micrometer-tracing-bridge-otel` is not on the classpath — verify the dependency is present: `docker exec vetra-backend java -cp /app/vetra-backend.jar -Dloader.main=ShowClasspath org.springframework.boot.loader.JarLauncher 2>&1 | grep micrometer`
