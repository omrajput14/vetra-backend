# Enterprise Clinical Case Operations, Work Queues & Operational Dashboard Architecture

## 1. Overview

Stage 13.1.8 extends the Vetra Clinical Intelligence platform by exposing clinical, longitudinal, and care-coordination state through deterministic operational read models, work queues, role-based views, and aggregate dashboard summaries.

The system answers key operational questions:
- Which cases currently require attention?
- Which cases are emergencies?
- Which veterinarian reviews are pending?
- Which care tasks are due or overdue?
- Which follow-ups are missed?
- Which animals have worsening treatment responses?
- What is the current workload for a veterinarian or care team?

---

## 2. Invariants & Architectural Boundaries

1. **Zero AI / Zero LLM**: No new AI agents, LLM calls, AI providers, prompts, model inference, or AI prioritization. All read models and work queues are 100% deterministic Spring components.
2. **Frozen AI Platform v1.1 Preserved**: `DefaultAIGateway` → `AIGovernancePipeline` → `AICacheManager` → `FailoverManager` → `ProviderRouter` → `AIProvider` remains completely frozen.
3. **Frozen Multi-Agent Framework Preserved**: `AgentGateway` → `AgentRegistry` → `AIAgent` remains completely frozen.
4. **9-Step Workflow Unchanged**: The 9-step encounter workflow sequence remains byte-for-byte compatible and untouched. Operations execute strictly **post-encounter** after longitudinal case management and care-coordination processing.
5. **Pure Projection Read-Models**: Projections map canonical state without inventing clinical conclusions, modifying source entities, or creating competing sources of truth.
6. **Centralized Deterministic Precedence**: All work queues enforce the exact same precedence mechanism:
   `EMERGENCY` > `VETERINARIAN_REVIEW_REQUIRED` > `WORSENING` > `FOLLOW_UP_OVERDUE` > `CARE_TASK_OVERDUE` > `UNDER_TREATMENT` > `FOLLOW_UP_REQUIRED` > `REFERRED` > `STABLE` > `RESOLVED` > `CLOSED`.
7. **Stable Pagination (`PageResult<T>`)**: Equal-priority items are deterministically sorted using tie-breakers: `urgency rank` → `nextDueAt` → `lastEncounterAt` → `caseId`.
8. **Role-Based Data Minimization**: `FarmerOperationalCaseView` excludes diagnostic uncertainty internals, evidence conflicts, literature citations, AI/provider metadata, telemetry, and internal task provenance.

---

## 3. Operational Domain Architecture

```text
Existing 9-Step Encounter Workflow (Diagnosis → Evidence → RAG → Ranking → Triage → Treatment → CDS → ActionPlan → Report)
        │
        ▼
ClinicalDiagnosisReport
        │
        ▼
ClinicalCaseService (Longitudinal Case Management)
        │
        ▼
CareCoordinationService (Care Coordination & Follow-Up Orchestration)
        │
        ▼
Canonical Persistent State (ClinicalCase, Encounters, ActionPlan, CDS, TreatmentResponse, CareTasks, FollowUpSchedules)
        │
        ▼ (Stage 13.1.8 Post-Encounter Operational Projection Layer)
ClinicalOperationsDashboardService
        ├── ClinicalCaseOperationalView (Canonical Case State Projection)
        ├── ClinicalCaseWorkQueue (Case Queue Items & Priority Sorting)
        ├── CareTaskWorkQueueService (Task Filtering, Priority Sorting & PageResult<T>)
        ├── VeterinarianWorkQueueService (Emergency, Review, Worsening & Overdue Queues)
        ├── FarmerOperationalCaseView (Caregiver Data Minimization Projection)
        ├── VeterinarianOperationalCaseView (Clinical Team Traceability Projection)
        └── ClinicalOperationsDashboardSummary (Aggregate Operational Metrics)
```

---

## 4. Telemetry & Low-Cardinality Observability

- **Metrics**: `clinical_operations_dashboard_total`, `clinical_case_queue_total`, `clinical_care_task_queue_total`, `clinical_veterinarian_queue_total`.
- **Allowed Low-Cardinality Tags**: `queue_type`, `priority`, `urgency`, `task_type`, `actor`, `status`, `operational_status`. High-cardinality IDs and text are strictly excluded.
- **Span Events**: `operations.dashboard.generated`, `case.queue.generated`, `care.task.queue.generated`, `veterinarian.queue.generated`.
