# Enterprise Longitudinal Clinical Case Management & Treatment Response Tracking Architecture

## Executive Summary

Stage 13.1.6 extends the Vetra Clinical Intelligence platform from isolated clinical encounters into a **longitudinal veterinary case management system**. It connects multiple encounters, diagnostic scans, treatments, follow-up inspections, multi-modal evidence changes, and clinical outcomes over time.

The underlying **AI Platform v1.1 execution pipeline** (`DefaultAIGateway` → `AIGovernancePipeline` → `AICacheManager` → `FailoverManager` → `ProviderRouter` → `AIProvider`) and the **Multi-Agent Framework** (`AgentGateway` → `AgentRegistry` → `AIAgent`) remain **100% frozen, immutable, and preserved**. Zero AI agents or LLM calls were introduced.

---

## 1. Longitudinal Architecture & Post-Encounter Integration Flow

```text
Encounter Workflow (9 Steps) ── Order 1 to 9 (Diagnosis → Evidence → RAG → Ranking → Triage → Treatment → CDS → ActionPlan → Report)
        │
        ▼
ClinicalDiagnosisReport
        │
        ▼ (Post-Encounter Integration)
ClinicalCaseService
        ├── Create / retrieve ClinicalCase
        ├── Append immutable ClinicalEncounter
        └── Append ClinicalTimelineEvent
                │
                ▼
        FollowUpAssessmentService
                │
                ▼
        ClinicalProgressAnalyzer (Deterministic Non-AI Comparison)
                │
                ▼
        TreatmentResponse (IMPROVING | STABLE | WORSENING | NO_MEASURABLE_CHANGE | INSUFFICIENT_DATA)
                │
        ┌───────┼────────┐
        ▼       ▼        ▼
    IMPROVING STABLE  WORSENING
        │       │        │
        ▼       ▼        ▼
     Continue Monitor  Escalate (Trigger ClinicalConditionWorsenedEvent & Case Referral)
```

---

## 2. Core Architectural Principles & Safety Invariants

1. **Strict Determinism**: `ClinicalProgressAnalyzer` and `ClinicalCaseService` are pure deterministic Spring components. Zero AI calls or LLM entries are executed.
2. **Historical Immutability**: `ClinicalEncounter` and `ClinicalTimelineEvent` objects are append-only. No `updateEncounter()`, `deleteEncounter()`, `updateTimelineEvent()`, or `deleteTimelineEvent()` methods exist. Past encounters are never altered.
3. **Post-Encounter Execution**: Longitudinal processing occurs AFTER the 9-step clinical encounter workflow has successfully generated a `ClinicalDiagnosisReport`. The 9-step workflow sequence remains 100% untouched.
4. **Conservative Data Sufficiency**: If comparable evidence is missing, incompatible, or non-overlapping, `TreatmentResponseStatus.INSUFFICIENT_DATA` is returned. Clinical improvement is NEVER inferred from missing data, diagnostic confidence increases, or reduced triage urgency alone.
5. **Like-for-Like Evidence Comparison**: Comparisons are strictly enforced across matching test names, measurement types, units, and valid timestamps. Raw numeric values are preserved.
6. **Escalation Safety Precedence**: `WORSENING` treatment response, new critical lab/vital abnormality, increased triage urgency, or veterinarian review flag immediately triggers referral escalation.

---

## 3. Case Lifecycle & State Transitions

```text
OPEN ──► UNDER_TREATMENT ──► FOLLOW_UP_REQUIRED ──► RESOLVED ──► CLOSED
  │             │                   │
  └─────────────┴───────────────────┴──────► REFERRED ──► CLOSED
```

Invalid state transitions (e.g. `CLOSED` -> `OPEN` or arbitrary status skips) are strictly rejected with an `IllegalStateException`.

---

## 4. In-Memory Reference Repository

`InMemoryClinicalCaseRepository` provides a thread-safe, `ConcurrentHashMap`-backed reference implementation enforcing append-only historical encounters and timeline events. The interface `ClinicalCaseRepository` is provider-independent and ready for future PostgreSQL persistence without changing domain or service abstractions.

---

## 5. Observability & Safe Telemetry

### Micrometer Metrics
- `clinical_cases_total`: Tagged with `case_status`.
- `clinical_encounters_total`: Tagged with `encounter_type`, `urgency`.
- `treatment_response_total`: Tagged with `response_status`.
- `clinical_condition_worsened_total`: Tagged with `urgency`.

### OpenTelemetry Span Events
- `case.created`
- `encounter.recorded`
- `clinical.timeline.updated`
- `followup.assessment.started`
- `treatment.response.assessed`
- `clinical.condition.worsened`
- `clinical.case.resolved`

### Safe Telemetry Invariants
Strictly excludes PII, UUIDs, animal metrics, diagnosis names, medication names, clinical text, and prompt narratives. Contains only stable categorical low-cardinality values.
