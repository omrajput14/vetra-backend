# Stage 13.1.7 — Enterprise Clinical Care Coordination & Follow-Up Task Orchestration Architecture

## Overview

Stage 13.1.7 extends the Vetra Clinical Intelligence platform into an operational **Clinical Care Coordination & Follow-Up Task Orchestration** layer. It converts existing structured longitudinal state (`ClinicalCase`, `ClinicalEncounter`, `ClinicalActionPlan`, `ClinicalDecisionSupport`, `TreatmentResponse`, `ClinicalFollowUp`, `ClinicalCaseTimeline`, `TriageAssessment`, `TreatmentPlan`) into deterministic, auditable, and prioritized care-coordination tasks to answer:

> "What clinical actions need to happen next, who is responsible for them, when are they due, what is overdue, and does any unresolved task require escalation?"

---

## Architectural Principles & Safety Invariants

1. **Zero AI / Zero LLM**: `ClinicalCareTaskEngine`, `CareTaskEscalationEngine`, `CareCoordinationService`, and `FollowUpSchedule` are 100% deterministic non-AI Spring components. No new AI agents or LLM calls were introduced.
2. **Frozen AI Platform Preserved**: AI Platform v1.1 (`DefaultAIGateway` → `AIGovernancePipeline` → `AICacheManager` → `FailoverManager` → `ProviderRouter` → `AIProvider`) and Multi-Agent Framework (`AgentGateway` → `AgentRegistry` → `AIAgent`) remain 100% frozen.
3. **9-Step Workflow Unchanged**: The 9-step encounter workflow sequence remains byte-for-byte compatible and untouched. Care coordination executes **after** `ClinicalDiagnosisReport` generation and longitudinal case processing.
4. **Emergency Safety Precedence**: Precedence is strictly enforced (`EMERGENCY` > `HIGH` > `MEDIUM` > `LOW`). Emergency tasks cannot be downgraded or suppressed by routine tasks.
5. **No Manufactured Facts**: All tasks derive strictly from existing structured state. Never manufacture medications, dosages, treatment instructions, or follow-up intervals.
6. **Task Deduplication & Idempotency**: Active tasks are deduplicated by semantic key (`caseId:taskType:actor:sourceActionId:sourceFollowUpId`). Historical completed/escalated tasks are preserved for auditability.

---

## Workflow Sequence

```text
Existing 9-Step Encounter Workflow (Diagnosis → Evidence → RAG → Ranking → Triage → Treatment → CDS → ActionPlan → Report)
        │
        ▼
ClinicalDiagnosisReport
        │
        ▼
ClinicalCaseService (Longitudinal Case Processing)
        │
        ▼
CareCoordinationService (Post-Encounter Operational Layer)
        │
        ├── ClinicalCareTaskEngine (Deterministic Care Task Generation)
        ├── CareTaskEscalationEngine (Deterministic Task & Schedule Escalation)
        └── InMemoryClinicalCareTaskRepository (Auditable Persistence & Querying)
```

---

## Domain Models

- `CareTaskType`: `VETERINARIAN_REVIEW`, `FOLLOW_UP`, `MONITORING`, `DIAGNOSTIC_TEST`, `TREATMENT_REVIEW`, `OWNER_CONTACT`, `REFERRAL`, `EMERGENCY_ESCALATION`, `CASE_REVIEW`.
- `CareTaskPriority`: `EMERGENCY`, `HIGH`, `MEDIUM`, `LOW`.
- `CareTaskActor`: `VETERINARIAN`, `CAREGIVER`, `LABORATORY`, `SYSTEM`, `REFERRAL_PROVIDER`.
- `CareTaskStatus`: `PENDING`, `ASSIGNED`, `IN_PROGRESS`, `DUE`, `OVERDUE`, `COMPLETED`, `CANCELLED`, `ESCALATED`.
- `ClinicalCareTask`: Immutable record representing an operational task.
- `CareTaskAssignment`: Immutable assignment tracking record.
- `CareTaskSummary`: Operational summary projection for a case.
- `FollowUpSchedule` & `FollowUpScheduleStatus`: Follow-up scheduling model derived from existing `ClinicalFollowUp` data.

---

## Observability & Telemetry

- **Domain Events**: `ClinicalCareTaskCreatedEvent`, `ClinicalCareTaskAssignedEvent`, `ClinicalCareTaskCompletedEvent`, `ClinicalCareTaskOverdueEvent`, `ClinicalCareTaskEscalatedEvent`, `ClinicalFollowUpDueEvent`, `ClinicalFollowUpMissedEvent`.
- **Metrics**: `clinical_care_tasks_total`, `clinical_care_task_overdue_total`, `clinical_care_task_escalation_total`, `clinical_care_task_completion_total`, `clinical_followups_due_total`, `clinical_followups_missed_total`. (Tagged strictly with low-cardinality values `task_type`, `priority`, `actor`, `status`, `urgency`).
- **Span Events**: `care.task.created`, `care.task.assigned`, `care.task.started`, `care.task.completed`, `care.task.overdue`, `care.task.escalated`, `followup.due`, `followup.missed`.

---

## Stage 13.1.8 Integration: Operational Work Queues

Stage 13.1.8 consumes `ClinicalCareTaskRepository` and `CareCoordinationService` via `CareTaskWorkQueueService` and `VeterinarianWorkQueueService` to project care tasks into deterministic, paginated operational work queues (`PageResult<T>`) ordered by `EMERGENCY > HIGH > MEDIUM > LOW` without altering canonical care task state.
