# Enterprise Clinical Action & Care Plan Orchestration Architecture

## Executive Summary

Stage 13.1.5 introduces **Enterprise Clinical Action & Care Plan Orchestration** to the Vetra Clinical Intelligence Workflow. This layer converts existing structured workflow state (`TriageAssessment`, `TreatmentPlan`, `ClinicalDecisionSupport`, `UnifiedClinicalEvidence`, `DiseaseCandidate`, `RetrievedContext`) into a deterministic, prioritized, auditable `ClinicalActionPlan`.

The underlying **AI Platform v1.1 execution pipeline** (`DefaultAIGateway` → `AIGovernancePipeline` → `AICacheManager` → `FailoverManager` → `ProviderRouter` → `AIProvider`) and **Multi-Agent Framework** (`AgentGateway` → `AgentRegistry` → `AIAgent`) remain **100% frozen, immutable, and preserved**.

---

## 1. 9-Step Workflow Sequence

```text
DiagnosisStep (Order 1) ── Visual pathology inspection
      │
      ▼
EvidenceAggregationStep (Order 2) ── Normalizes 7 evidence streams & detects conflicts
      │
      ▼
KnowledgeStep (Order 3) ── Species-aware RAG literature retrieval
      │
      ▼
RankingStep (Order 4) ── Dynamic weighted candidate ranking
      │
      ▼
ClinicalTriageStep (Order 5) ── 2-Layer safety evaluation (Rules + AI)
      │
      ▼
TreatmentStep (Order 6) ── Evidence-based treatment planning
      │
      ▼
DecisionSupportStep (Order 7) ── Deterministic explainability, traceability, & review flags
      │
      ▼
ActionPlanStep (Order 8) ── Deterministic action plan synthesis & prioritization
      │
      ▼
ReportStep (Order 9) ── Assembles ClinicalDiagnosisReport with ClinicalActionPlan
```

---

## 2. Core Architectural Principles & Safety Invariants

1. **Strict Determinism**: `ClinicalActionPlanEngine` is a pure Spring component that synthesizes `ClinicalActionPlan` directly from structured workflow data. Zero AI calls or LLM entries are executed.
2. **Fact & Provenance Integrity**: Copy medication names, dosages, frequencies, precautions, and contraindications EXACTLY as present in `TreatmentPlan`. If data is missing, the system explicitly represents the limitation instead of inventing details.
3. **Emergency Precedence**: An `EMERGENCY` triage classification always produces a mandatory `VETERINARY_REFERRAL` action with `ActionPriority.EMERGENCY` and `veterinarianRequired = true`. Lower-priority actions cannot override or downgrade an emergency.
4. **Authoritative Veterinarian Review**: If `ClinicalDecisionSupport.veterinarianReviewFlag.requiresReview == true`, `ClinicalActionPlan.veterinarianReviewRequired` MUST be `true`, preserving all review reason categories.
5. **Conservative Action Generation**: `ISOLATION`, `DIAGNOSTIC_TEST`, `PREVENTIVE_CARE`, `OWNER_NOTIFICATION` actions are generated ONLY when explicitly supported by existing context state.
6. **Farmer vs. Veterinarian Projections**: `FarmerActionPlanView` and `VeterinarianActionPlanView` are presentation projections over `ClinicalActionPlan` containing ZERO independent clinical decision logic.

---

## 3. Observability & Safe Audit Metadata

### Micrometer Metrics
- **`clinical_action_plan_total`**: Counter tagged with `urgency` and `review_required`.

### OpenTelemetry Span Events
- `action.plan.started`
- `action.plan.generated`
- `action.escalation.required`
- `action.veterinarian.review.required`
- `action.plan.completed`

### Safe Audit Metadata
Strictly excludes PII, UUIDs, animal metrics, raw text, and prompt narratives. Contains only safe structural metadata: `engineVersion`, `workflowStepOrder`, `actionPlanStrategy`, `evaluatedAt`, `totalStepCount`.
